package com.avioconsulting.jenkins.mule.workflow;

import hudson.model.AutoCompletionCandidates;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.QueryParameter;

public abstract class BaselineCloudHubDescriptor extends StepDescriptor {
    public static final String DEFAULT_WORKER_TYPE = "Micro";
    public static final String DEFAULT_MULE_VERSION = "4.1.5";
    public static final boolean DEFAULT_PERSISTENT_QUEUES = false;
    public static final int DEFAULT_WORKER_COUNT = 1;
    public static final String DEFAULT_OTHER_PROPS = "{}";

    public String getDefaultWorkerType() {
        return DEFAULT_WORKER_TYPE;
    }

    public boolean getPersistentQueueEnabled() {
        return DEFAULT_PERSISTENT_QUEUES;
    }

    public int getDefaultWorkerCount() {
        return DEFAULT_WORKER_COUNT;
    }

    public String getDefaultMuleVersion() {
        return DEFAULT_MULE_VERSION;
    }

    public String getDefaultOtherPropertiesJson() {
        return DEFAULT_OTHER_PROPS;
    }

    // combobox doesn't seem to work right with default values in the Pipeline UI
    public AutoCompletionCandidates doAutoCompleteMuleVersion(@QueryParameter String valueFromUser) {
        return getSuggestions(valueFromUser,
                              VERSION_SUGGESTIONS);
    }

    private static AutoCompletionCandidates getSuggestions(String valueFromUser,
                                                           String[] suggestions) {
        AutoCompletionCandidates items = new AutoCompletionCandidates();
        for (String possibility : suggestions) {
            if (valueFromUser == null || possibility.startsWith(valueFromUser.trim())) {
                items.add(possibility);
            }
        }
        return items;
    }

    private static final String[] VERSION_SUGGESTIONS = new String[]{
            "3.9.1",
            "3.9.2",
            DEFAULT_MULE_VERSION,
            "4.1.5"
    };

    public AutoCompletionCandidates doAutoCompleteAwsRegion(@QueryParameter String valueFromUser) {
        return getSuggestions(valueFromUser,
                              AWS_REGION_SUGGESTIONS);
    }

    private static final String[] AWS_REGION_SUGGESTIONS = new String[]{
            "us-east-1",
            "us-east-2",
            "us-west-1",
            "us-west-2"
    };

    public AutoCompletionCandidates doAutoCompleteWorkerType(@QueryParameter String valueFromUser) {
        return getSuggestions(valueFromUser,
                              WORKER_TYPE_SUGGESTIONS);
    }

    private static final String[] WORKER_TYPE_SUGGESTIONS = new String[]{
            DEFAULT_WORKER_TYPE,
            "Small",
            "Medium",
            "Large",
            "xLarge"
    };
}
