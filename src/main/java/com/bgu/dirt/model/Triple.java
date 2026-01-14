package com.bgu.dirt.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents (path, slot, word) triple with frequency and MI.
 */
public class Triple implements Serializable {
    private String path;
    private String slot; // "SlotX" or "SlotY"
    private String word;
    private long frequency;
    private double mutualInformation;
    
    public Triple(String path, String slot, String word, long frequency) {
        this.path = path;
        this.slot = slot;
        this.word = word;
        this.frequency = frequency;
        this.mutualInformation = 0.0;
    }
    
    // Getters and setters
    public String getPath() { return path; }
    public String getSlot() { return slot; }
    public String getWord() { return word; }
    public long getFrequency() { return frequency; }
    public void setFrequency(long freq) { this.frequency = freq; }
    public double getMutualInformation() { return mutualInformation; }
    public void setMutualInformation(double mi) { this.mutualInformation = mi; }
    
    public String getKey() {
        return path + "\t" + slot + "\t" + word;
    }
    
    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%d\t%.4f", path, slot, word, frequency, mutualInformation);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple triple = (Triple) o;
        return Objects.equals(path, triple.path) &&
               Objects.equals(slot, triple.slot) &&
               Objects.equals(word, triple.word);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path, slot, word);
    }
}
