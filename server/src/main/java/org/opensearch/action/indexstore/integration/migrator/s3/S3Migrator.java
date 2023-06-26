/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.s3;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensearch.action.indexstore.S3Uploader;
import org.opensearch.action.indexstore.integration.migrator.IMigrator;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchShardTarget;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;


public class S3Migrator implements IMigrator {
    private String format;
    private String index;
    private SearchShardTarget shardTarget;

    public S3Migrator(String format, String index, SearchShardTarget shardTarget) {
        this.format = format;
        this.index = index;
        this.shardTarget = shardTarget;
    }

    @Override
    public void migrate(SearchHit[] hits) {
        if (hits.length == 0) {
            return;
        }
        List<String> keyList;
        Map<String, Object> sourceMap = hits[0].getSourceAsMap();
        Set<String> keys = sourceMap.keySet();

        keyList = new ArrayList<>(keys);
        CSVData csvData = new CSVData();
        String header = buildHeader(keyList);
        csvData.header = header;

        StringBuilder csvContent = new StringBuilder();
        for (int i = 0; i < hits.length; i++) {
            sourceMap = hits[i].getSourceAsMap();
            for (int j = 0; j < keyList.size(); j++) {
                csvContent.append(sourceMap.get(keyList.get(j)).toString());
                csvContent.append(",");
            }
            csvContent.deleteCharAt(csvContent.length() - 1);
            csvContent.append(System.lineSeparator());
        }
        csvData.payload = csvContent.toString();

        // write to file, if existing file is present override it
        // not working
//            try {
//                PrintWriter writer = new PrintWriter("test.csv", StandardCharsets.UTF_8);
//                writer.write(csvData.toString());
//                writer.close();
//            } catch (IOException e) {
//                System.out.println("error in writing to file " + e.toString());
//            }
        // the below bucket should pre-exist, otherwise it won't work
        String s3filePath =  this.shardTarget.getFullyQualifiedIndexName()
//                + "/" + this.shardTarget.getNodeId()
//                + "/" + System.currentTimeMillis()
                + "/" + "shard_data_" + this.shardTarget.getShardId();

        System.out.println("s3FilePath is " + s3filePath);
        S3Uploader s3Uploader = new S3Uploader("hackathon-s3upload-pranikum", "ap-south-1");
        s3Uploader.Upload(s3filePath, ByteBuffer.wrap(csvData.toString().getBytes()));
        System.out.println("csv Data is " + csvData);
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
        if (keyList != null && keyList.size() > 0) {

            for (String key : keyList) {
                builder.append(key);
                builder.append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(System.lineSeparator());

        }
        return builder.toString();
    }
}
