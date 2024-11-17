package uiuc.cs598.gslb.utils;

import software.amazon.awssdk.regions.Region;

public class AWSConfig {

  public final Region region;
  public final String bucketName;
  public final String filePath;

  public final String domain;

  public AWSConfig(String region, String bucketName, String filePath, String domain) {
    this.region = software.amazon.awssdk.regions.Region.of(region);
    this.bucketName = bucketName;
    this.filePath = filePath;
    this.domain = domain;
  }
}
