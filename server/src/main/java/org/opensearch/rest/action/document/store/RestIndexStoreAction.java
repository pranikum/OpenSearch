/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.document.store;


import org.opensearch.action.indexstore.IndexStoreAction;
import org.opensearch.action.indexstore.IndexStoreRequest;
import org.opensearch.client.Requests;
import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class RestIndexStoreAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return singletonList(new Route(PUT, "/indices/{index_name}/store/{type}"));
    }

    @Override
    public String getName() {
        return "index_store_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        IndexStoreRequest indexStoreRequest = createRequest(request);
        return channel -> client.execute(IndexStoreAction.INSTANCE, indexStoreRequest, new RestToXContentListener<>(channel));
    }

    IndexStoreRequest createRequest(RestRequest request) throws IOException {
        System.out.println("Request params " + request.params());
        String indexName = request.param("index_name");
        String storeName = request.param("type");
        String storeFormat = request.param("format");

        System.out.println("Index is " + indexName + " Type is " + storeName + " format is " + storeFormat);

        IndexStoreRequest indexStoreRequest = Requests.indexStoreRequest(indexName, storeName, storeFormat);
        return indexStoreRequest;
    }
}
