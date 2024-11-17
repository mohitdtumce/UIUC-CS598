package org.example.helper;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import java.io.IOException;

/*
* gcloud container clusters update mohitshr-auto-cluster1 \
    --location=us-east4 \
    --enable-managed-prometheus \
    --monitoring=SYSTEM
*/

public class MonitoringHelper {

  public static void fetchIngressBytesCount(String projectId, String clusterName) {
    try {
      try (MetricServiceClient client = MetricServiceClient.create()) {
        // Define the time interval for the last hour
        long now = System.currentTimeMillis();
        Timestamp endTime = Timestamp.newBuilder().setSeconds(now / 1000).setNanos(0).build();
        Timestamp startTime = Timestamp.newBuilder().setSeconds((now - 300000) / 1000).build();

        // Create the request to list time series
        ListTimeSeriesRequest request = ListTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(projectId).toString())
            .setFilter(String.format(
                "metric.type=\"networking.googleapis.com/pod_flow/ingress_bytes_count\" AND "
                    + "resource.type=\"k8s_pod\" AND "
                    + "resource.label.\"project_id\"=\"%s\" AND "
                    + "resource.label.\"cluster_name\"=\"%s\"",
                projectId, clusterName))
            .setInterval(
                TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build())
            .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
            .setAggregation(com.google.monitoring.v3.Aggregation.newBuilder()
                .setPerSeriesAligner(com.google.monitoring.v3.Aggregation.Aligner.ALIGN_RATE)
                .setCrossSeriesReducer(com.google.monitoring.v3.Aggregation.Reducer.REDUCE_SUM)
                .setAlignmentPeriod(Duration.newBuilder().setSeconds(10).build())
                .build())
            .build();

        // Fetch and print the time series data
        for (TimeSeries timeSeries : client.listTimeSeries(request).iterateAll()) {
          System.out.println("Metric: " + timeSeries.getMetric());
          for (var point : timeSeries.getPointsList()) {
            System.out.printf("Ingress Bytes Count: %f at %s%n", point.getValue().getDoubleValue(),
                point.getInterval().getEndTime());
          }
        }
      }
    } catch (IOException | ApiException e) {
      System.err.println("Error fetching ingress bytes count: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void fetchEgressBytesCount(String projectId, String clusterName) {
    try {
      try (MetricServiceClient client = MetricServiceClient.create()) {
        // Define the time interval for the last hour
        long now = System.currentTimeMillis();
        Timestamp endTime = Timestamp.newBuilder().setSeconds(now / 1000).setNanos(0).build();
        Timestamp startTime = Timestamp.newBuilder().setSeconds((now - 300000) / 1000).build();

        // Create the request to list time series
        ListTimeSeriesRequest request = ListTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(projectId).toString())
            .setFilter(String.format(
                "metric.type=\"networking.googleapis.com/pod_flow/egress_bytes_count\" AND "
                    + "resource.type=\"k8s_pod\" AND "
                    + "resource.label.\"project_id\"=\"%s\" AND "
                    + "resource.label.\"cluster_name\"=\"%s\"",
                projectId, clusterName))
            .setInterval(
                TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build())
            .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
            .setAggregation(com.google.monitoring.v3.Aggregation.newBuilder()
                .setPerSeriesAligner(com.google.monitoring.v3.Aggregation.Aligner.ALIGN_RATE)
                .setCrossSeriesReducer(com.google.monitoring.v3.Aggregation.Reducer.REDUCE_SUM)
                .setAlignmentPeriod(Duration.newBuilder().setSeconds(10).build())
                .build())
            .build();

        // Fetch and print the time series data
        for (TimeSeries timeSeries : client.listTimeSeries(request).iterateAll()) {
          System.out.println("Metric: " + timeSeries.getMetric());
          for (var point : timeSeries.getPointsList()) {
            System.out.printf("Egress Bytes Count: %f at %s%n", point.getValue().getDoubleValue(),
                point.getInterval().getEndTime());
          }
        }
      }
    } catch (IOException | ApiException e) {
      System.err.println("Error fetching egress bytes count: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void fetchGkeMetrics(String projectId, String clusterName) {
    try {

      try (MetricServiceClient client = MetricServiceClient.create()) {
        // Define the time interval for the last hour
        long now = System.currentTimeMillis();
        Timestamp endTime = Timestamp.newBuilder().setSeconds(now / 1000).setNanos(0).build();
        Timestamp startTime = Timestamp.newBuilder().setSeconds((now - 3600000) / 1000).setNanos(0)
            .build();

        // Create the request to list time series for CPU usage
        ListTimeSeriesRequest cpuRequest = ListTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(projectId).toString())
            .setFilter(String.format(
                "metric.type=\"kubernetes.io/container/cpu/usage_time\" AND "
                    + "resource.type=\"k8s_container\" AND "
                    + "resource.label.\"project_id\"=\"%s\" AND "
                    + "resource.label.\"cluster_name\"=\"%s\"",
                projectId, clusterName))
            .setInterval(
                TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build())
            .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
            .setAggregation(com.google.monitoring.v3.Aggregation.newBuilder()
                .setPerSeriesAligner(com.google.monitoring.v3.Aggregation.Aligner.ALIGN_RATE)
                .setAlignmentPeriod(Duration.newBuilder().setSeconds(60)
                    .build()) // Set alignment period to 60 seconds
                .build())
            .build();

        // Fetch and print CPU usage metrics
        System.out.println("Fetching CPU usage metrics...");
        for (TimeSeries timeSeries : client.listTimeSeries(cpuRequest).iterateAll()) {
          System.out.println("Metric: " + timeSeries.getMetric());
          for (var point : timeSeries.getPointsList()) {
            System.out.printf("CPU Usage: %f at %s%n", point.getValue().getDoubleValue(),
                point.getInterval().getEndTime());
          }
        }

        // Create the request to list time series for Memory usage
        ListTimeSeriesRequest memoryRequest = ListTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(projectId).toString())
            .setFilter(String.format(
                "metric.type=\"kubernetes.io/container/memory/usage_bytes\" AND "
                    + "resource.type=\"k8s_container\" AND "
                    + "resource.label.\"project_id\"=\"%s\" AND "
                    + "resource.label.\"cluster_name\"=\"%s\"",
                projectId, clusterName))
            .setInterval(
                TimeInterval.newBuilder().setStartTime(startTime).setEndTime(endTime).build())
            .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
            .setAggregation(com.google.monitoring.v3.Aggregation.newBuilder()
                .setPerSeriesAligner(com.google.monitoring.v3.Aggregation.Aligner.ALIGN_RATE)
                .setAlignmentPeriod(Duration.newBuilder().setSeconds(60)
                    .build()) // Set alignment period to 60 seconds
                .build())
            .build();

        // Fetch and print Memory usage metrics
        System.out.println("Fetching Memory usage metrics...");
        for (TimeSeries timeSeries : client.listTimeSeries(memoryRequest).iterateAll()) {
          System.out.println("Metric: " + timeSeries.getMetric());
          for (var point : timeSeries.getPointsList()) {
            System.out.printf("Memory Usage: %f at %s%n", point.getValue().getDoubleValue(),
                point.getInterval().getEndTime());
          }
        }

      }
    } catch (IOException | ApiException e) {
      System.err.println("Error fetching GKE metrics: " + e.getMessage());
      e.printStackTrace();
    }
  }


}
