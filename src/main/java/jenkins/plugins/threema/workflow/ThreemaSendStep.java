package jenkins.plugins.threema.workflow;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.threema.StandardThreemaService;
import jenkins.plugins.threema.ThreemaService;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Set;

/**
 * Workflow step to send a Thrreema recipient notification.
 */
public class ThreemaSendStep extends Step {

    private String credentialsId;

    private String recipient;

    private String message;

    private boolean failOnError;

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundConstructor
    public ThreemaSendStep(String credentialsId, String recipient, boolean failOnError) {
        this.credentialsId = credentialsId;
        this.recipient = recipient;
        this.failOnError = failOnError;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    public String getRecipient() {
        return recipient;
    }

    @DataBoundSetter
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String getMessage() {
        return message;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new ThreemaSendStepExecution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {
            super();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "threemaSend";
        }

        @Override
        public String getDisplayName() {
            return "Send Threema notification";
        }
    }

    public static class ThreemaSendStepExecution
            extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        transient ThreemaSendStep step;

        transient TaskListener listener;

        @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")

        protected ThreemaSendStepExecution(StepContext context, ThreemaSendStep threemaSendStep) {
            super(context);
            step = threemaSendStep;
            try {
                this.listener = this.getContext().get(TaskListener.class);
            } catch (IOException | InterruptedException e) {
                //TODO WARN REFACTOR
            }
        }

        @Override
        protected Void run() throws Exception {
            ThreemaService threemaService = getThreemaService(step.credentialsId, step.recipient);

            boolean publishSuccess = threemaService.publish(this.getContext().get(Run.class), step.message);

            if (!publishSuccess && step.failOnError) {
                throw new AbortException("Threema notification failed. See Jenkins logs for details.");
            } else if (!publishSuccess) {
                listener.error("Threema notification failed. See Jenkins logs for details.");
            }
            return null;
        }

        // streamline unit testing
        ThreemaService getThreemaService(String credentialsId, String recipient) {
            return new StandardThreemaService(credentialsId, recipient);
        }
    }
}
