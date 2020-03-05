package com.avioconsulting.jenkins.mule.workflow;

import com.avioconsulting.jenkins.mule.impl.MuleUtil;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class GetMuleFileNameStep extends Step {
    private final String appName;
    private final String appVersion;
    private final String muleVersion;
    private boolean baseNameOnly;

    @DataBoundConstructor
    public GetMuleFileNameStep(String appName,
                               String appVersion,
                               String muleVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.muleVersion = muleVersion;
        this.baseNameOnly = false;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getMuleVersion() {
        return muleVersion;
    }

    public boolean isBaseNameOnly() {
        return baseNameOnly;
    }

    @DataBoundSetter
    public void setBaseNameOnly(boolean baseNameOnly) {
        this.baseNameOnly = baseNameOnly;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context,
                             appName,
                             appVersion,
                             muleVersion,
                             baseNameOnly);
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution {
        private final String appName;
        private final String appVersion;
        private final String muleVersion;
        private final boolean baseNameOnly;

        protected Execution(@Nonnull StepContext context,
                            String appName,
                            String appVersion,
                            String muleVersion,
                            boolean baseNameOnly) {
            super(context);
            this.appName = appName;
            this.appVersion = appVersion;
            this.muleVersion = muleVersion;
            this.baseNameOnly = baseNameOnly;
        }

        @Override
        protected String run() throws Exception {
            String prefix = this.baseNameOnly ? "" : "target/";
            return MuleUtil.getFileName(String.format("%s%s", prefix, appName),
                                        appVersion,
                                        muleVersion);
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            HashSet<Class<?>> set = new HashSet<>();
            return set;
        }

        @Override
        public String getFunctionName() {
            return "getMuleFileName";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Returns a formatted Mule filename including the target dir";
        }
    }
}
