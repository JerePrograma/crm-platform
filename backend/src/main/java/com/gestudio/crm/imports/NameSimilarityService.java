package com.gestudio.crm.imports;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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

    double characterSimilarity =
        1d - ((double) levenshteinDistance(left, right) / maximumLength);
    Set<String> leftTokens = tokens(left);
    Set<String> rightTokens = tokens(right);
    Set<String> union = new HashSet<>(leftTokens);
    union.addAll(rightTokens);
    Set<String> intersection = new HashSet<>(leftTokens);
    intersection.retainAll(rightTokens);
    double tokenSimilarity = union.isEmpty() ? 1d : (double) intersection.size() / union.size();
    double score = characterSimilarity * 0.8d + tokenSimilarity * 0.2d;

    Set<String> leftNumbers = numericTokens(leftTokens);
    Set<String> rightNumbers = numericTokens(rightTokens);
    if (!leftNumbers.isEmpty() && !rightNumbers.isEmpty() && !leftNumbers.equals(rightNumbers)) {
      return Math.min(score, 0.79d);
    }
    return score;
  }

  private Set<String> tokens(String value) {
    return Arrays.stream(value.split("\\s+"))
        .filter(token -> !token.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private Set<String> numericTokens(Set<String> tokens) {
    return tokens.stream()
        .filter(token -> token.chars().allMatch(Character::isDigit))
        .collect(Collectors.toUnmodifiableSet());
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
