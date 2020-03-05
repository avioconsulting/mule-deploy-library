package com.avioconsulting.jenkins.mule.workflow;

import com.avioconsulting.jenkins.mule.impl.CloudHubDeployer;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class DeployCloudHubStep extends Step {
    private static final String DEFAULT_WORKER_TYPE = BaselineCloudHubDescriptor.DEFAULT_WORKER_TYPE;
    private static final String DEFAULT_MULE_VERSION = BaselineCloudHubDescriptor.DEFAULT_MULE_VERSION;
    private static final boolean DEFAULT_PERSISTENT_QUEUES = BaselineCloudHubDescriptor.DEFAULT_PERSISTENT_QUEUES;
    private static final int DEFAULT_WORKER_COUNT = BaselineCloudHubDescriptor.DEFAULT_WORKER_COUNT;
    private static final String DEFAULT_OTHER_PROPS = BaselineCloudHubDescriptor.DEFAULT_OTHER_PROPS;

    private final String zipFilePath;
    private final String cryptoKey;
    private final String appName;
    private String muleVersion;
    private final String awsRegion;
    private String version;
    private boolean usePersistentQueues;
    private String workerType;
    private int workerCount;
    private String otherCloudHubPropertiesJson;
    private String otherAppPropertyOverrides;
    private String overrideByChangingFileInZip;

    public String getOverrideByChangingFileInZip() {
        return overrideByChangingFileInZip;
    }

    @DataBoundSetter
    public void setOverrideByChangingFileInZip(String overrideByChangingFileInZip) {
        this.overrideByChangingFileInZip = overrideByChangingFileInZip;
    }

    public String getOtherAppPropertyOverrides() {
        return otherAppPropertyOverrides;
    }

    @DataBoundSetter
    public void setOtherAppPropertyOverrides(String otherAppPropertyOverrides) {
        this.otherAppPropertyOverrides = otherAppPropertyOverrides;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getOtherCloudHubPropertiesJson() {
        return otherCloudHubPropertiesJson;
    }

    @DataBoundSetter
    public void setOtherCloudHubPropertiesJson(String otherCloudHubPropertiesJson) {
        this.otherCloudHubPropertiesJson = otherCloudHubPropertiesJson;
    }

    @Nonnull
    public String getZipFilePath() {
        return zipFilePath;
    }

    @Nonnull
    public String getCryptoKey() {
        return cryptoKey;
    }

    @Nonnull
    public String getAppName() {
        return appName;
    }

    @Nonnull
    public String getMuleVersion() {
        return muleVersion;
    }

    @DataBoundSetter
    public void setMuleVersion(String muleVersion) {
        this.muleVersion = muleVersion;
    }

    @Nonnull
    public String getAwsRegion() {
        return awsRegion;
    }

    @Nonnull
    public boolean isUsePersistentQueues() {
        return usePersistentQueues;
    }

    @DataBoundSetter
    public void setUsePersistentQueues(boolean usePersistentQueues) {
        this.usePersistentQueues = usePersistentQueues;
    }

    @Nonnull
    public String getWorkerType() {
        return workerType;
    }

    @DataBoundSetter
    public void setWorkerType(String workerType) {
        this.workerType = workerType;
    }

    @Nonnull
    public int getWorkerCount() {
        return workerCount;
    }

    @DataBoundSetter
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    @DataBoundConstructor
    public DeployCloudHubStep(String zipFilePath,
                              String cryptoKey,
                              String appName,
                              String awsRegion) {
        this.zipFilePath = zipFilePath;
        this.cryptoKey = cryptoKey;
        this.appName = appName;
        this.awsRegion = awsRegion;
        this.workerType = DEFAULT_WORKER_TYPE;
        this.usePersistentQueues = DEFAULT_PERSISTENT_QUEUES;
        this.workerCount = DEFAULT_WORKER_COUNT;
        this.muleVersion = DEFAULT_MULE_VERSION;
        this.otherCloudHubPropertiesJson = DEFAULT_OTHER_PROPS;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(context,
                             zipFilePath,
                             cryptoKey,
                             appName,
                             muleVersion,
                             awsRegion,
                             usePersistentQueues,
                             workerType,
                             workerCount,
                             otherCloudHubPropertiesJson,
                             version,
                             otherAppPropertyOverrides,
                             overrideByChangingFileInZip);
    }

    public static final class Execution extends SynchronousNonBlockingStepExecution {
        private final String zipFilePath;
        private final String cryptoKey;
        private final String appName;
        private final String muleVersion;
        private final String awsRegion;
        private final boolean usePersistentQueues;
        private final String workerType;
        private final int workerCount;
        private final String otherCloudHubPropertiesJson;
        private final String version;
        private final String otherAppPropertyOverrides;
        private final String overrideByChangingFileInZip;

        public Execution(@Nonnull StepContext context,
                         String zipFilePath,
                         String cryptoKey,
                         String appName,
                         String muleVersion,
                         String awsRegion,
                         boolean usePersistentQueues,
                         String workerType,
                         int workerCount,
                         String otherCloudHubPropertiesJson,
                         String version,
                         String otherAppPropertyOverrides,
                         String overrideByChangingFileInZip) {
            super(context);
            this.zipFilePath = zipFilePath;
            this.cryptoKey = cryptoKey;
            this.appName = appName;
            this.muleVersion = muleVersion;
            this.awsRegion = awsRegion;
            this.usePersistentQueues = usePersistentQueues;
            this.workerType = workerType;
            this.workerCount = workerCount;
            this.otherCloudHubPropertiesJson = otherCloudHubPropertiesJson;
            this.version = version;
            this.otherAppPropertyOverrides = otherAppPropertyOverrides;
            this.overrideByChangingFileInZip = overrideByChangingFileInZip;
        }

        @Override
        protected Void run() throws Exception {
            StepContext context = getContext();
            TaskListener listener = context.get(TaskListener.class);
            EnvVars environment = context.get(EnvVars.class);
            PrintStream logger = listener.getLogger();
            CommonConfig commonConfig = CommonStepCode.getCommonConfig(environment);
            CloudHubDeployer impl = new CloudHubDeployer(commonConfig.getOrgId(),
                                                         commonConfig.getUsername(),
                                                         commonConfig.getPassword(),
                                                         logger);
            FilePath zipFile = CommonStepCode.getZipFilePath(context,
                                                             zipFilePath);
            String anypointClientId = environment.get("anypoint_autodiscclientidsecret_usr");
            if (anypointClientId == null) {
                throw new Exception("Configure your Anypoint Client ID in the anypoint_autodiscclientidsecret_usr environment variable in your Jenkinsfile!");
            }
            String anypointClientSecret = environment.get("anypoint_autodiscclientidsecret_psw");
            if (anypointClientSecret == null) {
                throw new Exception("Configure your Anypoint Client Secret in the anypoint_autodiscclientidsecret_psw environment variable in your Jenkinsfile!");
            }
            String cloudHubAppPrefix = environment.get("cloudhub_app_prefix");
            if (cloudHubAppPrefix == null) {
                throw new Exception("Configure your CloudHub app prefix in the cloudhub_app_prefix environment variable in your Jenkinsfile!");
            }
            try {
                impl.deploy(commonConfig.getEnvironment(),
                            appName,
                            cloudHubAppPrefix,
                            zipFile.read(),
                            zipFile.getName(),
                            this.cryptoKey,
                            this.muleVersion,
                            this.awsRegion,
                            this.usePersistentQueues,
                            this.workerType,
                            this.workerCount,
                            this.otherCloudHubPropertiesJson,
                            anypointClientId,
                            anypointClientSecret,
                            this.otherAppPropertyOverrides,
                            this.overrideByChangingFileInZip);
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
    public static class DescriptorImpl extends BaselineCloudHubDescriptor {
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
            return "muleDeployCloudHub";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Will deploy a CloudHub application using Runtime Manager APIs";
        }
    }
}
