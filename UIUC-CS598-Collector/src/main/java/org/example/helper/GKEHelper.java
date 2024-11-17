package org.example.helper;

import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.container.v1.Cluster;
import com.google.container.v1.ListClustersResponse;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GKEHelper {

  public static String getGkeClusterInRegion(String projectId, String region, String clusterId) {
    String location = String.format("projects/%s/locations/%s/clusters/%s", projectId, region,
        clusterId);

    StringBuilder builder = new StringBuilder();
    try (ClusterManagerClient clusterManagerClient = ClusterManagerClient.create()) {
      Cluster cluster = clusterManagerClient.getCluster(location);
      builder.append("ClusterName:").append(cluster.getName()).append(",")
          .append("ClusterStatus:").append(cluster.getStatus()).append(",")
          .append("ClusterLocation:").append(cluster.getLocation()).append(",")
          .append("ClusterEndpoint:").append(cluster.getEndpoint()).append("\n");
    } catch (Exception e) {
      System.err.println("Error listing GKE clusters: " + e.getMessage());
      e.printStackTrace();
    }
    return builder.toString();
  }

  public static void listGkeClustersInRegion(String projectId, String region) {
    String location = String.format("projects/%s/locations/%s", projectId, region);

    try (ClusterManagerClient clusterManagerClient = ClusterManagerClient.create()) {

      ListClustersResponse response = clusterManagerClient.listClusters(location);

      for (Cluster cluster : response.getClustersList()) {
        System.out.printf("Cluster Name: %s%n", cluster.getName());
        System.out.printf("Cluster Status: %s%n", cluster.getStatus());
        System.out.printf("Cluster Location: %s%n", cluster.getLocation());
        System.out.printf("Cluster Endpoint: %s%n", cluster.getEndpoint());
        System.out.println("------");
      }
    } catch (Exception e) {
      System.err.println("Error listing GKE clusters: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static String fetchPodMetrics() {
    try {
      String namespace = "default";

      // Initialize Kubernetes API client
      ApiClient client = Config.defaultClient();
      Configuration.setDefaultApiClient(client);
      // Use the CoreV1 API to list pods
      CoreV1Api api = new CoreV1Api();

      StringJoiner podMetrics = new StringJoiner("\n");

      V1PodList podList = api.listNamespacedPod(namespace).execute();
      for (V1Pod pod : podList.getItems()) {
        StringJoiner containerMetrics = new StringJoiner(",");

        String podName = pod.getMetadata().getName();
        containerMetrics.add(podName);

        pod.getSpec().getContainers().forEach(container -> {
          containerMetrics.add(container.getName());
          if (container.getResources() != null && container.getResources().getRequests() != null) {
            var requests = container.getResources().getRequests();
            containerMetrics.add(
                requests.get("cpu") != null ? requests.get("cpu").getNumber().toString() : "");
            containerMetrics.add(
                requests.get("memory") != null ? requests.get("memory").getNumber().toString()
                    : "");
            containerMetrics.add(
                requests.get("ephemeral-storage") != null ? requests.get("ephemeral-storage")
                    .getNumber().toString() : "");
          }
        });

        podMetrics.add(containerMetrics.toString());
      }
      return podMetrics.toString();
    } catch (ApiException e) {
      Logger.getLogger(GKEHelper.class.getName())
          .log(Level.SEVERE, "Error fetching pod metrics", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return "";
  }

  public static String fetchNodeMetrics() throws IOException {
    // Initialize Kubernetes API client
    ApiClient client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);

    // Use the CoreV1 API to list pods
    CoreV1Api api = new CoreV1Api();

    StringJoiner nodesMetrics = new StringJoiner("\n");
    try {
      StringJoiner nodeMetrics = new StringJoiner(",");
      V1NodeList nodeList = api.listNode().execute();
      for (V1Node node : nodeList.getItems()) {
        String nodeName = node.getMetadata().getName();
        nodeMetrics.add(nodeName);

        var capacity = node.getStatus().getCapacity();
        nodeMetrics.add(
            capacity.get("cpu") != null ? capacity.get("cpu").getNumber().toString() : "");
        nodeMetrics.add(
            capacity.get("memory") != null ? capacity.get("memory").getNumber().toString() : "");
        nodeMetrics.add(
            capacity.get("ephemeral-storage") != null ? capacity.get("ephemeral-storage")
                .getNumber().toString() : "");

        var allocatable = node.getStatus().getAllocatable();
        nodeMetrics.add(
            allocatable.get("ephemeral-storage") != null ? allocatable.get("ephemeral-storage")
                .getNumber().toString() : "");

      }
      nodesMetrics.add(nodeMetrics.toString());
      return nodesMetrics.toString();
    } catch (ApiException e) {
      System.err.println("Error fetching node metrics: " + e.getMessage());
      e.printStackTrace();
    }
    return "";
  }

  public static String fetchPodStorageMetrics(String namespace) throws IOException {
    // Initialize Kubernetes API client
    ApiClient client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);
    // Use the CoreV1 API to list pods
    CoreV1Api api = new CoreV1Api();
    try {
      V1PersistentVolumeClaimList pvcList = api.listNamespacedPersistentVolumeClaim(namespace)
          .execute();
      StringBuilder builder = new StringBuilder();

      for (V1PersistentVolumeClaim pvc : pvcList.getItems()) {
        String pvcName = pvc.getMetadata().getName();
        String storageRequested = pvc.getSpec().getResources().getRequests().get("storage")
            .getNumber().toString();
        String storageUsed = pvc.getStatus().getCapacity().get("storage").getNumber().toString();

        builder.append("PVC Name: ").append(pvcName).append("\n");
        builder.append("Storage Requested: ").append(storageRequested).append("\n");
        builder.append("Storage Used: ").append(storageUsed).append("\n");
        builder.append("------\n");
      }
      return builder.toString();

    } catch (ApiException e) {
      System.err.println("Error fetching storage metrics: " + e.getMessage());
      e.printStackTrace();
    }
    return "";
  }
}
