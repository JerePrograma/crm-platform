package com.gestudio.crm.imports;

import org.springframework.stereotype.Component;

@Component
public class NameSimilarityService {

  public double similarity(String left, String right) {
    if (left == null || right == null) {
      return 0;
    }
    if (left.equals(right)) {
      return 1;
    }
    int maximumLength = Math.max(left.length(), right.length());
    if (maximumLength == 0) {
      return 1;
    }
    return 1d - ((double) levenshteinDistance(left, right) / maximumLength);
  }

  private int levenshteinDistance(String left, String right) {
    int[] previous = new int[right.length() + 1];
    int[] current = new int[right.length() + 1];
    for (int column = 0; column <= right.length(); column++) {
      previous[column] = column;
    }
    for (int row = 1; row <= left.length(); row++) {
      current[0] = row;
      for (int column = 1; column <= right.length(); column++) {
        int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
        current[column] =
            Math.min(
                Math.min(current[column - 1] + 1, previous[column] + 1),
                previous[column - 1] + substitutionCost);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[right.length()];
  }
}
