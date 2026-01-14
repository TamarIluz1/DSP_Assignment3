package com.bgu.dirt.parser;

import java.util.*;
import java.util.regex.*;

/**
 * Parses Google biarcs format (syntactic n-grams).
 * Format: ngram \t POS-tagged words \t dependency relations \t count
 * Example: "the dog" \t DT:the NN:dog \t det(dog, the) \t 1250
 */
public class BiarcParser {
    
    /**
     * Parse a single biarc line.
     * @return ParsedBiarc with words, POS tags, dependencies, and count
     */
    public static ParsedBiarc parse(String line) {
        String[] parts = line.split("\t");
        if (parts.length < 4) {
            return null; // Invalid format
        }
        
        String ngram = parts[0];
        String posLine = parts[1];
        String depLine = parts[2];
        long count = Long.parseLong(parts[3]);
        
        // Parse POS-tagged words: "DT:the NN:dog"
        Map<String, String> wordToPOS = parsePOSLine(posLine);
        
        // Parse dependencies: "det(dog, the) nsubj(dog, man)"
        List<Dependency> dependencies = parseDependencies(depLine);
        
        return new ParsedBiarc(ngram, wordToPOS, dependencies, count);
    }
    
    private static Map<String, String> parsePOSLine(String posLine) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : posLine.split(" ")) {
            if (token.contains(":")) {
                String[] parts = token.split(":", 2);
                String pos = parts[0];
                String word = parts[1];
                result.put(word, pos);
            }
        }
        return result;
    }
    
    private static List<Dependency> parseDependencies(String depLine) {
        List<Dependency> deps = new ArrayList<>();
        // Pattern: relation(head, dependent)
        Pattern p = Pattern.compile("(\\w+)\\((\\w+),\\s*(\\w+)\\)");
        Matcher m = p.matcher(depLine);
        while (m.find()) {
            String relation = m.group(1);
            String head = m.group(2);
            String dependent = m.group(3);
            deps.add(new Dependency(relation, head, dependent));
        }
        return deps;
    }
    
    /**
     * Container for parsed biarc data.
     */
    public static class ParsedBiarc {
        public String ngram;
        public Map<String, String> wordToPOS; // word -> POS
        public List<Dependency> dependencies;
        public long count;
        
        public ParsedBiarc(String ngram, Map<String, String> wordToPOS, 
                          List<Dependency> dependencies, long count) {
            this.ngram = ngram;
            this.wordToPOS = wordToPOS;
            this.dependencies = dependencies;
            this.count = count;
        }
    }
    
    /**
     * Represents a single dependency relation.
     */
    public static class Dependency {
        public String relation; // e.g., "nsubj", "dobj"
        public String head;
        public String dependent;
        
        public Dependency(String relation, String head, String dependent) {
            this.relation = relation;
            this.head = head;
            this.dependent = dependent;
        }
    }
}
