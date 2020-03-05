package com.avioconsulting.jenkins.mule.workflow;

import com.avioconsulting.jenkins.mule.impl.OnPremDeployer;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class DeployOnPremStep extends Step {
    private final String zipFilePath;
    private final String appName;
    private final String targetServerOrClusterName;
    private String version;
    private String propertyOverrides;
    private String overrideByChangingFileInZip;

    public String getOverrideByChangingFileInZip() {
        return overrideByChangingFileInZip;
    }

    @DataBoundSetter
    public void setOverrideByChangingFileInZip(String overrideByChangingFileInZip) {
        this.overrideByChangingFileInZip = overrideByChangingFileInZip;
    }

    public String getPropertyOverrides() {
        return propertyOverrides;
    }

    @DataBoundSetter
    public void setPropertyOverrides(String propertyOverrides) {
        this.propertyOverrides = propertyOverrides;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    @Nonnull
    public String getZipFilePath() {
        return zipFilePath;
    }

    @Nonnull
    public String getAppName() {
        return appName;
    }

    @Nonnull
    public String getTargetServerOrClusterName() {
        return targetServerOrClusterName;
    }

    @DataBoundConstructor
    public DeployOnPremStep(String zipFilePath,
                            String appName,
                            String targetServerOrClusterName) {
        this.zipFilePath = zipFilePath;
        this.appName = appName;
        this.targetServerOrClusterName = targetServerOrClusterName;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(context,
                             zipFilePath,
                             appName,
                             targetServerOrClusterName,
                             version,
                             propertyOverrides,
                             overrideByChangingFileInZip);
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution {
        private final String zipFilePath;
        private final String appName;
        private final String targetServerOrClusterName;
        private final String version;
        private final String propertyOverrides;
        private final String overrideByChangingFileInZip;

        public Execution(@Nonnull StepContext context,
                         String zipFilePath,
                         String appName,
                         String targetServerOrClusterName,
                         String version,
                         String propertyOverrides,
                         String overrideByChangingFileInZip) {
            super(context);
            this.zipFilePath = zipFilePath;
            this.appName = appName;
            this.targetServerOrClusterName = targetServerOrClusterName;
            this.version = version;
            this.propertyOverrides = propertyOverrides;
            this.overrideByChangingFileInZip = overrideByChangingFileInZip;
        }

        @Override
        protected Void run() throws Exception {
            StepContext context = getContext();
            TaskListener listener = context.get(TaskListener.class);
            EnvVars environment = context.get(EnvVars.class);
            PrintStream logger = listener.getLogger();
            CommonConfig commonConfig = CommonStepCode.getCommonConfig(environment);
            OnPremDeployer impl = new OnPremDeployer(commonConfig.getOrgId(),
                                                     commonConfig.getUsername(),
                                                     commonConfig.getPassword(),
                                                     logger);
            FilePath zipFile = CommonStepCode.getZipFilePath(context,
                                                             zipFilePath);
            try {
                impl.deploy(commonConfig.getEnvironment(),
                            appName,
                            zipFile.read(),
                            zipFile.getName(),
                            targetServerOrClusterName,
                            propertyOverrides,
                            overrideByChangingFileInZip);
            } finally {
                impl.close();
            }
            WorkflowRun run = context.get(WorkflowRun.class);
            CommonStepCode.setVersion(this.version,
                                      commonConfig.getEnvironment(),
                                      environment,
                                      run);
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            HashSet<Class<?>> set = new HashSet<>();
            set.add(TaskListener.class);
            set.add(EnvVars.class);
            set.add(FilePath.class);
            set.add(WorkflowRun.class);
            return set;
        }

        @Override
        public String getFunctionName() {
            return "muleDeployOnPrem";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Will deploy an on-premise application using Runtime Manager APIs";
        }
    }
}
