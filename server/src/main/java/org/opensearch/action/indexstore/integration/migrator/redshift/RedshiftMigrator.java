/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.redshift;

import org.opensearch.action.indexstore.integration.migrator.IMigrator;
import org.opensearch.search.SearchHit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedshiftMigrator implements IMigrator {
    private final String indexName;

    public RedshiftMigrator(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public void migrate(SearchHit[] hits) {
        List<String> fields = null;
        List<Map<String, Object>> dataMap = new ArrayList<>();
        if(hits.length > 0) {
            Map<String, Object> sourceMap = hits[0].getSourceAsMap();
            Set<String> keys = sourceMap.keySet();
            fields = new ArrayList<>(keys);

            for(int i=0; i<hits.length; i++) {
                dataMap.add(sourceMap);
            }
        }
        try {
            if(dataMap.size() > 0) {
                migrateDataToRedshift(fields, dataMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void migrateDataToRedshift(List<String> fields, List<Map<String, Object>> sourceMap) throws SQLException {
        Connection connection = null;
        try {
            RedshiftConnector connector = new RedshiftConnector();
            connection = connector.getConnection();
            if(connection == null) {
                System.out.println("Connection could not be established ");
                return;
            }
            connector.createTable(connection, indexName, fields);
            connector.insertData(connection, indexName, fields, sourceMap);

        } finally {
            if(connection != null) {
                connection.close();
            }
        }
    }
}
