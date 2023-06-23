/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensearch.action.indexstore.integration.migrator.IMigrator;
import org.opensearch.search.SearchHit;


public class S3Migrator implements IMigrator {
    private String format;
    public S3Migrator(String format) {
        this.format = format;
    }
    
    @Override
    public void migrate(SearchHit[] hits) {
        List<String> keyList;
        if(hits.length > 0) {
            Map<String, Object> sourceMap = hits[0].getSourceAsMap();
            Set<String> keys = sourceMap.keySet();

            keyList = new ArrayList<>(keys);
            CSVData csvData = new CSVData();
            String header = buildHeader(keyList);
            csvData.header = header;

            StringBuilder csvContent = new StringBuilder();
            for(int i=0; i<hits.length; i++) {
                sourceMap = hits[i].getSourceAsMap();
                for(int j=0; j < keyList.size(); j++) {
                    csvContent.append(sourceMap.get(keyList.get(j)).toString());
                    csvContent.append(",");
                }
                csvContent.deleteCharAt(csvContent.length()-1);
                csvContent.append(System.lineSeparator());
            }
            csvData.payload = csvContent.toString();

            System.out.println(csvData);
        }
    }

    class CSVData {
        String header;
        String payload;

        @Override
        public String toString() {
            return header + payload;
        }
    }

    private String buildHeader(List<String> keyList) {
        String header = "";
        System.out.println("keyList = " + keyList);
        StringBuilder builder = new StringBuilder();
        if(keyList != null && keyList.size() > 0) {

            for (String key : keyList) {
                builder.append(key);
                builder.append(",");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(System.lineSeparator());

        }
        return builder.toString();
    }
}
