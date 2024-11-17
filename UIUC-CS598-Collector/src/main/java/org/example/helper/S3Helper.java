package org.example.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3Helper {

  private final S3Client s3Client;
  private final Region region;
  private final AWSCredentialHelper credentialHelper;

  public S3Helper(Region region, AWSCredentialHelper credentialHelper) throws IOException {
    this.region = region;
    this.credentialHelper = credentialHelper;
    this.s3Client = S3Client.builder()
        .credentialsProvider(this.credentialHelper.getAwsCredentialProvider())
        .region(region).build();
  }

  /**
   * Checks if a bucket exists.
   */
  private static boolean bucketExists(S3Client s3, String bucketName) {
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      return true;
    } catch (NoSuchBucketException e) {
      return false;
    } catch (S3Exception e) {
      System.err.println(
          "Error occurred while checking bucket: " + e.awsErrorDetails().errorMessage());
      return false;
    }
  }

  public void listBuckets() {
    try {
      ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
      System.out.println("Buckets:");
      listBucketsResponse.buckets().forEach(bucket -> System.out.println(bucket.name()));
      System.out.println("Listed all S3 buckets.");
    } catch (S3Exception e) {
      System.err.println(
          "Error occurred while listing buckets: " + e.awsErrorDetails().errorMessage());
    }
  }

  public void uploadToS3(String bucketName, String filePath, String data) {
    try {
      if (!bucketExists(s3Client, bucketName)) {
        System.err.println("Bucket does not exist: " + bucketName);
        return;
      }

      s3Client.putObject(PutObjectRequest.builder()
              .bucket(bucketName)
              .key(filePath)
              .contentType("text/csv")
              .build(),
          RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));

      System.out.println("File uploaded to S3: s3://" + bucketName + "/" + filePath);
    } catch (S3Exception e) {
      System.err.println(
          "Error occurred while uploading to S3: " + e.awsErrorDetails().errorMessage());
    }
  }

  // public static void main(String[] args) {
  //   String region = "us-west-2"; // Specify your AWS region
  //   String bucketName = "your-s3-bucket-name"; // Replace with your bucket name
  //   String filePath = "InstanceHealth.csv"; // S3 key for the file
  //   String data = "instanceId,availabilityZone,state,cpuUtilization,networkIn,networkOut,diskReadBytes,diskWriteBytes\n"
  //       + "i-0123456789abcdef,us-west-2a,running,10.5,2048,1024,1000000,500000\n";
  //
  //   try (S3Client s3 = S3Client.builder().region(software.amazon.awssdk.regions.Region.of(region)).build()) {
  //     // List buckets
  //     listBuckets(s3);
  //
  //     // Upload data to S3
  //     uploadToS3(bucketName, filePath, data);
  //   }
  // }
}

