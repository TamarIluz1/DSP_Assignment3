package com.dsp.dirt.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.dsp.dirt.util.Keys.X;
import static com.dsp.dirt.util.Keys.Y;

public class SimilarityScorer {

    public static class Feats {
        public final Map<String, Double> x = new HashMap<String, Double>();
        public final Map<String, Double> y = new HashMap<String, Double>();
    }

    /**
     * Loads MI features from mi.tsv.
     * Expected columns: pred \t slot \t arg \t mi
     *
     * NOTE: We keep MI values as-is, but for similarity we will ignore non-positive MI
     * (common practical choice; reduces noise).
     */
    public static Map<String, Feats> loadMI(File miTsv, Set<String> neededPredicates) throws IOException {
        Map<String, Feats> out = new HashMap<String, Feats>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(miTsv));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;

                String[] p = line.split("\\t");
                if (p.length < 4) continue;

                String pred = normalizePredKey(p[0]);
                if (pred.isEmpty()) continue;

                if (neededPredicates != null && !neededPredicates.isEmpty() && !neededPredicates.contains(pred)) {
                    continue;
                }

                String slot = p[1];
                String arg = p[2];

                double mi;
                try {
                    mi = Double.parseDouble(p[3]);
                } catch (NumberFormatException e) {
                    continue;
                }

                Feats f = out.get(pred);
                if (f == null) {
                    f = new Feats();
                    out.put(pred, f);
                }

                if (X.equals(slot)) {
                    f.x.put(arg, mi);
                } else if (Y.equals(slot)) {
                    f.y.put(arg, mi);
                }
            }
        } finally {
            if (br != null) br.close();
        }

        return out;
    }

    /**
     * DIRT Eq.(3): sim(p,q) = sqrt( sim_x(p,q) * sim_y(p,q) )
     */
    public static double pathSimilarity(Feats a, Feats b) {
        if (a == null || b == null) return 0.0;

        double sx = slotSimilarity(a.x, b.x);
        double sy = slotSimilarity(a.y, b.y);

        if (sx <= 0.0 || sy <= 0.0) return 0.0;
        return Math.sqrt(sx * sy);
    }

    /**
     * DIRT Eq.(2) for a slot:
     *
     * sim = sum_{w in intersection} (I_a(w) + I_b(w))
     *       ---------------------------------------
     *       sum_{w in a} I_a(w) + sum_{w in b} I_b(w)
     *
     * Practical tweak: ignore MI <= 0 in BOTH numerator and denominator.
     */
    private static double slotSimilarity(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;

        double sumA = 0.0;
        for (Double va : a.values()) {
            if (va != null && va > 0.0) sumA += va;
        }

        double sumB = 0.0;
        for (Double vb : b.values()) {
            if (vb != null && vb > 0.0) sumB += vb;
        }

        double denom = sumA + sumB;
        if (denom <= 0.0) return 0.0;

        double numer = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double vbObj = b.get(e.getKey());
            if (vbObj == null) continue;

            double va = (e.getValue() == null ? 0.0 : e.getValue());
            double vb = vbObj.doubleValue();

            if (va > 0.0 && vb > 0.0) {
                numer += (va + vb);
            }
        }

        if (numer <= 0.0) return 0.0;
        return numer / denom;
    }

    /**
     * Normalizes predicate key from mi.tsv.
     * (Important to avoid lookup misses due to double spaces, etc.)
     */
    public static String normalizePredKey(String pred) {
        if (pred == null) return "";
        pred = pred.trim();
        if (pred.isEmpty()) return "";
        // collapse whitespace to single spaces
        pred = pred.replaceAll("\\s+", " ");
        return pred;
    }
}
