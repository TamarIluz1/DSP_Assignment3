package com.dsp.dirt.submit;

import java.util.Arrays;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;

public class DirtEmrSubmitter {

    private static final String BUCKET = "dsp-idan-tamar-3";
    private static final String JAR_S3 = "s3://" + BUCKET + "/jars/dirt-assignment3-1.0.0.jar";
    private static final String LOG_URI = "s3://" + BUCKET + "/emr-logs/";

    // --- Inputs (20 files) ---
    private static final String INPUT_FIRST10 = "s3://dsp-ass3-first10-biarcs/";
    private static final String INPUT_RANDOM10 = "s3://" + BUCKET + "/ass3/input_random10/"; // must exist!

    // --- Output base for the 20 run ---
    private static final String OUTPUT_20 = "s3://" + BUCKET + "/ass3/output_20/";

    // --- Test set locations in *your* bucket (recommended to upload once) ---
    private static final String POS_PREDS = "s3://" + BUCKET + "/ass3/test/positive-preds.txt";
    private static final String NEG_PREDS = "s3://" + BUCKET + "/ass3/test/negative-preds.txt";

    public static void main(String[] args) throws Exception {
        // Usage: DirtEmrSubmitter [region] [keypair] [instanceType] [instanceCount]
        String region = (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) ? args[0] : "us-east-1";
        String keypair = (args.length >= 2) ? args[1] : "";
        String instanceType = (args.length >= 3 && args[2] != null && !args[2].trim().isEmpty()) ? args[2] : "m4.large";
        int instanceCount = (args.length >= 4) ? Integer.parseInt(args[3]) : 3;

        AWSCredentialsProvider credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();

        AmazonElasticMapReduce emr = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.fromName(region))
                .build();

        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(instanceCount)
                .withMasterInstanceType(instanceType)
                .withSlaveInstanceType(instanceType)
                .withKeepJobFlowAliveWhenNoSteps(false);

        if (keypair != null && !keypair.trim().isEmpty()) {
            instances = instances.withEc2KeyName(keypair.trim());
        }

        // -------------------------
        // Step 1: Driver (A_counts + B_mi)
        // Driver args: <input1> <input2> ... <outBase>
        // -------------------------
        HadoopJarStepConfig step1 = new HadoopJarStepConfig()
                .withJar(JAR_S3)
                .withMainClass("com.dsp.dirt.Driver")
                .withArgs(
                        INPUT_FIRST10,
                        INPUT_RANDOM10,
                        OUTPUT_20
                );

        StepConfig s1 = new StepConfig()
                .withName("DIRT-20files-Driver")
                .withHadoopJarStep(step1)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        // -------------------------
        // Step 2: EvalPairs
        // EvalPairs args: <B_mi_dir> <pos> <neg> <out_scores>
        // -------------------------
        String MI_DIR = OUTPUT_20 + "B_mi/";
        String OUT_SCORES = OUTPUT_20 + "out_scores.tsv";

        HadoopJarStepConfig step2 = new HadoopJarStepConfig()
                .withJar(JAR_S3)
                .withMainClass("com.dsp.dirt.local.EvalPairs")
                .withArgs(
                        MI_DIR,
                        POS_PREDS,
                        NEG_PREDS,
                        OUT_SCORES
                );

        StepConfig s2 = new StepConfig()
                .withName("DIRT-20files-EvalPairs")
                .withHadoopJarStep(step2)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        RunJobFlowRequest req = new RunJobFlowRequest()
                .withName("ass3-dirt-medium-20files")
                .withReleaseLabel("emr-6.15.0")
                .withInstances(instances)
                .withSteps(Arrays.asList(s1, s2))
                .withLogUri(LOG_URI)
                .withServiceRole("EMR_DefaultRole")
                .withJobFlowRole("EMR_EC2_DefaultRole");

        RunJobFlowResult res = emr.runJobFlow(req);
        System.out.println("Submitted EMR JobFlowId: " + res.getJobFlowId());
        System.out.println("Output base: " + OUTPUT_20);
        System.out.println("MI dir: " + MI_DIR);
        System.out.println("Scores: " + OUT_SCORES);
        System.out.println("Cluster will auto-terminate after steps finish.");
    }
}
