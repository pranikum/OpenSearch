/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.File;
import java.nio.ByteBuffer;

public class S3Uploader {
    private final AwsBasicCredentials awsBasicCredentials;
    private final String bucket;
    private final String region;

    private S3Client s3client;

    public S3Uploader(String bucket, String region) {
        this.bucket = bucket;
        this.region = region;
        // populate the below and give the user access to s3
        this.awsBasicCredentials = AwsBasicCredentials.create("", "");
        this.initAWSClient();
        if (!this.bucketExists(this.bucket)) {
            try {
                this.s3client.createBucket(CreateBucketRequest.builder().bucket(this.bucket).build());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error occurred during bucket creation. Due to conflicting resource");
            }

        }
    }

    private void initAWSClient() {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(this.awsBasicCredentials);
        this.s3client = S3Client.builder().
            credentialsProvider(credentialsProvider).
            region(Region.of(this.region)).
            build();
    }

    public boolean bucketExists(String bucketName) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
            .bucket(bucketName)
            .build();

        try {
            this.s3client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
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

