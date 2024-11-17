package org.example.helper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AWSCredentialHelper {

  private final AwsCredentialsProvider credentialsProvider;
  private final String filePath;

  public AWSCredentialHelper(String filePath) throws IOException {
    this.filePath = filePath;
    this.credentialsProvider = createCredentialsProviderFromCSV(filePath);
  }

  private AwsCredentialsProvider createCredentialsProviderFromCSV(String filePath)
      throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      reader.readLine();
      // Skip the first line (header)
      String line = reader.readLine(); // Read the second line with credentials
      String[] parts = line.split(","); // Assuming comma-separated values
      if (parts.length == 2) {
        String accessKeyId = parts[0].trim();
        String secretAccessKey = parts[1].trim();
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
      } else {
        throw new IllegalArgumentException("Invalid credentials format in CSV file.");
      }
    }
  }

  public AwsCredentialsProvider getAwsCredentialProvider() {
    return this.credentialsProvider;
  }
}
