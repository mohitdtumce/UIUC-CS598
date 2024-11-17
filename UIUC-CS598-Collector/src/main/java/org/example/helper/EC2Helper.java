package org.example.helper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

public class EC2Helper {

  // Metrics to fetch from CloudWatch
  private static final List<String> METRICS = List.of(
      "CPUUtilization",
      "NetworkIn",
      "NetworkOut",
      "DiskReadBytes",
      "DiskWriteBytes"
  );
  private final Region region;
  private final Ec2Client ec2Client;
  private final CloudWatchClient cloudWatch;
  private final AWSCredentialHelper credentialHelper;

  public EC2Helper(Region region, AWSCredentialHelper credentialHelper) throws IOException {
    this.region = region;
    this.credentialHelper = credentialHelper;
    this.ec2Client = Ec2Client.builder()
        .credentialsProvider(this.credentialHelper.getAwsCredentialProvider())
        .region(region).build();
    this.cloudWatch = CloudWatchClient.builder()
        .credentialsProvider(this.credentialHelper.getAwsCredentialProvider())
        .region(region).build();
  }

  public String fetchMetrics() {
    StringBuilder output = new StringBuilder();
    output.append(
        "instanceId,availabilityZone,state,cpuUtilization,networkIn,networkOut,diskReadBytes,diskWriteBytes\n");

    try {
      DescribeInstancesResponse instancesResponse = ec2Client.describeInstances();
      for (Reservation reservation : instancesResponse.reservations()) {
        for (Instance instance : reservation.instances()) {
          String instanceId = instance.instanceId();
          String availabilityZone = instance.placement().availabilityZone();
          String state = instance.state().nameAsString();

          StringBuilder instanceMetrics = new StringBuilder();
          instanceMetrics.append(instanceId).append(",")
              .append(availabilityZone).append(",")
              .append(state);

          // Fetch CloudWatch metrics
          for (String metricName : METRICS) {
            double metricValue = fetchMetric(instanceId, metricName);
            instanceMetrics.append(",").append(metricValue);
          }

          output.append(instanceMetrics).append("\n");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return output.toString();
  }

  private double fetchMetric(String instanceId, String metricName) {
    try {
      Instant endTime = Instant.now();
      Instant startTime = endTime.minusSeconds(300); // Last 5 minutes

      GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
          .namespace("AWS/EC2")
          .metricName(metricName)
          .dimensions(Dimension.builder().name("InstanceId").value(instanceId).build())
          .startTime(startTime)
          .endTime(endTime)
          .period(60)
          .statistics(Statistic.AVERAGE)
          .build();

      GetMetricStatisticsResponse response = cloudWatch.getMetricStatistics(request);
      return response.datapoints().stream()
          .mapToDouble(Datapoint::average)
          .max()
          .orElse(0.0);

    } catch (Exception e) {
      System.err.printf("Error fetching metric %s for instance %s: %s%n", metricName, instanceId,
          e.getMessage());
      return 0.0;
    }
  }
}

