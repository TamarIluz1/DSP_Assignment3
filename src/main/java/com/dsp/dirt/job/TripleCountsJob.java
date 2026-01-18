package com.dsp.dirt.job;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.dsp.dirt.extract.PathExtractor;
import com.dsp.dirt.extract.PathInstance;
import com.dsp.dirt.parse.BiarcsParser;
import com.dsp.dirt.parse.BiarcsRecord;
import static com.dsp.dirt.util.Keys.PS;
import static com.dsp.dirt.util.Keys.S;
import static com.dsp.dirt.util.Keys.SW;
import static com.dsp.dirt.util.Keys.T;
import static com.dsp.dirt.util.Keys.k3;
import static com.dsp.dirt.util.Keys.k4;

public class TripleCountsJob {

    /** Counter group/name for |*,*,*| over all (p,slot,w) triples. */
    private static final String COUNTER_GROUP = "DIRT";
    private static final String COUNTER_TOTAL_T = "TOTAL_T";

    public static class M extends Mapper<LongWritable, Text, Text, LongWritable> {
        private final BiarcsParser parser = new BiarcsParser();
        private final PathExtractor extractor = new PathExtractor();
        private final Text outK = new Text();
        private final LongWritable outV = new LongWritable();

        @Override
        protected void map(LongWritable key, Text value, Context ctx) throws IOException, InterruptedException {
            String line = value.toString();
            BiarcsRecord r = parser.parseLine(line);
            if (r == null || !r.isValid()) return;

            List<PathInstance> instances = extractor.extract(r);
            if (instances == null || instances.isEmpty()) return;

            for (PathInstance pi : instances) {
                long c = pi.count;
                if (c <= 0) continue;

                // (p,slot,w)
                outK.set(k4(T, pi.pathKey, pi.slot, pi.filler));
                outV.set(c);
                ctx.write(outK, outV);

                // (p,slot,*)
                outK.set(k3(PS, pi.pathKey, pi.slot));
                outV.set(c);
                ctx.write(outK, outV);

                // (*,slot,w)
                outK.set(k3(SW, pi.slot, pi.filler));
                outV.set(c);
                ctx.write(outK, outV);

                // (*,slot,*)
                outK.set(k3(S, pi.slot, "*"));
                outV.set(c);
                ctx.write(outK, outV);
            }
        }
    }

    public static class SumR extends Reducer<Text, LongWritable, Text, LongWritable> {
        @Override
        protected void reduce(Text key, Iterable<LongWritable> vals, Context ctx)
                throws IOException, InterruptedException {
            long sum = 0L;
            for (LongWritable v : vals) sum += v.get();
            ctx.write(key, new LongWritable(sum));

            // Increment TOTAL_T only for reduced (final) T keys
            if (key.toString().startsWith(T + "\t")) {
                ctx.getCounter(COUNTER_GROUP, COUNTER_TOTAL_T).increment(sum);
            }
        }
    }

    public static Job build(Configuration conf, Path in, Path out) throws IOException {
        Job job = Job.getInstance(conf, "A_triple_counts");
        job.setJarByClass(TripleCountsJob.class);

        job.setMapperClass(M.class);
        job.setCombinerClass(SumR.class);
        job.setReducerClass(SumR.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        TextInputFormat.addInputPath(job, in);
        TextOutputFormat.setOutputPath(job, out);
        return job;
    }
}
