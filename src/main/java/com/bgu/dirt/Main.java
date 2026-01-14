package com.bgu.dirt;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import com.bgu.dirt.jobs.*;
import java.io.*;

public class Main implements Tool {
    private Configuration conf;
    
    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }
    
    @Override
    public Configuration getConf() {
        return conf;
    }
    
    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: Main <input-dir> <output-dir> <small|large>");
            return 1;
        }
        
        String inputDir = args[0];
        String outputDir = args[1];
        String mode = args[2]; // "small" or "large"
        
        // Job 1: Extract paths and create triple database
        System.out.println("===== Running Job 1: Extract Paths =====");
        String job1Output = outputDir + "/job1-triples";
        org.apache.hadoop.mapreduce.Job job1 = Job1_ExtractPaths.createJob(conf, inputDir, job1Output);
        if (!job1.waitForCompletion(true)) {
            return 1;
        }
        
        // Job 2: Compute Mutual Information
        System.out.println("===== Running Job 2: Compute MI =====");
        String job2Output = outputDir + "/job2-mi";
        org.apache.hadoop.mapreduce.Job job2 = Job2_ComputeMI.createJob(conf, job1Output, job2Output);
        if (!job2.waitForCompletion(true)) {
            return 1;
        }
        
        // Job 3: Compute path-pair similarities
        System.out.println("===== Running Job 3: Compute Similarities =====");
        String job3Output = outputDir + "/job3-similarities";
        org.apache.hadoop.mapreduce.Job job3 = Job3_ComputeSimilarity.createJob(conf, job2Output, job3Output);
        if (!job3.waitForCompletion(true)) {
            return 1;
        }
        
        // Run evaluation
        System.out.println("===== Running Evaluation =====");
        runEvaluation(conf, job3Output, outputDir, mode);
        
        return 0;
    }
    
    private void runEvaluation(Configuration conf, String similarityFile, String outputDir, String mode) throws Exception {
        // TODO: Load positive and negative predicates
        // TODO: Map predicates to paths
        // TODO: Compute F1-score and precision-recall curve
        System.out.println("Evaluation complete. Results in: " + outputDir);
    }
    
    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new Main(), args);
        System.exit(exitCode);
    }
}
