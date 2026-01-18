package com.dsp.dirt.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import static com.dsp.dirt.util.Keys.PS;
import static com.dsp.dirt.util.Keys.S;
import static com.dsp.dirt.util.Keys.SW;
import static com.dsp.dirt.util.Keys.T;

public class ComputeMIJob {

    /** Must be set by Driver before launching this job. */
    public static final String CONF_TOTAL_T = "DIRT_TOTAL_T";

    /**
     * Input lines are: KEY \t COUNT
     * where KEY is one of:
     *  T\tp\tslot\tw
     *  PS\tp\tslot
     *  SW\tslot\tw
     *  S\tslot\t*
     */
    public static class M extends Mapper<LongWritable, Text, Text, Text> {
        private final Text outK = new Text();
        private final Text outV = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context ctx) throws IOException, InterruptedException {
            String line = value.toString();
            int lastTab = line.lastIndexOf('\t');
            if (lastTab < 0) return;

            String kPart = line.substring(0, lastTab);
            String cPart = line.substring(lastTab + 1);

            String[] fields = kPart.split("\\t");
            if (fields.length < 2) return;

            String type = fields[0];

            // Partition by slot so reducer sees all totals for that slot.
            if (type.equals(S) && fields.length == 3) {
                String slot = fields[1];
                outK.set(slot);
                outV.set("S\t" + cPart);
                ctx.write(outK, outV);

            } else if (type.equals(SW) && fields.length == 3) {
                String slot = fields[1];
                String w = fields[2];
                outK.set(slot);
                outV.set("SW\t" + w + "\t" + cPart);
                ctx.write(outK, outV);

            } else if (type.equals(PS) && fields.length == 3) {
                String p = fields[1];
                String slot = fields[2];
                outK.set(slot);
                outV.set("PS\t" + p + "\t" + cPart);
                ctx.write(outK, outV);

            } else if (type.equals(T) && fields.length == 4) {
                String p = fields[1];
                String slot = fields[2];
                String w = fields[3];
                outK.set(slot);
                outV.set("T\t" + p + "\t" + w + "\t" + cPart);
                ctx.write(outK, outV);
            }
        }
    }

    public static class R extends Reducer<Text, Text, Text, DoubleWritable> {

        private final Text outK = new Text();
        private final DoubleWritable outV = new DoubleWritable();

        @Override
        protected void reduce(Text slotKey, Iterable<Text> vals, Context ctx)
                throws IOException, InterruptedException {

            long totalT = ctx.getConfiguration().getLong(CONF_TOTAL_T, -1L);
            if (totalT <= 0) return; // Driver didn't set it → MI is invalid.

            long c_s = -1L; // (*,slot,*) sanity check only

            Map<String, Long> c_sw = new HashMap<>(); // w -> (*,slot,w)
            Map<String, Long> c_ps = new HashMap<>(); // p -> (p,slot,*)
            List<String[]> triples = new ArrayList<>(); // [p, w, c_psw]

            for (Text tv : vals) {
                String[] f = tv.toString().split("\\t");
                if (f.length == 0) continue;

                switch (f[0]) {
                    case "S":
                        if (f.length >= 2) c_s = parseLongSafe(f[1]);
                        break;

                    case "SW":
                        if (f.length >= 3) c_sw.put(f[1], parseLongSafe(f[2]));
                        break;

                    case "PS":
                        if (f.length >= 3) c_ps.put(f[1], parseLongSafe(f[2]));
                        break;

                    case "T":
                        if (f.length >= 4) triples.add(new String[]{f[1], f[2], f[3]});
                        break;
                }
            }

            if (c_s <= 0) return; // no slot totals → skip

            for (String[] t : triples) {
                String p = t[0];
                String w = t[1];
                long c_psw = parseLongSafe(t[2]); // |p,slot,w|

                Long c_ps_val = c_ps.get(p);      // |p,slot,*|
                Long c_sw_val = c_sw.get(w);      // |*,slot,w|

                if (c_ps_val == null || c_sw_val == null) continue;
                if (c_psw <= 0 || c_ps_val <= 0 || c_sw_val <= 0) continue;

                // DIRT Eq.(1):
                // MI(p,slot,w) = log( |p,slot,w| * |*,*,*| / ( |p,slot,*| * |*,slot,w| ) )
                double num = (double) c_psw * (double) totalT;
                double den = (double) c_ps_val * (double) c_sw_val;
                if (den <= 0.0) continue;

                double mi = Math.log(num / den);

                // Practical DIRT: keep only non-negative MI (reduces noise; matches later “positive MI only” usage)
                if (mi <= 0.0) continue;

                outK.set(p + "\t" + slotKey.toString() + "\t" + w);
                outV.set(mi);
                ctx.write(outK, outV);
            }
        }

        private long parseLongSafe(String s) {
            try { return Long.parseLong(s.trim()); }
            catch (Exception e) { return -1L; }
        }
    }

    public static Job build(Configuration conf, Path in, Path out) throws IOException {
        Job job = Job.getInstance(conf, "B_compute_mi");
        job.setJarByClass(ComputeMIJob.class);

        job.setMapperClass(M.class);
        job.setReducerClass(R.class);
        job.setNumReduceTasks(2);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        TextInputFormat.addInputPath(job, in);
        TextOutputFormat.setOutputPath(job, out);

        return job;
    }
}
