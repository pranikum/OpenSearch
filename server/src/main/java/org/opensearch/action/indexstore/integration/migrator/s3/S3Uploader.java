/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.s3;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.File;
import java.nio.ByteBuffer;

public class S3Uploader {
    private AwsBasicCredentials awsBasicCredentials;
    private String bucket;
    private String region;

    private S3Client s3client;

    public S3Uploader(String bucket, String region) {
        this.bucket = bucket;
        this.region = region;
        awsBasicCredentials = AwsBasicCredentials.create("AKIAYAFPCO753KVM6WU4", "tZ9vzGLW/1iR6hl9In0AarUATogsICJxRczqdIEk");
        this.initAWSClient();
    }

    private void initAWSClient() {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
        this.s3client = S3Client.builder().
                credentialsProvider(credentialsProvider).
                region(Region.of(this.region)).
                build();
    }

    public void Upload(String filePath) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(this.bucket).key(filePath).build();
        this.s3client.putObject(request, RequestBody.fromFile(new File(filePath)));
    }

    public void Upload(String filePath, ByteBuffer byteBuffer) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(this.bucket).key(filePath).build();
        this.s3client.putObject(request, RequestBody.fromByteBuffer(byteBuffer));
    }

//    public static void main(String[] args) {
//        DummyS3Uploader dummyS3Uploader = new DummyS3Uploader("hackathon-s3upload", "us-east-1");
//        dummyS3Uploader.Upload("/Users/ramachil/test.csv");
//    }
}
