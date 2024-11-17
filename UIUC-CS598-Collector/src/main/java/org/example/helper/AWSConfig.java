package org.example.helper;

import software.amazon.awssdk.regions.Region;

public class AWSConfig {

  public final Region region;
  public final String bucketName;
  public final String filePath;

  public AWSConfig(String region, String bucketName, String filePath) {
    this.region = software.amazon.awssdk.regions.Region.of(region);
    this.bucketName = bucketName;
    this.filePath = filePath;
  }
}
