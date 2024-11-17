package org.example.helper;


public class InstanceData {

  public String instanceName;
  public String availabilityZone;
  public String state;
  public double cpuUtilization;
  public double ramUsed;
  public double ramSize;
  public double networkIn;
  public double networkOut;
  public double averageIOLatency;
  public double diskReadBytes;
  public double diskWriteBytes;

  // Constructor for GCP instances
  public InstanceData(String instanceName, String availabilityZone, String state,
      double cpuUtilization,
      double ramUsed, double ramSize, double networkIn, double networkOut,
      double averageIOLatency, double diskReadBytes, double diskWriteBytes) {
    this.instanceName = instanceName;
    this.availabilityZone = availabilityZone;
    this.state = state;
    this.cpuUtilization = cpuUtilization;
    this.ramUsed = ramUsed;
    this.ramSize = ramSize;
    this.networkIn = networkIn;
    this.networkOut = networkOut;
    this.averageIOLatency = averageIOLatency;
    this.diskReadBytes = diskReadBytes;
    this.diskWriteBytes = diskWriteBytes;
  }

  // Constructor for AWS instances
  public InstanceData(String instanceId, String availabilityZone, String state,
      double cpuUtilization,
      double networkIn, double networkOut, double diskReadBytes, double diskWriteBytes) {
    this.instanceName = instanceId;
    this.availabilityZone = availabilityZone;
    this.state = state;
    this.cpuUtilization = cpuUtilization;
    this.ramUsed = 0;  // Not available in AWS data
    this.ramSize = 0;  // Not available in AWS data
    this.networkIn = networkIn;
    this.networkOut = networkOut;
    this.averageIOLatency = 0;  // Not available in AWS data
    this.diskReadBytes = diskReadBytes;
    this.diskWriteBytes = diskWriteBytes;
  }

  @Override
  public String toString() {
    return "InstanceData{" +
        "instanceName='" + instanceName + '\'' +
        ", availabilityZone='" + availabilityZone + '\'' +
        ", state='" + state + '\'' +
        ", cpuUtilization=" + cpuUtilization +
        ", ramUsed=" + ramUsed +
        ", ramSize=" + ramSize +
        ", networkIn=" + networkIn +
        ", networkOut=" + networkOut +
        ", averageIOLatency=" + averageIOLatency +
        ", diskReadBytes=" + diskReadBytes +
        ", diskWriteBytes=" + diskWriteBytes +
        '}';
  }
}

