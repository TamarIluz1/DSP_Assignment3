package com.dsp.dirt.local;

import com.dsp.dirt.extract.Stemmer;

import java.io.*;
import java.util.*;

public class EvalPairs {

    private static final Stemmer STEM = new Stemmer();

    private static String normalizePred(String pred) {
        if (pred == null) return "";
        pred = pred.trim();
        if (pred.isEmpty()) return "";

        String[] toks = pred.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i].trim();
            if (t.isEmpty()) continue;

            if (!"X".equals(t) && !"Y".equals(t)) {
                t = STEM.stem(t.toLowerCase());
            }

            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString().trim();
    }

    private static double safeSimilarity(Map<String, SimilarityScorer.Feats> featsByPath, String p1, String p2) {
        SimilarityScorer.Feats f1 = featsByPath.get(p1);
        SimilarityScorer.Feats f2 = featsByPath.get(p2);
        if (f1 == null || f2 == null) return 0.0;
        return SimilarityScorer.pathSimilarity(f1, f2);
    }

    /**
     * args:
     * 0: mi.tsv (local merged file; download from S3 output B_mi)
     * 1: positive-preds.txt
     * 2: negative-preds.txt
     * 3: out_scores.tsv
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: EvalPairs <mi.tsv> <pos.txt> <neg.txt> <out.tsv>");
            System.exit(1);
        }

        File mi = new File(args[0]);
        File pos = new File(args[1]);
        File neg = new File(args[2]);
        File out = new File(args[3]);

        List<String[]> posPairs = readPairs(pos);
        List<String[]> negPairs = readPairs(neg);

        Set<String> needed = new HashSet<String>();
        for (String[] p : posPairs) { needed.add(p[0]); needed.add(p[1]); }
        for (String[] p : negPairs) { needed.add(p[0]); needed.add(p[1]); }

        // IMPORTANT: needed set is normalized predicates, so MI loader must also normalize.
        Map<String, SimilarityScorer.Feats> featsByPath = SimilarityScorer.loadMI(mi, needed);

        System.out.println("Needed predicates: " + needed.size());
        System.out.println("Loaded predicates: " + featsByPath.size());

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(out));
            pw.println("label\tp1\tp2\tscore");

            for (String[] p : posPairs) {
                double s = safeSimilarity(featsByPath, p[0], p[1]);
                pw.println("pos\t" + p[0] + "\t" + p[1] + "\t" + s);
            }
            for (String[] p : negPairs) {
                double s = safeSimilarity(featsByPath, p[0], p[1]);
                pw.println("neg\t" + p[0] + "\t" + p[1] + "\t" + s);
            }
        } finally {
            if (pw != null) pw.close();
        }

        System.out.println("Wrote: " + out.getAbsolutePath());
    }

    private static List<String[]> readPairs(File f) throws IOException {
        List<String[]> out = new ArrayList<String[]>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\t");
                if (parts.length >= 2) {
                    String p1 = normalizePred(parts[0]);
                    String p2 = normalizePred(parts[1]);
                    if (!p1.isEmpty() && !p2.isEmpty()) {
                        out.add(new String[]{p1, p2});
                    }
                }
            }
        } finally {
            if (br != null) br.close();
        }
        return out;
    }
}
