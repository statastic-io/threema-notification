package jenkins.plugins.threema;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import static hudson.Util.fixNull;

public class ThreemaNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(ThreemaNotifier.class.getName());

    private String recipient;
    private String credentialsId;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getRecipient() {
        return recipient;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public boolean getStartNotification() {
        return startNotification;
    }

    public boolean getNotifySuccess() {
        return notifySuccess;
    }

    public boolean getNotifyAborted() {
        return notifyAborted;
    }

    public boolean getNotifyFailure() {
        return notifyFailure;
    }

    public boolean getNotifyNotBuilt() {
        return notifyNotBuilt;
    }

    public boolean getNotifyUnstable() {
        return notifyUnstable;
    }

    public boolean getNotifyBackToNormal() {
        return notifyBackToNormal;
    }

    public boolean getNotifyRepeatedFailure() {
        return notifyRepeatedFailure;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setIcon(@CheckForNull String recipient) {
        this.recipient = fixNull(recipient);
    }

    @DataBoundSetter
    public void setStartNotification(boolean startNotification) {
        this.startNotification = startNotification;
    }

    @DataBoundSetter
    public void setNotifySuccess(boolean notifySuccess) {
        this.notifySuccess = notifySuccess;
    }


    @DataBoundSetter
    public void setNotifyAborted(boolean notifyAborted) {
        this.notifyAborted = notifyAborted;
    }

    @DataBoundSetter
    public void setNotifyFailure(boolean notifyFailure) {
        this.notifyFailure = notifyFailure;
    }

    @DataBoundSetter
    public void setNotifyNotBuilt(boolean notifyNotBuilt) {
        this.notifyNotBuilt = notifyNotBuilt;
    }

    @DataBoundSetter
    public void setNotifyUnstable(boolean notifyUnstable) {
        this.notifyUnstable = notifyUnstable;
    }

    @DataBoundSetter
    public void setNotifyBackToNormal(boolean notifyBackToNormal) {
        this.notifyBackToNormal = notifyBackToNormal;
    }

    @DataBoundSetter
    public void setNotifyRepeatedFailure(boolean notifyRepeatedFailure) {
        this.notifyRepeatedFailure = notifyRepeatedFailure;
    }

    @DataBoundConstructor
    public ThreemaNotifier(
            final String recipient,
            final String credentialsId,
            final boolean startNotification,
            final boolean notifyAborted,
            final boolean notifyFailure,
            final boolean notifyNotBuilt,
            final boolean notifySuccess,
            final boolean notifyUnstable,
            final boolean notifyBackToNormal,
            final boolean notifyRepeatedFailure) {
        super();
        this.recipient = recipient;
        this.credentialsId = credentialsId;
        this.startNotification = startNotification;
        this.notifyAborted = notifyAborted;
        this.notifyFailure = notifyFailure;
        this.notifyNotBuilt = notifyNotBuilt;
        this.notifySuccess = notifySuccess;
        this.notifyUnstable = notifyUnstable;
        this.notifyBackToNormal = notifyBackToNormal;
        this.notifyRepeatedFailure = notifyRepeatedFailure;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public ThreemaService newThreemaService(AbstractBuild r, BuildListener listener) {
//        EnvVars env;
//        try {
//            env = r.getEnvironment(listener);
//        } catch (Exception e) {
//            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
//            env = new EnvVars();
//        }
//        recipient = env.expand(recipient);
//        from = env.expand(from);

        return new StandardThreemaService(credentialsId, recipient);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (startNotification) {
            Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
            for (Publisher publisher : map.values()) {
                if (publisher instanceof ThreemaNotifier) {
                    logger.info("Invoking Started...");
                    new ActiveNotifier((ThreemaNotifier) publisher, listener).started(build);
                }
            }
        }
        return super.prebuild(build, listener);
    }

    @Extension
    @Symbol("threemaNotifier")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String credentialsId;
        private String recipient;

        public DescriptorImpl() {
            load();
        }


        @Exported
        public String getCredentialsId() {
            return credentialsId;
        }

        @DataBoundSetter
        public void setCredentialsId(String credentialsId) {
            this.credentialsId = credentialsId;
        }


        @DataBoundSetter
        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public String getRecipient() {
            return recipient;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return true;
        }

        ThreemaService getThreemaService(
                final String credentialsId, final String recipient) {
            return new StandardThreemaService(credentialsId, recipient);
        }

        @Override
        public String getDisplayName() {
            return "Threema Notifications";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item,
                                                     @QueryParameter String credentialsId) {

            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId); // (2)
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId); // (2)
                }
            }

            return result
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM,
                            item,
                            StandardUsernamePasswordCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.allOf(
                                    CredentialsMatchers.always()
                            ))
                    .includeCurrentValue(credentialsId);
        }
    }

    @Deprecated
    public static class ThreemaJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {

        private String secret;
        private String from;
        private String recipient;
        private String credentialsId;
        private boolean startNotification;
        private boolean notifySuccess;
        private boolean notifyAborted;
        private boolean notifyNotBuilt;
        private boolean notifyUnstable;
        private boolean notifyFailure;
        private boolean notifyBackToNormal;
        private boolean notifyRepeatedFailure;


        @DataBoundConstructor
        public ThreemaJobProperty(
                String secret,
                String from,
                String recipient,
                String credentialsId,
                boolean startNotification,
                boolean notifyAborted,
                boolean notifyFailure,
                boolean notifyNotBuilt,
                boolean notifySuccess,
                boolean notifyUnstable,
                boolean notifyBackToNormal,
                boolean notifyRepeatedFailure) {
            this.secret = secret;
            this.from = from;
            this.recipient = recipient;
            this.credentialsId = credentialsId;
            this.startNotification = startNotification;
            this.notifyAborted = notifyAborted;
            this.notifyFailure = notifyFailure;
            this.notifyNotBuilt = notifyNotBuilt;
            this.notifySuccess = notifySuccess;
            this.notifyUnstable = notifyUnstable;
            this.notifyBackToNormal = notifyBackToNormal;
            this.notifyRepeatedFailure = notifyRepeatedFailure;
        }

        @Exported
        public String getSecret() {
            return secret;
        }

        @Exported
        public String getFrom() {
            return from;
        }

        @Exported
        public String getRecipient() {
            return recipient;
        }

        @Exported
        public String getCredentialsId() {
            return credentialsId;
        }

        @Exported
        public boolean getStartNotification() {
            return startNotification;
        }

        @Exported
        public boolean getNotifySuccess() {
            return notifySuccess;
        }

        @Override
        public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            return super.prebuild(build, listener);
        }

        @Exported
        public boolean getNotifyAborted() {
            return notifyAborted;
        }

        @Exported
        public boolean getNotifyFailure() {
            return notifyFailure;
        }

        @Exported
        public boolean getNotifyNotBuilt() {
            return notifyNotBuilt;
        }

        @Exported
        public boolean getNotifyUnstable() {
            return notifyUnstable;
        }

        @Exported
        public boolean getNotifyBackToNormal() {
            return notifyBackToNormal;
        }

        @Exported
        public boolean getNotifyRepeatedFailure() {
            return notifyRepeatedFailure;
        }

//        @Extension
//        public static final class Migrator extends ItemListener {
//
//            @SuppressWarnings("deprecation")
//            @Override
//            public void onLoaded() {
//                logger.info("Starting Settings Migration Process");
//                for (AbstractProject<?, ?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
//                    final ThreemaJobProperty threemaJobProperty =
//                            p.getProperty(ThreemaJobProperty.class);
//
//                    if (threemaJobProperty == null) {
//                        logger.fine(
//                                String.format(
//                                        "Configuration is already up to date for \"%s\", skipping migration",
//                                        p.getName()));
//                        continue;
//                    }
//
//                    ThreemaNotifier threemaNotifier = p.getPublishersList().get(ThreemaNotifier.class);
//
//                    if (threemaNotifier == null) {
//                        logger.fine(
//                                String.format(
//                                        "Configuration does not have a notifier for \"%s\", not migrating settings",
//                                        p.getName()));
//                    } else {
//                        logger.info(String.format("Starting migration for \"%s\"", p.getName()));
//                        // map settings
//                        if (StringUtils.isBlank(threemaNotifier.getSecret().getPlainText())) {
//                            threemaNotifier.setSecret(threemaJobProperty.getSecret());
//                        }
//                        if (StringUtils.isBlank(threemaNotifier.from)) {
//                            threemaNotifier.from = threemaJobProperty.getFrom();
//                        }
//                        if (StringUtils.isBlank(threemaNotifier.recipient)) {
//                            threemaNotifier.recipient = threemaJobProperty.getRecipient();
//                        }
//
//                        threemaNotifier.startNotification = threemaJobProperty.getStartNotification();
//
//                        threemaNotifier.notifyAborted = threemaJobProperty.getNotifyAborted();
//                        threemaNotifier.notifyFailure = threemaJobProperty.getNotifyFailure();
//                        threemaNotifier.notifyNotBuilt = threemaJobProperty.getNotifyNotBuilt();
//                        threemaNotifier.notifySuccess = threemaJobProperty.getNotifySuccess();
//                        threemaNotifier.notifyUnstable = threemaJobProperty.getNotifyUnstable();
//                        threemaNotifier.notifyBackToNormal = threemaJobProperty.getNotifyBackToNormal();
//                        threemaNotifier.notifyRepeatedFailure =
//                                threemaJobProperty.getNotifyRepeatedFailure();
//                    }
//
//                    try {
//                        // property section is not used anymore - remove
//                        p.removeProperty(ThreemaJobProperty.class);
//                        p.save();
//                        logger.info("Configuration updated successfully");
//                    } catch (IOException e) {
//                        logger.log(Level.SEVERE, e.getMessage(), e);
//                    }
//                }
//            }
//        }
    }
}
