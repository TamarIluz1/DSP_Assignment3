package com.bgu.dirt.model;

import java.io.Serializable;
import java.util.*;

/**
 * Feature vector for a path: maps (slot, word) to weighted features.
 */
public class FeatureVector implements Serializable {
    private String path;
    private Map<String, Double> features; // key = "slot:word", value = weight (frequency or MI)
    private double norm; // L2 norm for cosine similarity
    
    public FeatureVector(String path) {
        this.path = path;
        this.features = new HashMap<>();
        this.norm = 0.0;
    }
    
    public void addFeature(String slot, String word, double weight) {
        String featureKey = slot + ":" + word;
        features.put(featureKey, weight);
    }
    
    public void computeNorm() {
        norm = 0.0;
        for (double w : features.values()) {
            norm += w * w;
        }
        norm = Math.sqrt(norm);
    }
    
    public String getPath() { return path; }
    public Map<String, Double> getFeatures() { return features; }
    public double getNorm() { return norm; }
    public Set<String> getFeatureKeys() { return features.keySet(); }
    
    @Override
    public String toString() {
        return path + "\t" + features.toString();
    }
}
