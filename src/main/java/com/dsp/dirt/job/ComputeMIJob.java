package com.dsp.dirt.job;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
            if (totalT <= 0) return;

            long c_s = -1L; // (*,slot,*)

            // Marginals kept in memory
            Map<String, Long> c_sw = new HashMap<>(); // w -> (*,slot,w)
            Map<String, Long> c_ps = new HashMap<>(); // p -> (p,slot,*)

            // Spill T records to local disk instead of RAM
            File tmp = File.createTempFile("dirt_triples_" + slotKey.toString() + "_", ".tsv");
            // If node crashes it might remain, but it's in /tmp and will be reclaimed
            BufferedWriter bw = null;

            try {
                bw = new BufferedWriter(new FileWriter(tmp), 1 << 20); // 1MB buffer

                for (Text tv : vals) {
                    String s = tv.toString();
                    String[] f = s.split("\\t");
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
                            // f: ["T", p, w, c_psw]
                            if (f.length >= 4) {
                                // write: p \t w \t c_psw
                                bw.write(f[1]);
                                bw.write('\t');
                                bw.write(f[2]);
                                bw.write('\t');
                                bw.write(f[3]);
                                bw.newLine();
                            }
                            break;
                    }
                }
            } finally {
                if (bw != null) bw.close();
            }

            if (c_s <= 0) {
                tmp.delete();
                return;
            }

            // Second pass: stream triples file and compute MI without holding triples in heap
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(tmp), 1 << 20);

                String line;
                while ((line = br.readLine()) != null) {
                    // line: p \t w \t c_psw
                    int t1 = line.indexOf('\t');
                    if (t1 < 0) continue;
                    int t2 = line.indexOf('\t', t1 + 1);
                    if (t2 < 0) continue;

                    String p = line.substring(0, t1);
                    String w = line.substring(t1 + 1, t2);
                    long c_psw = parseLongSafe(line.substring(t2 + 1));
                    if (c_psw <= 0) continue;

                    Long c_ps_val = c_ps.get(p);
                    Long c_sw_val = c_sw.get(w);
                    if (c_ps_val == null || c_sw_val == null) continue;
                    if (c_ps_val <= 0 || c_sw_val <= 0) continue;

                    double num = (double) c_psw * (double) totalT;
                    double den = (double) c_ps_val * (double) c_sw_val;
                    if (den <= 0.0) continue;

                    double mi = Math.log(num / den);
                    if (mi <= 0.0) continue;

                    outK.set(p + "\t" + slotKey.toString() + "\t" + w);
                    outV.set(mi);
                    ctx.write(outK, outV);
                }

            } finally {
                if (br != null) br.close();
                tmp.delete();
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
        job.setNumReduceTasks(10);

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
