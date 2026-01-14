package com.bgu.dirt.config;

public class DIRTConfig {
    // Verb POS tags to accept (Stanford parser notation)
    public static final String[] VERB_TAGS = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
    
    // Auxiliary verbs to filter out
    public static final String[] AUX_VERBS = {"is", "are", "am", "was", "were", "be", "been", "being",
                                               "have", "has", "had", "do", "does", "did"};
    
    // Noun POS tags
    public static final String[] NOUN_TAGS = {"NN", "NNS", "NNP", "NNPS"};
    
    // Content word POS tags (nouns, verbs, adjectives, adverbs)
    public static final String[] CONTENT_TAGS = {"NN", "NNS", "NNP", "NNPS", 
                                                  "VB", "VBD", "VBG", "VBN", "VBP", "VBZ",
                                                  "JJ", "JJR", "JJS",
                                                  "RB", "RBR", "RBS"};
    
    // Min frequency threshold for internal relations
    public static final int MIN_INTERNAL_RELATION_FREQ = 5;
    
    // Max path length (to avoid sparse data)
    public static final int MAX_PATH_LENGTH = 6;
    
    // Number of top similar paths to generate
    public static final int TOP_K_SIMILAR = 40;
    
    // Default threshold for similarity (can be tuned)
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1;
}
