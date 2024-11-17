package org.example;

import com.google.cloud.compute.v1.Condition.Sys;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.helper.AWSConfig;
import org.example.helper.AWSCredentialHelper;
import org.example.helper.EC2Helper;
import org.example.helper.GCEHelper;
import org.example.helper.GCPConfig;
import org.example.helper.GCSHelper;
import org.example.helper.S3Helper;

public class MetricsUploaderTask implements Runnable {

  private final GCPConfig gcpConfig;
  private final GCEHelper gceHelper;
  private final GCSHelper gcsHelper;
  private final AWSConfig awsConfig;
  private final EC2Helper ec2Helper;
  private final S3Helper s3Helper;

  private AtomicInteger executionCount = new AtomicInteger(0);

  public MetricsUploaderTask(GCPConfig gcpConfig, AWSConfig awsConfig)
      throws IOException {
    this.gcpConfig = gcpConfig;
    this.gceHelper = new GCEHelper(gcpConfig);
    this.gcsHelper = new GCSHelper(gcpConfig);

    this.awsConfig = awsConfig;
    AWSCredentialHelper credentialHelper = new AWSCredentialHelper(
        "/usr/local/google/home/mohitshr/.ssh/aws_access_keys.csv");
    this.ec2Helper = new EC2Helper(this.awsConfig.region, credentialHelper);
    this.s3Helper = new S3Helper(this.awsConfig.region, credentialHelper);
  }

  @Override
  public void run() {
    int count = executionCount.incrementAndGet();
    System.out.printf("Running MetricsUploader: %d\n", count);
    try {
      // Fetch metrics using GCEHelper
      String gcpData = this.gceHelper.fetchMetrics();
      System.out.println(gcpData);
      gcsHelper.uploadToGCS(gcpData);

      System.out.println("GCP Metrics uploaded successfully at: " + System.currentTimeMillis());
    } catch (Exception e) {
      System.err.println("Error during execution: " + e.getMessage());
    }

    try {
      // Fetch metrics using EC2Helper
      String awsData = ec2Helper.fetchMetrics();
      System.out.println(awsData);
      this.s3Helper.uploadToS3(this.awsConfig.bucketName, this.awsConfig.filePath, awsData);

      System.out.println("EC2 Metrics uploaded successfully at: " + System.currentTimeMillis());
    } catch (Exception e) {
      System.err.println("Error during execution: " + e.getMessage());
    }
    System.out.print("ExecutionComplete\n\n");
  }
}


