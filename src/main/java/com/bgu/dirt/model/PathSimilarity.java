package com.bgu.dirt.model;

import java.io.Serializable;

/**
 * Similarity between two paths.
 */
public class PathSimilarity implements Serializable, Comparable<PathSimilarity> {
    private String path1;
    private String path2;
    private double similarity;
    
    public PathSimilarity(String path1, String path2, double similarity) {
        this.path1 = path1;
        this.path2 = path2;
        this.similarity = similarity;
    }
    
    public String getPath1() { return path1; }
    public String getPath2() { return path2; }
    public double getSimilarity() { return similarity; }
    
    @Override
    public int compareTo(PathSimilarity o) {
        // Sort by similarity descending
        return Double.compare(o.similarity, this.similarity);
    }
    
    @Override
    public String toString() {
        return String.format("%s\t%s\t%.6f", path1, path2, similarity);
    }
}
