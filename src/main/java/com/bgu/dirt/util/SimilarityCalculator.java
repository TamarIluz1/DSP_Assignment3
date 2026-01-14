package com.bgu.dirt.util;

import com.bgu.dirt.model.FeatureVector;
import java.util.*;

/**
 * Computes Lin-style similarity between two paths.
 * Similarity(p1, p2) = sum of shared features / (sum of p1 features + sum of p2 features)
 */
public class SimilarityCalculator {
    
    public static double computeSimilarity(FeatureVector v1, FeatureVector v2) {
        Set<String> features1 = v1.getFeatureKeys();
        Set<String> features2 = v2.getFeatureKeys();
        
        // Sum of shared features
        double sharedSum = 0.0;
        for (String feature : features1) {
            if (features2.contains(feature)) {
                double w1 = v1.getFeatures().get(feature);
                double w2 = v2.getFeatures().get(feature);
                sharedSum += Math.min(w1, w2);
            }
        }
        
        // Sum of all features
        double totalSum = 0.0;
        for (double w : v1.getFeatures().values()) {
            totalSum += w;
        }
        for (double w : v2.getFeatures().values()) {
            totalSum += w;
        }
        
        if (totalSum == 0) return 0.0;
        return sharedSum / totalSum;
    }
}
