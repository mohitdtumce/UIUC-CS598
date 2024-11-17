package org.example.helper;


public class GCPConfig {

  public final String projectId;
  public final String region;
  public final String bucketName;
  public final String filePath;

  public GCPConfig(String projectId, String region, String bucketName, String filePath) {
    this.projectId = projectId;
    this.region = region;
    this.bucketName = bucketName;
    this.filePath = filePath;
  }

}
