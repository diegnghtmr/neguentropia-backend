package com.neguentropia.similarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TfIdf {
    private TfIdf() {
    }

    public static double[][] fit(List<List<String>> documents) {
        LinkedHashMap<String, Integer> vocabulary = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> documentFrequency = new LinkedHashMap<>();
        ArrayList<Map<String, Integer>> termCounts = new ArrayList<>();
        for (List<String> tokens : documents) {
            LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
            for (String token : tokens) {
                if (!vocabulary.containsKey(token)) {
                    vocabulary.put(token, vocabulary.size());
                }
                counts.put(token, counts.getOrDefault(token, 0) + 1);
            }
            for (String term : counts.keySet()) {
                documentFrequency.put(term, documentFrequency.getOrDefault(term, 0) + 1);
            }
            termCounts.add(counts);
        }
        double[] idf = new double[vocabulary.size()];
        for (Map.Entry<String, Integer> term : vocabulary.entrySet()) {
            idf[term.getValue()] = Math.log((documents.size() + 1.0)
                    / (documentFrequency.get(term.getKey()) + 1.0)) + 1;
        }
        double[][] vectors = new double[documents.size()][vocabulary.size()];
        for (int document = 0; document < documents.size(); document++) {
            for (Map.Entry<String, Integer> term : vocabulary.entrySet()) {
                int index = term.getValue();
                vectors[document][index] = termCounts.get(document).getOrDefault(term.getKey(), 0)
                        / (double) documents.get(document).size() * idf[index];
            }
        }
        return vectors;
    }

    public static double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftSquared = 0;
        double rightSquared = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftSquared += left[i] * left[i];
            rightSquared += right[i] * right[i];
        }
        double cosine = dot / (Math.sqrt(leftSquared) * Math.sqrt(rightSquared));
        if (cosine > 1) {
            return 1;
        }
        return cosine;
    }
}
