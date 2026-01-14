package com.bgu.dirt.model;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a dependency path in a tree.
 * Format: N:subj:V!find\"V:obj:N\"solution\"N:to:N
 * Root is the verb (or noun) at the center.
 */
public class Path implements Serializable, Comparable<Path> {
    private String pathStr;
    private String root;
    private String slotX;
    private String slotY;
    private List<String> relations; // all relations including slots
    
    public Path(String pathStr, String root, String slotX, String slotY, List<String> relations) {
        this.pathStr = pathStr;
        this.root = root;
        this.slotX = slotX;
        this.slotY = slotY;
        this.relations = relations;
    }
    
    public String getPathStr() { return pathStr; }
    public String getRoot() { return root; }
    public String getSlotX() { return slotX; }
    public String getSlotY() { return slotY; }
    public List<String> getRelations() { return relations; }
    
    @Override
    public String toString() { return pathStr; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Objects.equals(pathStr, path.pathStr);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pathStr);
    }
    
    @Override
    public int compareTo(Path o) {
        return this.pathStr.compareTo(o.pathStr);
    }
}
