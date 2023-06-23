/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.OriginalIndices;
import org.opensearch.action.search.QueryPhaseResultConsumer;
import org.opensearch.action.search.SearchContextId;
import org.opensearch.action.search.SearchPhaseController;
import org.opensearch.action.search.SearchQueryThenFetchAsyncAction;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchShardIterator;
import org.opensearch.action.search.SearchTask;
import org.opensearch.action.search.SearchTransportService;
import org.opensearch.action.search.TransportSearchAction;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.node.NodeClient;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.routing.GroupShardsIterator;
import org.opensearch.cluster.routing.ShardIterator;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.breaker.CircuitBreaker;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.index.Index;
import org.opensearch.index.query.Rewriteable;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.internal.AliasFilter;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.RemoteClusterAware;
import org.opensearch.transport.RemoteClusterService;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.opensearch.action.search.SearchType.QUERY_THEN_FETCH;

public class TransportIndexStoreAction extends HandledTransportAction<IndexStoreRequest, IndexStoreResponse> {
    private static final Logger logger = LogManager.getLogger(TransportIndexStoreAction.class);
    private final ClusterService clusterService;
    private final SearchTransportService searchTransportService;
    private final SearchPhaseController searchPhaseController;
    private final ThreadPool threadPool;
    private final CircuitBreaker circuitBreaker;
    private final IndexStoreSearchService searchService;
    private final RemoteClusterService remoteClusterService;
    private final IndexNameExpressionResolver indexNameExpressionResolver;

    @Inject
    public TransportIndexStoreAction(
            NodeClient client,
            ThreadPool threadPool,
            CircuitBreakerService circuitBreakerService,
            TransportService transportService,
            IndexStoreSearchService searchService,
            SearchTransportService searchTransportService,
            SearchPhaseController searchPhaseController,
            ClusterService clusterService,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            NamedWriteableRegistry namedWriteableRegistry) {
        super(IndexStoreAction.NAME, transportService, actionFilters, (Writeable.Reader<IndexStoreRequest>) IndexStoreRequest::new);
        this.searchTransportService = searchTransportService;
        this.clusterService = clusterService;
        this.searchPhaseController = searchPhaseController;
        this.remoteClusterService = searchTransportService.getRemoteClusterService();
        this.threadPool = threadPool;
        this.circuitBreaker = circuitBreakerService.getBreaker(CircuitBreaker.REQUEST);
        this.indexNameExpressionResolver = indexNameExpressionResolver;
        this.searchService = searchService;
    }

    @Override
    protected void doExecute(Task task, IndexStoreRequest request, ActionListener<IndexStoreResponse> listener) {
        logger.info("Received Index store Request [{}]", request);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.allowPartialSearchResults(false);
        searchRequest.indices(request.getIndex());
        IndexStoreMatchAllQueryBuilder matchAllQueryBuilder = new IndexStoreMatchAllQueryBuilder();
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource().query(matchAllQueryBuilder);
        searchRequest.source(sourceBuilder);

        SearchTask searchTask = searchRequest.createTask(task.getId(), task.getType(), task.getAction(), task.getParentTaskId(), Collections.emptyMap());

        final TransportSearchAction.SearchTimeProvider timeProvider = new TransportSearchAction.SearchTimeProvider(
                System.currentTimeMillis(),
                System.nanoTime(),
                System::nanoTime
        );

        ActionListener<SearchSourceBuilder> rewriteListener = ActionListener.wrap(source -> {
            if (source != searchRequest.source()) {
                // only set it if it changed - we don't allow null values to be set but it might be already null. this way we catch
                // situations when source is rewritten to null due to a bug
                searchRequest.source(source);
            }
            final ClusterState clusterState = clusterService.state();
            final SearchContextId searchContext;
            final Map<String, OriginalIndices> remoteClusterIndices;

                searchContext = null;
                remoteClusterIndices = remoteClusterService.groupIndices(
                        searchRequest.indicesOptions(),
                        searchRequest.indices(),
                        idx -> indexNameExpressionResolver.hasIndexAbstraction(idx, clusterState)
                );

            OriginalIndices localIndices = remoteClusterIndices.remove(RemoteClusterAware.LOCAL_CLUSTER_GROUP_KEY);
            if (remoteClusterIndices.isEmpty()) {
                final List<SearchShardIterator> localShardIterators;
                final Map<String, Set<String>> indexRoutings;
                final String[] concreteLocalIndices;

                final Index[] indices = indexNameExpressionResolver.concreteIndices(clusterState, localIndices, System.currentTimeMillis());
                Map<String, Set<String>> routingMap = indexNameExpressionResolver.resolveSearchRouting(
                        clusterState,
                        searchRequest.routing(),
                        searchRequest.indices()
                );
                routingMap = routingMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(routingMap);
                concreteLocalIndices = new String[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    concreteLocalIndices[i] = indices[i].getName();
                }
                Map<String, Long> nodeSearchCounts = searchTransportService.getPendingSearchRequests();
                GroupShardsIterator<ShardIterator> localShardRoutings = clusterService.operationRouting()
                        .searchShards(
                                clusterState,
                                concreteLocalIndices,
                                routingMap,
                                searchRequest.preference(),
                                searchService.getResponseCollectorService(),
                                nodeSearchCounts
                        );
                localShardIterators = StreamSupport.stream(localShardRoutings.spliterator(), false)
                        .map(it -> new SearchShardIterator(searchRequest.getLocalClusterAlias(), it.shardId(), it.getShardRoutings(), localIndices))
                        .collect(Collectors.toList());
                indexRoutings = routingMap;

                final GroupShardsIterator<SearchShardIterator> shardIterators = GroupShardsIterator.sortAndCreate(localShardIterators);

                // optimize search type for cases where there is only one shard group to search on
                if (shardIterators.size() == 1) {
                    // if we only have one group, then we always want Q_T_F, no need for DFS, and no need to do THEN since we hit one shard
                    searchRequest.searchType(QUERY_THEN_FETCH);
                }
                if (searchRequest.allowPartialSearchResults() == null) {
                    // No user preference defined in search request - apply cluster service default
                    searchRequest.allowPartialSearchResults(searchService.defaultAllowPartialSearchResults());
                }

                final DiscoveryNodes nodes = clusterState.nodes();
                BiFunction<String, String, Transport.Connection> connectionLookup = TransportSearchAction.buildConnectionLookup(
                        searchRequest.getLocalClusterAlias(),
                        nodes::get,
                        (clusterName, nodeId) -> null,
                        searchTransportService::getConnection
                );
                final Executor asyncSearchExecutor = asyncSearchExecutor(concreteLocalIndices, clusterState);
//                final boolean preFilterSearchShards = TransportSearchAction.shouldPreFilterSearchShards(
//                        clusterState,
//                        searchRequest,
//                        concreteLocalIndices,
//                        localShardIterators.size()
//                );

                final QueryPhaseResultConsumer queryResultConsumer = searchPhaseController.newSearchPhaseResults(
                        threadPool.executor(ThreadPool.Names.SEARCH),
                        circuitBreaker,
                        searchTask.getProgressListener(),
                        new SearchRequest(request.getIndex()),
                        localShardIterators.size(),
                        exc -> cancelTask(null, exc)
                );
                Map<String, AliasFilter> aliasFilter = buildPerIndexAliasFilter(searchRequest, clusterState, indices, Collections.emptyMap());
                SearchQueryThenFetchAsyncAction searchAsyncAction = new SearchQueryThenFetchAsyncAction(
                        logger,
                        searchTransportService,
                        connectionLookup,
                        aliasFilter,
                        resolveIndexBoosts(searchRequest, clusterState),
                        indexRoutings,
                        searchPhaseController,
                        asyncSearchExecutor,
                        queryResultConsumer,
                        searchRequest,
                        new ActionListener<SearchResponse>() {
                            @Override
                            public void onResponse(SearchResponse searchResponse) {
                                logger.info("Search response hits value " + searchResponse.getHits().getTotalHits().value);
                                CsvSchema schema = null;

                                for (SearchHit hit : searchResponse.getHits().getHits()) {
                                    logger.info("Doc fields " + hit.getFields());
                                }

                            }

                            @Override
                            public void onFailure(Exception e) {
                                logger.info("Search response Failure", e);
                            }
                        },
                        shardIterators,
                        timeProvider,
                        clusterState,
                        searchTask,
                        new SearchResponse.Clusters(1, 1, 0)
                );
                searchAsyncAction.setIndexStoreRequest(request);
                searchAsyncAction.start();
            }
        }, listener::onFailure);

        Rewriteable.rewriteAndFetch(
                searchRequest.source(),
                searchService.getRewriteContext(timeProvider::getAbsoluteStartMillis),
                rewriteListener
        );
        listener.onResponse(new IndexStoreResponse(true));
    }

    private void cancelTask(SearchTask task, Exception exc) {
        logger.info("In cancel task");
    }

    private Map<String, AliasFilter> buildPerIndexAliasFilter(
            SearchRequest request,
            ClusterState clusterState,
            Index[] concreteIndices,
            Map<String, AliasFilter> remoteAliasMap
    ) {
        final Map<String, AliasFilter> aliasFilterMap = new HashMap<>();
        final Set<String> indicesAndAliases = indexNameExpressionResolver.resolveExpressions(clusterState, request.indices());
        for (Index index : concreteIndices) {
            clusterState.blocks().indexBlockedRaiseException(ClusterBlockLevel.READ, index.getName());
            AliasFilter aliasFilter = searchService.buildAliasFilter(clusterState, index.getName(), indicesAndAliases);
            assert aliasFilter != null;
            aliasFilterMap.put(index.getUUID(), aliasFilter);
        }
        aliasFilterMap.putAll(remoteAliasMap);
        return aliasFilterMap;
    }

    Executor asyncSearchExecutor(final String[] indices, final ClusterState clusterState) {
        final boolean onlySystemIndices = Arrays.stream(indices).allMatch(index -> {
            final IndexMetadata indexMetadata = clusterState.metadata().index(index);
            return indexMetadata != null && indexMetadata.isSystem();
        });
        return onlySystemIndices ? threadPool.executor(ThreadPool.Names.SYSTEM_READ) : threadPool.executor(ThreadPool.Names.SEARCH);
    }

    private Map<String, Float> resolveIndexBoosts(SearchRequest searchRequest, ClusterState clusterState) {
        if (searchRequest.source() == null) {
            return Collections.emptyMap();
        }

        SearchSourceBuilder source = searchRequest.source();
        if (source.indexBoosts() == null) {
            return Collections.emptyMap();
        }

        Map<String, Float> concreteIndexBoosts = new HashMap<>();
        for (SearchSourceBuilder.IndexBoost ib : source.indexBoosts()) {
            Index[] concreteIndices = indexNameExpressionResolver.concreteIndices(
                    clusterState,
                    searchRequest.indicesOptions(),
                    ib.getIndex()
            );

            for (Index concreteIndex : concreteIndices) {
                concreteIndexBoosts.putIfAbsent(concreteIndex.getUUID(), ib.getBoost());
            }
        }
        return Collections.unmodifiableMap(concreteIndexBoosts);
    }
}
