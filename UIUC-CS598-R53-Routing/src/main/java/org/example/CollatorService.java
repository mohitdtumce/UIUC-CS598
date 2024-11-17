package org.example;

import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.GSLBService.Route53Weights;
import org.example.helper.AWSConfig;
import org.example.helper.AWSCredentialHelper;
import org.example.helper.GCPConfig;
import org.example.helper.GCSHelper;
import org.example.helper.InstanceData;
import org.example.helper.S3Helper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.model.GeoLocation;

public class CollatorService implements Runnable {

  private final GCPConfig gcpConfig;

  private final GCSHelper gcsHelper;

  private final AWSConfig awsConfig;

  private final S3Helper s3Helper;

  private final GSLBService gslbService;

  private final AtomicInteger executionCount = new AtomicInteger(0);

  public CollatorService(GCPConfig gcpConfig, AWSConfig awsConfig) throws IOException {
    this.gcpConfig = gcpConfig;
    this.gcsHelper = new GCSHelper(gcpConfig);

    this.awsConfig = awsConfig;
    AWSCredentialHelper credentialHelper = new AWSCredentialHelper(
        "/usr/local/google/home/mohitshr/.ssh/aws_access_keys.csv");
    this.s3Helper = new S3Helper(this.awsConfig, credentialHelper);

    gslbService = new GSLBService(awsConfig, credentialHelper);
  }

  @Override
  public void run() {
    int count = executionCount.incrementAndGet();
    System.out.printf("Running CollatorService: %d\n", count);

    System.out.println("Fetching GCS Data...");
    List<InstanceData> gcsData = null;
    try {
      gcsData = gcsHelper.readGCSInstances();
    } catch (IOException | CsvValidationException e) {
      throw new RuntimeException(e);
    }
    for (InstanceData dataPoint : gcsData) {
      System.out.println(dataPoint.toString());
    }

    System.out.println("Fetching GCS Data...");
    List<InstanceData> awsData = null;
    try {
      awsData = s3Helper.readS3Instances();
    } catch (IOException | CsvValidationException e) {
      throw new RuntimeException(e);
    }
    for (InstanceData dataPoint : awsData) {
      System.out.println(dataPoint.toString());
    }

    System.out.println("Updating weights...");
    Route53Weights weights = computeWeights(gcsData, awsData);
    try {
      gslbService.updateWeight(GeoLocation.builder().countryCode("US").build(), Region.US_EAST_1,
          weights);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  Route53Weights computeWeights(List<InstanceData> gcsData, List<InstanceData> awsData) {
    // Step 1: Calculate individual scores
    long awsScore = calculateScore(awsData);
    long gcpScore = calculateScore(gcsData);
    long azureScore = Math.max(0, 100 - gcpScore - awsScore);
    return new Route53Weights(awsScore, azureScore, gcpScore);
  }

  private Long calculateScore(List<InstanceData> instances) {
    int numberOfInstances = instances.size();
    double totalCpu = 0;
    double totalNetworkIn = 0;
    double totalDiskReadBytes = 0;

    for (InstanceData instance : instances) {
      if (instance.state.equalsIgnoreCase("running")) {
        totalCpu += instance.cpuUtilization;
        totalNetworkIn += instance.networkIn;
        totalDiskReadBytes += instance.diskReadBytes;
      }
    }

    // Step 2: Normalize metrics (adjust weight factors as needed)
    double cpuScore = totalCpu; // Higher is better
    double instanceScore = numberOfInstances; // Higher is better
    double ingressLoadScore = totalNetworkIn > 0 ? 1 / totalNetworkIn : 1; // Lower is better
    double diskLoadScore = totalDiskReadBytes > 0 ? 1 / totalDiskReadBytes : 1; // Lower is better

    // Step 3: Combine metrics (weights can be adjusted as per requirements)
    return (long) ((0.4 * cpuScore) + (0.4 * instanceScore) + (0.1 * ingressLoadScore) + (0.1
        * diskLoadScore));
  }
}

