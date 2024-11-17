package org.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.example.helper.AWSConfig;
import org.example.helper.GCEHelper;
import org.example.helper.GCPConfig;
import org.example.helper.GCSHelper;

public class Main {

  public static void main(String[] args) throws Exception {

    GCPConfig gcpConfig = new GCPConfig("mohitshr-learning",
        "us-central1", "mohitshr-project-gslb-us-central1", "InstanceHealth.csv");


    AWSConfig awsConfig = new AWSConfig("us-east-1",
        "mohitshr-project-gslb-us-central1","InstanceHealth.csv");

    Runnable task = new MetricsUploaderTask(gcpConfig, awsConfig);

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