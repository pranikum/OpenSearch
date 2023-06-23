/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.util.BigArrays;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.node.ResponseCollectorService;
import org.opensearch.script.ScriptService;
import org.opensearch.search.SearchService;
import org.opensearch.search.fetch.FetchPhase;
import org.opensearch.search.fetch.QueryFetchSearchResult;
import org.opensearch.search.internal.ReaderContext;
import org.opensearch.search.internal.SearchContext;
import org.opensearch.search.query.QueryPhase;
import org.opensearch.threadpool.ThreadPool;

import java.util.concurrent.Executor;

public class IndexStoreSearchService extends SearchService {

    public IndexStoreSearchService(
            ClusterService clusterService,
            IndicesService indicesService,
            ThreadPool threadPool,
            ScriptService scriptService,
            BigArrays bigArrays,
            QueryPhase queryPhase,
            FetchPhase fetchPhase,
            ResponseCollectorService responseCollectorService,
            CircuitBreakerService circuitBreakerService,
            Executor indexSearcherExecutor) {
        super(clusterService,
                indicesService,
                threadPool,
                scriptService,
                bigArrays,
                queryPhase,
                fetchPhase,
                responseCollectorService,
                circuitBreakerService,
                indexSearcherExecutor);
    }

    protected QueryFetchSearchResult executeFetchPhase(ReaderContext reader, SearchContext context, long afterQueryTime) {
        context.setMigration(true);
        return super.executeFetchPhase(reader, context, afterQueryTime);

    }
}
