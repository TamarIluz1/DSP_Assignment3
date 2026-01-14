package com.dsp.dirt.extract;

public class PathInstance {
    public final String pathKey;   // canonical representation of dependency path p
    public final String slot;      // "X" or "Y"
    public final String filler;    // word filling the slot (noun)
    public final long count;       // occurrence count from corpus line

    public PathInstance(String pathKey, String slot, String filler, long count) {
        this.pathKey = pathKey;
        this.slot = slot;
        this.filler = filler;
        this.count = count;
    }
}
