/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import org.opensearch.action.ActionType;

public class IndexStoreAction extends ActionType<IndexStoreResponse> {
    public static final IndexStoreAction INSTANCE = new IndexStoreAction();
    public static final String NAME = "indices:data/read/store";

    private IndexStoreAction() {
        super(NAME, IndexStoreResponse::new);
    }
}
