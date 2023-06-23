/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration;

import org.opensearch.search.internal.SearchContext;

public class DataMigrationHandler {
    private SearchContext context;

    public DataMigrationHandler(SearchContext context) {
        this.context = context;
    }

    public void handleMigration() {

    }
}
