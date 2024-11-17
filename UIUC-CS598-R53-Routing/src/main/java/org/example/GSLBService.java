package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.example.R53Helper.WeightedRecord;
import org.example.helper.AWSConfig;
import org.example.helper.AWSCredentialHelper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.model.GeoLocation;
import software.amazon.awssdk.services.route53.model.HostedZone;

public class GSLBService {

  private final AWSConfig config;

  private final AWSCredentialHelper credentialHelper;
  private final R53Helper helper;

  private final Map<GeoLocation, List<Region>> geoMapping = Map.of(
      GeoLocation.builder().countryCode("US").build(), List.of(Region.US_EAST_1, Region.US_EAST_2),
      GeoLocation.builder().countryCode("IN").build(),
      List.of(Region.AP_NORTHEAST_1, Region.AP_NORTHEAST_2)
  );

  GSLBService(AWSConfig config, AWSCredentialHelper credentialHelper) {
    this.config = config;
    this.credentialHelper = credentialHelper;
    this.helper = new R53Helper(config, credentialHelper);
  }

  public void setupGSLB() throws IOException {
    // Get the hosted zone ID

    HostedZone zone = helper.findOrCreateHostedZone(this.config.domain);
    System.out.println("Found hosted zone:" + zone);

    for (Map.Entry<GeoLocation, List<Region>> entry : geoMapping.entrySet()) {
      for (Region region : entry.getValue()) {
        String geoSubdomain = String.format("%s.%s", region, this.config.domain);

        String weightedSubdomain = String.format("weighted.%s", geoSubdomain);
        System.out.println(
            helper.createCNAMERecord(zone.id(), geoSubdomain, weightedSubdomain, entry.getKey()));

        List<WeightedRecord> records = new ArrayList<>();

        records.add(createWeightedRecord("aws", region, weightedSubdomain, 40L));
        records.add(createWeightedRecord("azure", region, weightedSubdomain, 30L));
        records.add(createWeightedRecord("gcp", region, weightedSubdomain, 20L));
        helper.createWeightedARecords(zone.id(), weightedSubdomain, records);
      }
    }
  }

  public void updateWeight(GeoLocation geoLocation, Region region, Route53Weights weights)
      throws IOException {
    // Get the hosted zone ID

    HostedZone zone = helper.findOrCreateHostedZone(this.config.domain);
    System.out.println("Found hosted zone:" + zone);

    String weightedSubdomain = String.format("weighted.%s.%s", region, this.config.domain);

    List<WeightedRecord> records = new ArrayList<>();

    records.add(createWeightedRecord("aws", region, weightedSubdomain, weights.awsWeight));
    records.add(createWeightedRecord("azure", region, weightedSubdomain, weights.azureWeight));
    records.add(createWeightedRecord("gcp", region, weightedSubdomain, weights.gcpWeight));
    helper.updateWeightedRecords(zone.id(), weightedSubdomain, records);
  }

  private R53Helper.WeightedRecord createWeightedRecord(String provider, Region region,
      String weightedSubdomain, Long weight) {
    String identifier = String.format("%s.%s", provider, weightedSubdomain).trim();
    return switch (provider) {
      case "aws" -> new WeightedRecord(
          identifier,
          String.format("10.0.0.%d", Utils.consistentHash(provider + region)),
          weight
      );
      case "gcp" -> new WeightedRecord(
          identifier,
          String.format("172.16.0.%d", Utils.consistentHash(provider + region)),
          weight
      );
      default -> new WeightedRecord(
          identifier,
          String.format("192.168.0.%d", Utils.consistentHash(provider + region)),
          weight
      );
    };
  }

  public static class Route53Weights {

    public long awsWeight;
    public long azureWeight;
    public long gcpWeight;

    public Route53Weights(long awsWeight, long azureWeight, long gcpWeight) {
      this.awsWeight = awsWeight;
      this.azureWeight = azureWeight;
      this.gcpWeight = gcpWeight;
    }

    @Override
    public String toString() {
      return String.format("AWS: %d, Azure: %d, GCP: %d", awsWeight, azureWeight, gcpWeight);
    }
  }
}
