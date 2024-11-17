package org.example.helper;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GCSHelper {

  private final GCPConfig config;

  private final Storage storage;

  public GCSHelper(GCPConfig gcpConfig) {
    this.config = gcpConfig;
    this.storage = StorageOptions.newBuilder().setProjectId(this.config.projectId).build()
        .getService();
  }

  public List<InstanceData> readGCSInstances() throws IOException, CsvValidationException {
    List<InstanceData> instances = new ArrayList<>();

    // Fetch the blob's content as a byte array
    Blob blob = storage.get(this.config.bucketName, this.config.filePath);
    byte[] blobContent = blob.getContent();

    // Use ByteArrayInputStream to process the content in memory
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(blobContent);
        CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
      String[] nextLine;
      reader.readNext();
      while ((nextLine = reader.readNext()) != null) {
        // Read and store the data from CSV (specific to GCP CSV structure)
        String instanceName = nextLine[0];
        String availabilityZone = nextLine[1];
        String state = nextLine[2];
        double cpuUtilization = Double.parseDouble(nextLine[3]);
        double ramUsed = Double.parseDouble(nextLine[4]);
        double ramSize = Double.parseDouble(nextLine[5]);
        double networkIn = Double.parseDouble(nextLine[6]);
        double networkOut = Double.parseDouble(nextLine[7]);
        double averageIOLatency = Double.parseDouble(nextLine[8]);
        double diskReadBytes = Double.parseDouble(nextLine[9]);
        double diskWriteBytes = Double.parseDouble(nextLine[10]);

        instances.add(
            new InstanceData(instanceName, availabilityZone, state, cpuUtilization, ramUsed,
                ramSize, networkIn, networkOut, averageIOLatency, diskReadBytes, diskWriteBytes));
      }
    }

    return instances;
  }

}
