package org.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

  public static long consistentHash(String input) {
    try {
      // Use a strong hash function like SHA-256
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));

      // Convert the hash bytes to a long
      long result = 0;
      for (int i = 0; i < 8; i++) {
        result = (result << 8) | (hash[i] & 0xFF);
      }
      return ((result % 128) + 128) % 128;

    } catch (NoSuchAlgorithmException e) {
      // Handle the exception appropriately
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
