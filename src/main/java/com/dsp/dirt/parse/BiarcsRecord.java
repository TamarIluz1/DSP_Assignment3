package com.dsp.dirt.parse;

/**
 * TODO: adapt fields to your dataset (Biarcs / syntactic n-grams).
 * Keep whatever you need for path extraction: tokens, POS, dependencies, frequency, etc.
 */
public class BiarcsRecord {
    public final long count; // frequency for this record in the corpus

    public BiarcsRecord(long count) {
        this.count = count;
    }
}
