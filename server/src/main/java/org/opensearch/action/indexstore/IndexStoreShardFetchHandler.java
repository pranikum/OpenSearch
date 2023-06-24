/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;

import org.opensearch.action.indexstore.integration.migrator.IMigrator;
import org.opensearch.action.indexstore.integration.migrator.redshift.RedshiftMigrator;
import org.opensearch.action.indexstore.integration.migrator.s3.S3Migrator;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchShardTarget;

public class IndexStoreShardFetchHandler {

    private String index;
    private String storeType;
    private String format;

    private SearchShardTarget shardTarget;

    public IndexStoreShardFetchHandler(String index, String storeType, String format, SearchShardTarget shardTarget) {
        this.index = index;
        this.storeType = storeType;
        this.format = format;
        this.shardTarget = shardTarget;
    }

    public void handleShard(SearchHit[] hits) {
        IMigrator migrator = getStoreMigrator();
        if (migrator != null) {
            migrator.migrate(hits);
        }
    }

    private IMigrator getStoreMigrator() {
        IMigrator migrator = null;
        if("s3".equalsIgnoreCase(this.storeType)) {
            migrator = new S3Migrator(this.format, this.index, this.shardTarget);
        } else if("Redshift".equalsIgnoreCase(this.storeType)) {
            migrator = new RedshiftMigrator(this.index);
        }
        return migrator;
    }
}
