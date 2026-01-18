package com.dsp.dirt.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiarcsParser {

    /**
     * README format:
     * head_word<TAB>syntactic-ngram<TAB>total_count<TAB>counts_by_year
     *
     * syntactic-ngram is tokens separated by spaces.
     * each token: word/pos-tag/dep-label/head-index
     * head-index is 1-based, 0 indicates root. (README)
     */
    public BiarcsRecord parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new BiarcsRecord("", Collections.<BiarcsToken>emptyList(), 0L, Collections.<Integer, Long>emptyMap());
        }

        String[] parts = line.split("\t");
        if (parts.length < 3) {
            return new BiarcsRecord("", Collections.<BiarcsToken>emptyList(), 0L, Collections.<Integer, Long>emptyMap());
        }

        String headWord = parts[0].trim();
        String ngram = parts[1].trim();

        long total;
        try {
            total = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException e) {
            return new BiarcsRecord(headWord, Collections.<BiarcsToken>emptyList(), 0L, Collections.<Integer, Long>emptyMap());
        }

        if (total <= 0 || ngram.isEmpty() || headWord.isEmpty()) {
            return new BiarcsRecord(headWord, Collections.<BiarcsToken>emptyList(), 0L, Collections.<Integer, Long>emptyMap());
        }

        List<BiarcsToken> tokens = parseTokens(ngram);
        Map<Integer, Long> byYear = parseYears(parts); // optional, safe

        return new BiarcsRecord(headWord, tokens, total, byYear);
    }

    private List<BiarcsToken> parseTokens(String ngram) {
        String[] specs = ngram.split("\\s+");
        List<BiarcsToken> out = new ArrayList<BiarcsToken>(specs.length);

        for (int i = 0; i < specs.length; i++) {
            BiarcsToken t = parseTokenSpec(i + 1, specs[i]);
            if (t != null) out.add(t);
        }
        return out;
    }

    /**
     * Token format: word/pos/dep/headIndex
     * The README guarantees pos/dep don't contain '/', but word can contain any non-whitespace char.
     * So we parse from the end: last 3 segments are pos, dep, headIndex; everything before is word.
     */
    private BiarcsToken parseTokenSpec(int index, String spec) {
        if (spec == null || spec.trim().isEmpty()) return null;

        String[] segs = spec.split("/");
        if (segs.length < 4) return null;

        String headIndexStr = segs[segs.length - 1];
        String dep = segs[segs.length - 2];
        String pos = segs[segs.length - 3];

        String word = String.join("/", Arrays.copyOfRange(segs, 0, segs.length - 3));

        int headIndex;
        try {
            headIndex = Integer.parseInt(headIndexStr);
        } catch (NumberFormatException e) {
            return null;
        }

        return new BiarcsToken(index, word, pos, dep, headIndex);
    }

    /**
     * counts_by_year starts at parts[3], each item is "year,count".
     */
    private Map<Integer, Long> parseYears(String[] parts) {
        if (parts.length <= 3) return Collections.emptyMap();

        Map<Integer, Long> m = new HashMap<Integer, Long>();
        for (int i = 3; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) continue;

            int comma = s.indexOf(',');
            if (comma <= 0 || comma >= s.length() - 1) continue;

            try {
                int year = Integer.parseInt(s.substring(0, comma));
                long c = Long.parseLong(s.substring(comma + 1));
                if (c > 0) m.put(year, c);
            } catch (NumberFormatException ignored) {
            }
        }
        return m;
    }
}
