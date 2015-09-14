package com.pagerduty.go.notification.base;


import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class GoNotificationMessage {
    private Logger LOGGER = Logger.getLoggerFor(GoNotificationMessage.class);

    static class Pipeline {
        @SerializedName("name")
        private String name;

        @SerializedName("counter")
        private String counter;

        @SerializedName("stage")
        private Stage stage;
    }

    static class Stage {
        @SerializedName("name")
        private String name;

        @SerializedName("counter")
        private String counter;

        @SerializedName("state")
        private String state;

        @SerializedName("result")
        private String result;

        @SerializedName("create-time")
        private String createTime;

        @SerializedName("last-transition-time")
        private String lastTransitionTime;

        @SerializedName("jobs")
        private List<Job> jobs;
    }

    static class Job {
        @SerializedName("name")
        private String name;

        @SerializedName("schedule-time")
        private String scheduleTime;

        @SerializedName("complete-time")
        private String completeTime;

        @SerializedName("state")
        private String state;

        @SerializedName("result")
        private String result;

        @SerializedName("agent-uuid")
        private String agentUuid;
    }

    @SerializedName("pipeline")
    private Pipeline pipeline;

    public String fullyQualifiedJobName() {
        return pipeline.name + "/" + pipeline.counter + "/" + pipeline.stage.name + "/" + pipeline.stage.counter;
    }


    public String getPipelineName() {
        return pipeline.name;
    }

    public String getPipelineCounter() {
        return pipeline.counter;
    }

    public String getStageName() {
        return pipeline.stage.name;
    }

    public String getStageCounter() {
        return pipeline.stage.counter;
    }

    public String getStageState() {
        return pipeline.stage.state;
    }

    public String getStageResult() {
        return pipeline.stage.result;
    }

    public String getCreateTime() {
        return pipeline.stage.createTime;
    }

    public String getLastTransitionTime() {
        return pipeline.stage.lastTransitionTime;
    }

    public List<String> getJobNames() {
        List<String> jobNames = new ArrayList<String>();
        for (Job job : pipeline.stage.jobs) {
            jobNames.add(job.name);
        }
        return jobNames;
    }

    public String getJobScheduleTime(String jobName) {
        Job job = getJob(jobName);
        return job.scheduleTime;
    }

    public String getJobCompleteTime(String jobName) {
        Job job = getJob(jobName);
        return job.completeTime;
    }

    public String getJobState(String jobName) {
        Job job = getJob(jobName);
        return job.state;
    }

    public String getJobResult(String jobName) {
        Job job = getJob(jobName);
        return job.result;
    }

    private Job getJob(String jobName) {
        for (Job job : pipeline.stage.jobs) {
            if (jobName.equals(job.name)) {
                return job;
            }
        }
        return null;
    }


}
