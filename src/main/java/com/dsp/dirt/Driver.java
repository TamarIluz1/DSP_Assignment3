package com.dsp.dirt;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import com.dsp.dirt.job.ComputeMIJob;
import com.dsp.dirt.job.TripleCountsJob;

public class Driver {

    // Must match TripleCountsJob counter group/name
    private static final String COUNTER_GROUP = "DIRT";
    private static final String COUNTER_TOTAL_T = "TOTAL_T";

    // Config key that ComputeMIJob will read
    private static final String CONF_TOTAL_T = "DIRT_TOTAL_T";

    public static void main(String[] args) throws Exception {
        args = stripLeadingMainClassArg(args);

        if (args.length < 2) {
            System.err.println("Usage: Driver <input> <outBase>");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        Path in = new Path(args[0]);
        Path outBase = new Path(args[1]);

        Path outA = new Path(outBase, "A_counts");
        Path outB = new Path(outBase, "B_mi");

        // Optional but recommended: delete existing outputs to avoid “File already exists”.
        deleteIfExists(conf, outA);
        deleteIfExists(conf, outB);

        // --- Job A: counts ---
        Job j1 = TripleCountsJob.build(conf, in, outA);
        if (!j1.waitForCompletion(true)) System.exit(2);

        // Read |*,*,*| total over all (p,slot,w) triples from the counter
        long totalT = j1.getCounters()
                       .findCounter(COUNTER_GROUP, COUNTER_TOTAL_T)
                       .getValue();

        if (totalT <= 0) {
            System.err.println("ERROR: TOTAL_T counter is <= 0. Cannot compute MI correctly.");
            System.exit(4);
        }

        // IMPORTANT: inject this into the Configuration used by Job B
        conf.setLong(CONF_TOTAL_T, totalT);

        System.out.println("TOTAL_T (|*,*,*|) = " + totalT);

        // --- Job B: MI ---
        Job j2 = ComputeMIJob.build(conf, outA, outB);
        if (!j2.waitForCompletion(true)) System.exit(3);

        System.out.println("DONE. MI output at: " + outB);
    }

    private static String[] stripLeadingMainClassArg(String[] args) {
        if (args == null || args.length == 0) return new String[0];
        if (args[0] != null && args[0].contains("com.dsp.dirt.Driver")) {
            String[] out = new String[Math.max(0, args.length - 1)];
            System.arraycopy(args, 1, out, 0, out.length);
            return out;
        }
        return args;
    }

    private static void deleteIfExists(Configuration conf, Path p) throws Exception {
        FileSystem fs = p.getFileSystem(conf);
        if (fs.exists(p)) fs.delete(p, true);
    }
}
