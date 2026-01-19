package com.dsp.dirt.local;

import com.dsp.dirt.extract.Stemmer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EvalPairs {

    private static final Stemmer STEM = new Stemmer();

    private static class PairRow {
        String label; // pos/neg
        String p1;
        String p2;
        PairRow(String label, String p1, String p2) {
            this.label = label;
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    private static String normalizePredForTest(String pred) {
        if (pred == null) return "";
        pred = pred.trim();
        if (pred.isEmpty()) return "";

        String[] toks = pred.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String raw : toks) {
            String t = raw.trim();
            if (t.isEmpty()) continue;

            if (!"X".equals(t) && !"Y".equals(t)) {
                t = STEM.stem(t.toLowerCase());
            }

            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    public static void main(String[] args) throws Exception {
        // args: <miDirOrFile> <test_pairs.tsv> <out_scores.tsv>
        if (args.length < 3) {
            System.err.println("Usage: EvalPairs <miDirOrFile> <test_pairs.tsv> <out_scores.tsv>");
            System.exit(1);
        }
        System.err.println("args[0] (miDirOrFile): " + args[1]);
        System.err.println("args[1] (test_pairs.tsv): " + args[2]);
        System.err.println("args[2] (out_scores.tsv): " + args[3]);


        Configuration conf = new Configuration();
        Path miPath = new Path(args[1]);
        Path pairsPath = new Path(args[2]);
        Path outPath = new Path(args[3]);

        FileSystem fsPairs = pairsPath.getFileSystem(conf);
        FileSystem fsMi    = miPath.getFileSystem(conf);
        FileSystem fsOut   = outPath.getFileSystem(conf);

        // 1) Read test pairs
        List<PairRow> pairs = readTestPairs(fsPairs, pairsPath);
        if (pairs.isEmpty()) {
            System.err.println("No pairs found in: " + pairsPath);
            System.exit(2);
        }

        Set<String> needed = new HashSet<String>();
        for (PairRow pr : pairs) {
            needed.add(pr.p1);
            needed.add(pr.p2);
        }

        // 2) Load MI feats only for needed predicates (from miPath dir OR file)
        Map<String, SimilarityScorer.Feats> feats = loadMIFromFS(fsMi, miPath, needed);

        System.out.println("Pairs: " + pairs.size());
        System.out.println("Needed predicates: " + needed.size());
        System.out.println("Loaded predicates: " + feats.size());

        // 3) Write out_scores.tsv to S3
        if (fsOut.exists(outPath)) fsOut.delete(outPath, true);


        FSDataOutputStream os = fsOut.create(outPath, true);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        pw.println("label\tp1\tp2\tscore");
        for (PairRow pr : pairs) {
            SimilarityScorer.Feats f1 = feats.get(pr.p1);
            SimilarityScorer.Feats f2 = feats.get(pr.p2);
            double s = (f1 == null || f2 == null) ? 0.0 : SimilarityScorer.pathSimilarity(f1, f2);
            pw.println(pr.label + "\t" + pr.p1 + "\t" + pr.p2 + "\t" + s);
        }
        pw.flush();
        pw.close();

        System.out.println("Wrote: " + outPath);
    }

    private static List<PairRow> readTestPairs(FileSystem fs, Path pairsPath) throws IOException {
        List<PairRow> out = new ArrayList<PairRow>();

        FSDataInputStream is = fs.open(pairsPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        boolean first = true;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\t");
            if (first) {
                first = false;
                // allow header
                if (parts.length >= 3 && parts[0].equalsIgnoreCase("label")) {
                    continue;
                }
            }

            if (parts.length < 3) continue;

            String label = parts[0].trim().toLowerCase();
            if (!label.equals("pos") && !label.equals("neg")) continue;

            String p1 = normalizePredForTest(parts[1]);
            String p2 = normalizePredForTest(parts[2]);
            if (p1.isEmpty() || p2.isEmpty()) continue;

            out.add(new PairRow(label, p1, p2));
        }

        br.close();
        return out;
    }

    private static Map<String, SimilarityScorer.Feats> loadMIFromFS(FileSystem fs, Path miPath, Set<String> needed)
            throws IOException {

        Map<String, SimilarityScorer.Feats> out = new HashMap<String, SimilarityScorer.Feats>();

        List<Path> files = new ArrayList<Path>();
        FileStatus st = fs.getFileStatus(miPath);

        if (st.isDirectory()) {
            RemoteIterator<FileStatus> it = fs.listStatusIterator(miPath);
            while (it.hasNext()) {
                FileStatus f = it.next();
                if (!f.isFile()) continue;
                String name = f.getPath().getName();
                if (name.startsWith("part-") || name.endsWith(".tsv") || name.endsWith(".txt")) {
                    files.add(f.getPath());
                }
            }
        } else {
            files.add(miPath);
        }

        for (Path p : files) {
            FSDataInputStream is = fs.open(p);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] a = line.split("\\t");
                if (a.length < 4) continue;

                String pred = SimilarityScorer.normalizePredKey(a[0]);
                if (pred.isEmpty()) continue;

                // IMPORTANT: needed predicates are normalized+stemmed for test;
                // extracted predicates are already stemmed by your extraction.
                if (needed != null && !needed.isEmpty() && !needed.contains(pred)) {
                    continue;
                }

                String slot = a[1];
                String arg = a[2];

                double mi;
                try {
                    mi = Double.parseDouble(a[3]);
                } catch (Exception e) {
                    continue;
                }

                SimilarityScorer.Feats f = out.get(pred);
                if (f == null) {
                    f = new SimilarityScorer.Feats();
                    out.put(pred, f);
                }

                if ("X".equals(slot)) f.x.put(arg, mi);
                else if ("Y".equals(slot)) f.y.put(arg, mi);
            }
            br.close();
        }

        return out;
    }
}
