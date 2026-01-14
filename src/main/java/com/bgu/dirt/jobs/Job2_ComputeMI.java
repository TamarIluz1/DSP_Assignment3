package com.bgu.dirt.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import com.bgu.dirt.util.MICalculator;

public class Job2_ComputeMI {
    
    public static class Mapper extends org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws java.io.IOException, InterruptedException {
            // Input: triple_key \t count
            String[] parts = value.toString().split("\t", 2);
            if (parts.length < 2) return;
            
            String tripleKey = parts[0];
            long count = Long.parseLong(parts[1]);
            
            // Emit: path \t slot:word:count
            String[] tripleFields = tripleKey.split("\t");
            if (tripleFields.length >= 3) {
                String path = tripleFields[0];
                String slotWord = tripleFields[1] + ":" + tripleFields[2];
                
                Text outKey = new Text(path);
                Text outVal = new Text(slotWord + ":" + count);
                context.write(outKey, outVal);
            }
        }
    }
    
    public static class Reducer extends org.apache.hadoop.mapreduce.Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws java.io.IOException, InterruptedException {
            // Aggregate all (slot, word, count) for a single path
            // Compute MI for each
            
            // TODO: Implement MI calculation using MICalculator
            // Output: path \t slot:word:mi
            
            for (Text val : values) {
                context.write(key, val);
            }
        }
    }
    
    public static Job createJob(Configuration conf, String inputPath, String outputPath) throws java.io.IOException {
        Job job = Job.getInstance(conf, "DIRT-Job2-ComputeMI");
        job.setJarByClass(Job2_ComputeMI.class);
        
        job.setMapperClass(Mapper.class);
        job.setReducerClass(Reducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job;
    }
}
