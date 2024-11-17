package org.example;

import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.example.helper.AWSConfig;
import org.example.helper.AWSCredentialHelper;
import org.example.helper.GCPConfig;

public class Main {

  public static void main(String[] args) throws IOException, CsvValidationException {
    GCPConfig gcpConfig = new GCPConfig("mohitshr-learning",
        "us-central1", "mohitshr-project-gslb-us-central1", "InstanceHealth.csv");

    AWSConfig awsConfig = new AWSConfig("us-east-1",
        "mohitshr-project-gslb-us-central1", "InstanceHealth.csv", "uiuc.cs598.sharma83.");
    AWSCredentialHelper credentialHelper = new AWSCredentialHelper(
        "/usr/local/google/home/mohitshr/.ssh/aws_access_keys.csv");

    Runnable task = new CollatorService(gcpConfig, awsConfig);

    // Schedule the task
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down scheduler...");
      scheduler.shutdown();
    }));
  }
}