package com.dsp.dirt;

import com.dsp.dirt.job.ComputeMIJob;
import com.dsp.dirt.job.TripleCountsJob;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

public class Driver {

    /**
     * args:
     *   0: input path (corpus)
     *   1: output base path
     *
     * Produces:
     *   {outBase}/A_counts/
     *   {outBase}/B_mi/
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Driver <input> <outBase>");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        Path in = new Path(args[0]);
        Path outBase = new Path(args[1]);

        Path outA = new Path(outBase, "A_counts");
        Path outB = new Path(outBase, "B_mi");

        Job j1 = TripleCountsJob.build(conf, in, outA);
        if (!j1.waitForCompletion(true)) System.exit(2);

        Job j2 = ComputeMIJob.build(conf, outA, outB);
        if (!j2.waitForCompletion(true)) System.exit(3);

        System.out.println("DONE. MI output at: " + outB);
    }
}
