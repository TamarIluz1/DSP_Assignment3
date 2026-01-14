package com.bgu.dirt.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import com.bgu.dirt.model.Triple;
import com.bgu.dirt.parser.*;

public class Job1_ExtractPaths {
    
    public static class Mapper extends org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, LongWritable> {
        private DependencyTreeExtractor extractor;
        private final Text outKey = new Text();
        private final LongWritable outVal = new LongWritable(1);
        
        @Override
        protected void setup(Context context) {
            extractor = new DependencyTreeExtractor();
        }
        
        @Override
        protected void map(LongWritable key, Text value, Context context) throws java.io.IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) return;
            
            // Parse biarc line
            BiarcParser.ParsedBiarc biarc = BiarcParser.parse(line);
            if (biarc == null) return;
            
            // Extract paths
            java.util.Listm.bgu.dirt.model.Path> paths = extractor.extractPaths(biarc);
            
            // For each path, emit triples (path, SlotX, word_x) and (path, SlotY, word_y)
            for (com.bgu.dirt.model.Path path : paths) {
                // Emit SlotX triple
                String tripleKeyX = path.getPathStr() + "\tSlotX\t" + value.toString(); // placeholder
                outKey.set(tripleKeyX);
                context.write(outKey, outVal);
                
                // Emit SlotY triple
                String tripleKeyY = path.getPathStr() + "\tSlotY\t" + value.toString(); // placeholder
                outKey.set(tripleKeyY);
                context.write(outKey, outVal);
            }
        }
    }
    
    public static class Reducer extends org.apache.hadoop.mapreduce.Reducer<Text, LongWritable, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws java.io.IOException, InterruptedException {
            // Aggregate counts
            long count = 0;
            for (LongWritable val : values) {
                count += val.get();
            }
            
            // Output: triple_key \t count
            Text outVal = new Text(String.valueOf(count));
            context.write(key, outVal);
        }
    }
    
    public static Job createJob(Configuration conf, String inputPath, String outputPath) throws java.io.IOException {
        Job job = Job.getInstance(conf, "DIRT-Job1-ExtractPaths");
        job.setJarByClass(Job1_ExtractPaths.class);
        
        job.setMapperClass(Mapper.class);
        job.setReducerClass(Reducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job;
    }
}
