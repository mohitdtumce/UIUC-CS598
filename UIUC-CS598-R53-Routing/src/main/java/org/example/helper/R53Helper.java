package org.example;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.helper.AWSConfig;
import org.example.helper.AWSCredentialHelper;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GeoLocation;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.route53.model.Route53Exception;

public class R53Helper {

  private final AWSConfig config;

  private final AWSCredentialHelper credentialHelper;
  private final Route53Client route53Client;

  public R53Helper(AWSConfig awsConfig, AWSCredentialHelper credentialHelper) {
    this.config = awsConfig;
    this.credentialHelper = credentialHelper;
    this.route53Client = Route53Client.builder()
        .credentialsProvider(this.credentialHelper.getAwsCredentialProvider())
        .region(this.config.region) // Replace with your desired region
        .endpointOverride(URI.create("https://route53.amazonaws.com")) // Optional endpoint override
        .build();
  }

  public List<String> listHostedZoneId() {
    ListHostedZonesRequest request = ListHostedZonesRequest.builder().build();
    ListHostedZonesResponse response = route53Client.listHostedZones(request);

    List<String> zones = new ArrayList<>();
    for (HostedZone zone : response.hostedZones()) {
      System.out.println(zone.toString());
      zones.add(zone.toString());
    }
    return zones;
  }

  HostedZone findOrCreateHostedZone(String domain) {
    HostedZone zone = findHostedZone(domain);
    if (zone == null) {
      zone = createHostedZone(domain);
    }
    return zone;
  }

  HostedZone findHostedZone(String domain) {
    ListHostedZonesResponse response = route53Client.listHostedZones(
        ListHostedZonesRequest.builder().build());

    for (HostedZone zone : response.hostedZones()) {
      System.out.println(zone.toString());
      if (Objects.equals(zone.name(), domain)) {
        return zone;
      }
    }
    return null;
  }

  HostedZone createHostedZone(String domainName) {
    try {
      CreateHostedZoneRequest request = CreateHostedZoneRequest.builder()
          .name(domainName)
          .callerReference(new RandomStringUtils().nextAlphanumeric(10))
          .build();

      CreateHostedZoneResponse response = route53Client.createHostedZone(request);

      // Access hosted zone details from the response
      System.out.println("Hosted Zone Name: " + response.hostedZone().name());
      System.out.println("Hosted Zone ID: " + response.hostedZone().id());
      return response.hostedZone();
    } catch (Route53Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  ChangeResourceRecordSetsResponse createCNAMERecord(
      String hostedZoneId, String domain, String geoSubdomain, GeoLocation geoLocation) {
    ResourceRecordSet cnameRecordSet = ResourceRecordSet.builder()
        .name(domain)  // Subdomain: us-east-1.ucd.cs598.sharma83
        .type(RRType.CNAME)
        .setIdentifier(domain)
        .geoLocation(geoLocation) // Geolocation settings
        .ttl(300L)
        .resourceRecords(ResourceRecord.builder().value(geoSubdomain).build())
        .build();
    return changeResourceRecordSets(hostedZoneId, cnameRecordSet, ChangeAction.CREATE);
  }

  void createWeightedARecords(String hostedZoneId,
      String subdomain, List<WeightedRecord> records) {
    for (WeightedRecord record : records) {
      var aRecordSet = ResourceRecordSet.builder()
          .name(subdomain)
          .type(RRType.A)
          .setIdentifier(record.identifier)
          .weight(record.weight)
          .ttl(300L)
          .resourceRecords(
              ResourceRecord.builder().value(record.ipv4).build())
          .build();
      changeResourceRecordSets(hostedZoneId, aRecordSet, ChangeAction.CREATE);
    }
  }

  ChangeResourceRecordSetsResponse changeResourceRecordSets(String hostedZoneId,
      ResourceRecordSet recordSet, ChangeAction changeAction) {
    ChangeResourceRecordSetsRequest request = ChangeResourceRecordSetsRequest.builder()
        .hostedZoneId(hostedZoneId)
        .changeBatch(ChangeBatch.builder()
            .changes(Change.builder()
                .action(changeAction)
                .resourceRecordSet(recordSet)
                .build())
            .build())
        .build();
    try {
      return route53Client.changeResourceRecordSets(request);
    } catch (Route53Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  void updateWeightedRecords(String hostedZoneId, String subdomain,
      List<WeightedRecord> updatedRecords) {
    for (WeightedRecord record : updatedRecords) {
      var aRecordSet = ResourceRecordSet.builder()
          .name(subdomain) // Same subdomain as existing weighted records
          .type(RRType.A)
          .setIdentifier(record.identifier) // Unique identifier for the weighted record
          .weight(record.weight)
          .ttl(300L)
          .resourceRecords(
              ResourceRecord.builder().value(record.ipv4).build()) // Existing IP address
          .build();

      changeResourceRecordSets(hostedZoneId, aRecordSet, ChangeAction.UPSERT);
    }
  }

  static class WeightedRecord {

    String identifier;
    String ipv4;
    Long weight;

    public WeightedRecord(String identifier, String ipv4, Long weight) {
      this.identifier = identifier;
      this.ipv4 = ipv4;
      this.weight = weight;
    }
  }
}