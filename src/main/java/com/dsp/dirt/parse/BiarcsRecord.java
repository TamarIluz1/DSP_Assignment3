package com.dsp.dirt.parse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BiarcsRecord {
    public final String headWord;             // field 0
    public final List<BiarcsToken> tokens;    // parsed fragment
    public final long totalCount;             // field 2
    public final Map<Integer, Long> byYear;   // optional: year -> count (can be empty)

    public BiarcsRecord(String headWord, List<BiarcsToken> tokens, long totalCount, Map<Integer, Long> byYear) {
        this.headWord = headWord == null ? "" : headWord;
        this.tokens = tokens == null ? Collections.emptyList() : tokens;
        this.totalCount = totalCount;
        this.byYear = byYear == null ? Collections.emptyMap() : byYear;
    }

    public boolean isValid() { return totalCount > 0 && !tokens.isEmpty(); }

}
