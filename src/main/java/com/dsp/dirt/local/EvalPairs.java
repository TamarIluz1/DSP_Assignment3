package com.dsp.dirt.local;

import java.io.*;
import java.util.*;

public class EvalPairs {

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

        Set<String> neededPaths = new HashSet<>();
        for (String[] p : posPairs) { neededPaths.add(p[0]); neededPaths.add(p[1]); }
        for (String[] p : negPairs) { neededPaths.add(p[0]); neededPaths.add(p[1]); }

        var featsByPath = SimilarityScorer.loadMI(mi, neededPaths);

        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("label\tp1\tp2\tscore");

            for (String[] p : posPairs) {
                double s = SimilarityScorer.pathSimilarity(featsByPath.get(p[0]), featsByPath.get(p[1]));
                pw.println("pos\t" + p[0] + "\t" + p[1] + "\t" + s);
            }
            for (String[] p : negPairs) {
                double s = SimilarityScorer.pathSimilarity(featsByPath.get(p[0]), featsByPath.get(p[1]));
                pw.println("neg\t" + p[0] + "\t" + p[1] + "\t" + s);
            }
        }

        System.out.println("Wrote: " + out.getAbsolutePath());
    }

    private static List<String[]> readPairs(File f) throws IOException {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\t");
                if (parts.length >= 2) out.add(new String[]{parts[0], parts[1]});
            }
        }
        return out;
    }
}
