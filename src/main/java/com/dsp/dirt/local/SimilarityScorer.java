package com.dsp.dirt.local;

import java.io.*;
import java.util.*;

public class SimilarityScorer {

    public static class Feats {
        // slot -> word -> mi
        public final Map<String, Map<String, Double>> slot2w = new HashMap<>();
        public void put(String slot, String w, double mi) {
            slot2w.computeIfAbsent(slot, k -> new HashMap<>()).put(w, mi);
        }
    }

    /**
     * Load MI lines:
     *   p \t slot \t w \t mi
     */
    public static Map<String, Feats> loadMI(File miFile, Set<String> onlyPaths) throws IOException {
        Map<String, Feats> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(miFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\\t");
                if (f.length < 4) continue;
                String p = f[0], slot = f[1], w = f[2];
                double mi = Double.parseDouble(f[3]);
                if (onlyPaths != null && !onlyPaths.contains(p)) continue;

                Feats feats = map.computeIfAbsent(p, k -> new Feats());
                feats.put(slot, w, mi);
            }
        }
        return map;
    }

    /**
     * TODO: implement exact DIRT similarity equation you will report.
     * Skeleton uses an overlap-based score:
     *   num = sum_{w in intersection} (mi1(w) + mi2(w))
     *   den = sum_{w in p1} mi1(w) + sum_{w in p2} mi2(w)
     */
    public static double slotSimilarity(Feats a, Feats b, String slot) {
        Map<String, Double> A = a == null ? Map.of() : a.slot2w.getOrDefault(slot, Map.of());
        Map<String, Double> B = b == null ? Map.of() : b.slot2w.getOrDefault(slot, Map.of());

        double num = 0.0;
        for (String w : A.keySet()) {
            if (B.containsKey(w)) {
                num += A.get(w) + B.get(w);
            }
        }

        double den = 0.0;
        for (double v : A.values()) den += v;
        for (double v : B.values()) den += v;

        if (den == 0.0) return 0.0;
        return num / den;
    }

    public static double pathSimilarity(Feats a, Feats b) {
        double sx = slotSimilarity(a, b, "X");
        double sy = slotSimilarity(a, b, "Y");
        return Math.sqrt(sx * sy);
    }
}
