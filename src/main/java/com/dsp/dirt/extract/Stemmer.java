package com.dsp.dirt.extract;

/**
 * Placeholder stemmer.
 * TODO: replace with a real Porter stemmer (or any required stemmer) if the assignment expects it.
 */
public class Stemmer {
    public String stem(String w) {
        return w == null ? "" : w.toLowerCase();
    }
}
