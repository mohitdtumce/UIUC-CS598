package org.example.helper;

import com.google.api.MetricDescriptor;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesClient.ListPagedResponse;
import com.google.cloud.compute.v1.ListInstancesRequest;
import com.google.cloud.compute.v1.Region;
import com.google.cloud.compute.v1.RegionsClient;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Aggregation;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Timestamps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GCEHelper {

  // Do not change the order.
  private static final List<String> metrics = List.of(
      // CPU
      "compute.googleapis.com/instance/cpu/utilization",

      // RAM
      "compute.googleapis.com/instance/memory/balloon/ram_used",
      "compute.googleapis.com/instance/memory/balloon/ram_size",

      // Network
      "compute.googleapis.com/instance/network/received_bytes_count",
      "compute.googleapis.com/instance/network/sent_bytes_count",

      // Disk
      "compute.googleapis.com/instance/disk/average_io_latency",
      "compute.googleapis.com/instance/disk/read_bytes_count",
      "compute.googleapis.com/instance/disk/write_bytes_count"
  );

  private final MetricServiceClient metricServiceClient = MetricServiceClient.create();
  private final InstancesClient instancesClient = InstancesClient.create();

  private final RegionsClient regionsClient = RegionsClient.create();

  private final GCPConfig config;

  public GCEHelper(GCPConfig config) throws IOException {
    this.config = config;
  }

  public List<MetricPair> initializeMetricPairs(List<String> metricNames) throws IOException {
    List<MetricPair> metricPairs = new ArrayList<>();
    for (String metricName : metricNames) {
      // Ensure metricName has the full path with projectId
      String fullMetricName = String.format("projects/%s/metricDescriptors/%s",
          this.config.projectId,
          metricName);
      MetricDescriptor descriptor = metricServiceClient.getMetricDescriptor(fullMetricName);
      Aggregation aggregation = getAggregationForMetric(descriptor);
      metricPairs.add(new MetricPair(metricName, aggregation));
    }
    return metricPairs;
  }

  private List<String> listZonesInRegion() {
    List<String> zones = new ArrayList<>();
    // Get the region details
    Region regionDetails = regionsClient.get(this.config.projectId, this.config.region);

    // List and print zones available in the region
    // System.out.println("Zones in region " + region + ":");
    for (String zone : regionDetails.getZonesList()) {
      String zoneName = zone.substring(zone.lastIndexOf('/') + 1);
      // System.out.println(zoneName);
      zones.add(zoneName);
    }
    return zones;
  }

  public String removePrefix(String str, String prefix) {
    if (str.startsWith(prefix)) {
      return str.substring(prefix.length());
    }
    return str;
  }

  public String fetchMetrics() {
    List<String> zones = listZonesInRegion();
    StringJoiner output = new StringJoiner("\n");
    output.add("instanceName,availabilityZone,state,cpuUtilization,ramUsed,ramSize,"
        + "networkIn,networkOut,averageIOLatency,diskReadBytes,diskWriteBytes");
    for (String zone : zones) {
      try {
        List<MetricPair> metricPairs = initializeMetricPairs(metrics);

        ListInstancesRequest request = ListInstancesRequest.newBuilder()
            .setProject(this.config.projectId).setZone(zone)
            .setFilter("scheduling.preemptible = true").build();

        ListPagedResponse response = instancesClient.list(request);

        for (Instance instance : response.iterateAll()) {
          StringJoiner instanceData = new StringJoiner(",");
          instanceData.add(instance.getName());
          instanceData.add(zone);
          instanceData.add(instance.getStatus());
          for (MetricPair metricPair : metricPairs) {
            instanceData.add(
                fetchMetrics(metricPair, zone, instance.getId()));
          }
          output.add(instanceData.toString());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return output.toString();
  }

  private Aggregation getAggregationForMetric(MetricDescriptor descriptor) {
    switch (descriptor.getMetricKind()) {
      case GAUGE:
        return Aggregation.newBuilder()
            .setAlignmentPeriod(Duration.newBuilder().setSeconds(60))
            .setPerSeriesAligner(Aggregation.Aligner.ALIGN_MEAN).build();
      case CUMULATIVE:
        return Aggregation.newBuilder()
            .setAlignmentPeriod(Duration.newBuilder().setSeconds(60))
            .setPerSeriesAligner(Aggregation.Aligner.ALIGN_SUM).build();
      default:
        return Aggregation.newBuilder().build();
    }
  }

  private String fetchMetrics(MetricPair metricPair, String zone, long instanceId) {
    ProjectName projectName = ProjectName.of(this.config.projectId);
    TimeInterval interval = TimeInterval.newBuilder()
        .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
        .setStartTime(Timestamps.fromMillis(System.currentTimeMillis() - 300_000))
        .build();

    ListTimeSeriesRequest request = ListTimeSeriesRequest.newBuilder()
        .setName(projectName.toString())
        .setFilter(String.format(
            "metric.type=\"%s\" AND resource.labels.instance_id=\"%s\" AND resource.labels.zone=\"%s\"",
            metricPair.metricName, instanceId, zone))
        .setInterval(interval)
        .setAggregation(metricPair.aggregation)
        .build();

    double maxVal = 0.0;
    for (TimeSeries timeSeries : metricServiceClient.listTimeSeries(request).iterateAll()) {
      for (var point : timeSeries.getPointsList()) {
        maxVal = Math.max(maxVal, point.getValue().getDoubleValue());
      }
    }
    return String.valueOf(maxVal);
  }

  record MetricPair(String metricName, Aggregation aggregation) {

  }
}