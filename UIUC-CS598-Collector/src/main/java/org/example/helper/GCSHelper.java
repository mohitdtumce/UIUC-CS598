package org.example.helper;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GCSHelper {

  private final GCPConfig config;

  private final Storage storage;

  public GCSHelper(GCPConfig gcpConfig) {
    this.config = gcpConfig;
    this.storage = StorageOptions.newBuilder().setProjectId(this.config.projectId).build()
        .getService();
  }

  public void authenticateImplicitWithAdc(String project) {
    System.out.println("Buckets:");
    Page<Bucket> buckets = storage.list();
    for (Bucket bucket : buckets.iterateAll()) {
      System.out.println(bucket.toString());
    }
    System.out.println("Listed all storage buckets.");
  }

  public void uploadToGCS(String data)
      throws IOException {
    BlobId blobId = BlobId.of(this.config.bucketName, this.config.filePath);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

    Blob blob = storage.get(blobId);

    if (blob != null && blob.exists()) {
      // Truncate the existing file
      blob.delete();
      System.out.println(
          "Existing file truncated: gs://" + this.config.bucketName + "/" + this.config.filePath);
    }

    // Upload the new data
    try (ByteArrayInputStream contentStream = new ByteArrayInputStream(
        data.getBytes(StandardCharsets.UTF_8))) {
      storage.createFrom(blobInfo, contentStream);
      System.out.println(
          "File uploaded to GCS: gs://" + this.config.bucketName + "/" + this.config.filePath);
    }
  }
}
