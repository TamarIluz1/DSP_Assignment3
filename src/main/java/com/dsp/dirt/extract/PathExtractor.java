package com.dsp.dirt.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dsp.dirt.parse.BiarcsRecord;
import com.dsp.dirt.parse.BiarcsToken;

import static com.dsp.dirt.util.Keys.X;
import static com.dsp.dirt.util.Keys.Y;

public class PathExtractor {

    private final Stemmer stemmer = new Stemmer();

    // Auxiliary verb / function-word filter (RAW, before stemming).
    private static final Set<String> FILTER_AUX_RAW;
    static {
        Set<String> s = new HashSet<String>();
        // be + forms (note: Porter stems "are" -> "ar", so we include both)
        s.add("be"); s.add("am"); s.add("is"); s.add("are"); s.add("ar");
        s.add("was"); s.add("were"); s.add("been"); s.add("being");
        // do
        s.add("do"); s.add("does"); s.add("did");
        // have
        s.add("have"); s.add("has"); s.add("had");
        // modals
        s.add("can"); s.add("could"); s.add("will"); s.add("would");
        s.add("shall"); s.add("should"); s.add("may"); s.add("might"); s.add("must");
        // contractions (if they ever appear as tokens)
        s.add("'m"); s.add("'re"); s.add("'s"); s.add("'ve"); s.add("'d"); s.add("'ll");
        FILTER_AUX_RAW = Collections.unmodifiableSet(s);
    }

    private boolean isAuxiliaryRaw(String w) {
        if (w == null) return false;
        String raw = w.trim().toLowerCase();
        return FILTER_AUX_RAW.contains(raw);
    }

    public List<PathInstance> extract(BiarcsRecord r) {
        if (r == null || !r.isValid() || r.tokens == null || r.tokens.isEmpty()) {
            return Collections.emptyList();
        }

        int n = r.tokens.size();

        // Build 1-based token array for O(1) lookup by index.
        BiarcsToken[] tok = new BiarcsToken[n + 1];
        for (BiarcsToken t : r.tokens) {
            if (t != null && t.index >= 1 && t.index <= n) tok[t.index] = t;
        }

        // parent[i] = headIndex (0 is root)
        int[] parent = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            if (tok[i] == null) continue;
            int h = tok[i].headIndex;
            parent[i] = (h >= 0 && h <= n) ? h : 0;
        }

        // Collect candidate noun slots (X/Y must be nouns).
        List<Integer> nouns = new ArrayList<Integer>();
        for (int i = 1; i <= n; i++) {
            if (tok[i] == null) continue;
            if (isNoun(tok[i].pos) && isGoodWord(tok[i].word)) nouns.add(i);
        }
        if (nouns.size() < 2) return Collections.emptyList();

        long count = r.totalCount;
        if (count <= 0) return Collections.emptyList();

        List<PathInstance> out = new ArrayList<PathInstance>();

        for (int aPos = 0; aPos < nouns.size(); aPos++) {
            for (int bPos = aPos + 1; bPos < nouns.size(); bPos++) {
                int a = nouns.get(aPos);
                int b = nouns.get(bPos);

                if (tok[a] == null || tok[b] == null) continue;

                int l = lca(a, b, parent);
                if (l == 0 || tok[l] == null) continue;

                // Requirement: head must be a verb
                if (!isVerb(tok[l].pos)) continue;

                // Highly recommended: filter auxiliary verb heads
                if (isAuxiliaryRaw(tok[l].word)) continue;

                // AB direction: X=a, Y=b
                List<Integer> pathAB = dependencyPath(a, b, l, parent);
                if (!pathAB.isEmpty()) {
                    String pXY = buildPredicate(pathAB, tok, a, b);
                    if (!pXY.isEmpty()) {
                        out.add(new PathInstance(pXY, X, stemmer.stem(tok[a].word), count));
                        out.add(new PathInstance(pXY, Y, stemmer.stem(tok[b].word), count));
                    }
                }

                // BA direction: X=b, Y=a  (must recompute path order)
                List<Integer> pathBA = dependencyPath(b, a, l, parent);
                if (!pathBA.isEmpty()) {
                    String pYX = buildPredicate(pathBA, tok, b, a);
                    if (!pYX.isEmpty()) {
                        out.add(new PathInstance(pYX, X, stemmer.stem(tok[b].word), count));
                        out.add(new PathInstance(pYX, Y, stemmer.stem(tok[a].word), count));
                    }
                }
            }
        }

        return out;
    }

    private String buildPredicate(List<Integer> path, BiarcsToken[] tok, int xIdx, int yIdx) {
        List<String> parts = new ArrayList<String>();

        for (int idx : path) {
            if (idx == xIdx) { parts.add("X"); continue; }
            if (idx == yIdx) { parts.add("Y"); continue; }

            BiarcsToken t = tok[idx];
            if (t == null) continue;

            // Include: verbs, adjectives, adverbs, particles, and crucially IN/TO prepositions.
            if (!shouldIncludeInPredicate(t.pos)) continue;

            // Basic token sanity
            if (!isGoodWord(t.word)) continue;

            // Filter auxiliaries anywhere on the path (prevents are->ar, etc.)
            if (isVerb(t.pos) && isAuxiliaryRaw(t.word)) continue;
            if ((isVerb(t.pos) || "MD".equals(t.pos)) && isAuxiliaryRaw(t.word)) continue;


            parts.add(stemmer.stem(t.word));
        }

        parts = collapseAdjacent(parts);

        // Must contain both slots
        if (!parts.contains("X") || !parts.contains("Y")) return "";

        String pred = joinWithSpace(parts).trim();

        // Degenerate paths
        if (pred.equals("X Y") || pred.equals("Y X")) return "";

        return pred;
    }

    private List<String> collapseAdjacent(List<String> in) {
        if (in.isEmpty()) return in;
        List<String> out = new ArrayList<String>(in.size());
        String prev = null;
        for (String s : in) {
            if (prev == null || !prev.equals(s)) out.add(s);
            prev = s;
        }
        return out;
    }

    private String joinWithSpace(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /** Path as node indices from a -> ... -> lca -> ... -> b (node sequence). */
    private List<Integer> dependencyPath(int a, int b, int lca, int[] parent) {
        List<Integer> left = new ArrayList<Integer>();
        int x = a;
        while (x != 0 && x != lca) {
            left.add(x);
            x = parent[x];
        }
        left.add(lca);

        List<Integer> right = new ArrayList<Integer>();
        int y = b;
        while (y != 0 && y != lca) {
            right.add(y);
            y = parent[y];
        }
        Collections.reverse(right);

        List<Integer> path = new ArrayList<Integer>(left.size() + right.size());
        path.addAll(left);
        path.addAll(right);
        return path;
    }

    private int lca(int a, int b, int[] parent) {
        Set<Integer> anc = new HashSet<Integer>();
        int x = a;
        while (x != 0) {
            anc.add(x);
            x = parent[x];
        }
        int y = b;
        while (y != 0) {
            if (anc.contains(y)) return y;
            y = parent[y];
        }
        return 0;
    }

    private boolean isNoun(String pos) { return pos != null && pos.startsWith("NN"); }
    private boolean isVerb(String pos) { return pos != null && pos.startsWith("VB"); }

    private boolean shouldIncludeInPredicate(String pos) {
        if (pos == null) return false;
        return pos.startsWith("VB") || pos.startsWith("JJ") || pos.startsWith("RB")
                || "IN".equals(pos) || "TO".equals(pos) || "RP".equals(pos);
    }

    /**
     * Keep tokens that contain at least one LETTER.
     * (Digits-only / punctuation-only tokens add noise and won't match your gold predicates anyway.)
     */
    private boolean isGoodWord(String w) {
        if (w == null) return false;
        w = w.trim();
        if (w.isEmpty()) return false;
        if ("_".equals(w)) return false;
        if ("•".equals(w) || "§".equals(w)) return false;

        boolean hasLetter = false;
        for (int i = 0; i < w.length(); i++) {
            if (Character.isLetter(w.charAt(i))) { hasLetter = true; break; }
        }
        return hasLetter;
    }
}
