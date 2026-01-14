package com.bgu.dirt.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import com.bgu.dirt.util.SimilarityCalculator;

public class Job3_ComputeSimilarity {
    
    public static class Mapper extends org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, Text> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws java.io.IOException, InterruptedException {
            // Input: feature vectors for all paths
            // Build in-memory feature vectors and compute pairwise similarities
            
            // TODO: Load all feature vectors into memory (or use secondary sort)
            // and compute pairwise similarities
        }
    }
    
    public static class Reducer extends org.apache.hadoop.mapreduce.Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws java.io.IOException, InterruptedException {
            // Output path pairs with similarity > threshold
        }
    }
    
    public static Job createJob(Configuration conf, String inputPath, String outputPath) throws java.io.IOException {
        Job job = Job.getInstance(conf, "DIRT-Job3-ComputeSimilarity");
        job.setJarByClass(Job3_ComputeSimilarity.class);
        
        job.setMapperClass(Mapper.class);
        job.setReducerClass(Reducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job;
    }
}
