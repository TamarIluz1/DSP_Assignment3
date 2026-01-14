package com.bgu.dirt.parser;

import com.bgu.dirt.model.Path;
import com.bgu.dirt.config.DIRTConfig;
import java.util.*;

/**
 * Extracts paths from dependency trees following DIRT algorithm.
 * 
 * Constraints:
 * - Slot fillers must be nouns
 * - Only relations between content words (nouns, verbs, adj, adv)
 * - Head must be a verb (filter auxiliaries)
 * - Include prepositions (IN, TO)
 */
public class DependencyTreeExtractor {
    
    private PathConstraintFilter filter;
    
    public DependencyTreeExtractor() {
        this.filter = new PathConstraintFilter();
    }
    
    /**
     * Extract all valid paths from a parsed biarc.
     */
    public List<Path> extractPaths(BiarcParser.ParsedBiarc biarc) {
        List<Path> paths = new ArrayList<>();
        
        // For each word in the biarc, try to find path if it can be a root
        for (String word : biarc.wordToPOS.keySet()) {
            String pos = biarc.wordToPOS.get(word);
            
            // Root must be a verb (non-auxiliary)
            if (!isVerb(pos) || isAuxiliaryVerb(word)) {
                continue;
            }
            
            // Find all paths with this verb as root
            List<Path> pathsFromRoot = findPathsFromRoot(word, biarc);
            paths.addAll(pathsFromRoot);
        }
        
        return paths;
    }
    
    private List<Path> findPathsFromRoot(String root, BiarcParser.ParsedBiarc biarc) {
        List<Path> result = new ArrayList<>();
        
        // For each pair of nouns in the biarc, try to find a path connecting them
        // through the root verb using the dependency tree.
        
        List<String> nouns = new ArrayList<>();
        for (String word : biarc.wordToPOS.keySet()) {
            if (isNoun(biarc.wordToPOS.get(word))) {
                nouns.add(word);
            }
        }
        
        for (int i = 0; i < nouns.size(); i++) {
            for (int j = i + 1; j < nouns.size(); j++) {
                String noun1 = nouns.get(i);
                String noun2 = nouns.get(j);
                
                // Try to find path from noun1 to noun2 through root
                Path path1to2 = findPath(noun1, noun2, root, biarc);
                if (path1to2 != null) {
                    result.add(path1to2);
                }
                
                // Try reverse direction
                Path path2to1 = findPath(noun2, noun1, root, biarc);
                if (path2to1 != null) {
                    result.add(path2to1);
                }
            }
        }
        
        return result;
    }
    
    private Path findPath(String sourceNoun, String targetNoun, String root, BiarcParser.ParsedBiarc biarc) {
        // TODO: Implement BFS/DFS to find path in dependency tree
        // For now, return null (skeleton)
        return null;
    }
    
    private boolean isVerb(String pos) {
        for (String verbTag : DIRTConfig.VERB_TAGS) {
            if (pos.equals(verbTag)) return true;
        }
        return false;
    }
    
    private boolean isNoun(String pos) {
        for (String nounTag : DIRTConfig.NOUN_TAGS) {
            if (pos.equals(nounTag)) return true;
        }
        return false;
    }
    
    private boolean isContentWord(String pos) {
        for (String tag : DIRTConfig.CONTENT_TAGS) {
            if (pos.equals(tag)) return true;
        }
        return false;
    }
    
    private boolean isAuxiliaryVerb(String word) {
        String lowerWord = word.toLowerCase();
        for (String aux : DIRTConfig.AUX_VERBS) {
            if (lowerWord.equals(aux)) return true;
        }
        return false;
    }
}
