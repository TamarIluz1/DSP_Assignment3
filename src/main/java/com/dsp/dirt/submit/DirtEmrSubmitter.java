package com.dsp.dirt.submit;

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

    // Fixed inputs for your "small" run
    private static final String INPUT_SMALL = "s3://dsp-ass3-first10-biarcs/";
    private static final String BUCKET = "dsp-idan-tamar-3";

    // You upload your shaded jar here
    private static final String JAR_S3 = "s3://" + BUCKET + "/jars/dirt-assignment3-1.0.0.jar";

    // Outputs/logs in your bucket
    private static final String OUTPUT_SMALL = "s3://" + BUCKET + "/ass3/output_small/";
    private static final String LOG_URI = "s3://" + BUCKET + "/emr-logs/";

    public static void main(String[] args) throws Exception {
        // Usage: DirtEmrSubmitter [region] [keypair] [instanceType] [instanceCount]
        String region = (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) ? args[0] : "us-east-1";
        String keypair = (args.length >= 2) ? args[1] : ""; // optional
        String instanceType = (args.length >= 3 && args[2] != null && !args[2].trim().isEmpty()) ? args[2] : "m4.large";
        int instanceCount = (args.length >= 4) ? Integer.parseInt(args[3]) : 3; // 1 master + 2 core

        AWSCredentialsProvider credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();

        AmazonElasticMapReduce emr = AmazonElasticMapReduceClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.fromName(region))
                .build();

        // --- Cluster config ---
        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(instanceCount)
                .withMasterInstanceType(instanceType)
                .withSlaveInstanceType(instanceType)
                .withKeepJobFlowAliveWhenNoSteps(false); // terminate after step completes

        // Java-8 replacement for isBlank()
        if (keypair != null && !keypair.trim().isEmpty()) {
            instances = instances.withEc2KeyName(keypair.trim());
        }

        // --- Step: run your Driver on EMR ---
        HadoopJarStepConfig step = new HadoopJarStepConfig()
                .withJar(JAR_S3)
                .withMainClass("com.dsp.dirt.Driver")
                .withArgs(INPUT_SMALL, OUTPUT_SMALL);

        StepConfig stepConfig = new StepConfig()
                .withName("DIRT-small-10files")
                .withHadoopJarStep(step)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        RunJobFlowRequest req = new RunJobFlowRequest()
                .withName("ass3-dirt-small-10files")
                .withReleaseLabel("emr-6.15.0")
                .withInstances(instances)
                .withSteps(stepConfig)
                .withLogUri(LOG_URI)
                .withServiceRole("EMR_DefaultRole")
                .withJobFlowRole("EMR_EC2_DefaultRole");

        RunJobFlowResult res = emr.runJobFlow(req);
        System.out.println("Submitted EMR JobFlowId: " + res.getJobFlowId());
        System.out.println("Output will be under: " + OUTPUT_SMALL);
        System.out.println("Cluster will auto-terminate after step finishes (KeepJobFlowAliveWhenNoSteps=false).");
    }
}
