package com.quickfind.recommendation;

import java.util.Map;

/** Total score plus the contribution of each signal, in the order they were computed. */
public record ScoreBreakdown(double total, Map<String, Double> components) {
}
