package com.dsp.dirt.parse;

public class BiarcsToken {
    public final int index;        // 1-based position in the fragment
    public final String word;      // lexical form (lowercased in corpus)
    public final String pos;       // Penn Treebank tag
    public final String depLabel;  // Stanford basic dependency label
    public final int headIndex;    // 0=root, otherwise 1..N

    public BiarcsToken(int index, String word, String pos, String depLabel, int headIndex) {
        this.index = index;
        this.word = word;
        this.pos = pos;
        this.depLabel = depLabel;
        this.headIndex = headIndex;
    }
}