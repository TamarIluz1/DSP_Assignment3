package com.bgu.dirt.util;

import org.tartarus.snowball.ext.EnglishStemmer;

/**
 * Stemming utility using Porter Stemmer (Snowball).
 */
public class Stemmer {
    private static final EnglishStemmer stemmer = new EnglishStemmer();
    
    public static String stem(String word) {
        synchronized (stemmer) {
            stemmer.setCurrent(word.toLowerCase());
            stemmer.stem();
            return stemmer.getCurrent();
        }
    }
}
