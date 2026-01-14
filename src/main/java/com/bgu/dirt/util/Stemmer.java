package com.bgu.dirt.util;

import opennlp.tools.stemmer.PorterStemmer;

/**
 * Stemming utility using Porter Stemmer (via OpenNLP).
 */
public class Stemmer {
    private static final PorterStemmer stemmer = new PorterStemmer();
    
    public static String stem(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return stemmer.stem(word.toLowerCase());
    }
    
    // Test method
    public static void main(String[] args) {
        System.out.println(stem("involves"));   // involv
        System.out.println(stem("involve"));    // involv
        System.out.println(stem("running"));    // run
        System.out.println(stem("manufactures")); // manufactur
    }
}