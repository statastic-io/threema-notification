package jenkins.plugins.threema;

import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageBuilder {

    private static final Logger logger = Logger.getLogger(MessageBuilder.class.getName());

    private static final String STARTING_STATUS_MESSAGE = "\uD83D\uDE4F Running",
            BACK_TO_NORMAL_STATUS_MESSAGE = "\uD83D\uDC4D Back to normal",
            STILL_FAILING_STATUS_MESSAGE = "\uD83D\uDED1 Still Failing",
            SUCCESS_STATUS_MESSAGE = "\uD83D\uDC4D Success",
            FAILURE_STATUS_MESSAGE = "\uD83D\uDED1 Failure",
            ABORTED_STATUS_MESSAGE = "\u26a0 Aborted",
            NOT_BUILT_STATUS_MESSAGE = "\u26a0️ Not built",
            UNSTABLE_STATUS_MESSAGE = "\u26a0 Unstable",
            UNKNOWN_STATUS_MESSAGE = "\u2753 Unknown";

    private final StringBuffer message;
    private final Run<?, ?> build;

    public MessageBuilder(Run<?, ?> build) {
        this.message = new StringBuffer();
        this.build = build;
        startMessage();
    }

    public MessageBuilder appendStatusMessage() {
        message.append(this.escape(getStatusMessage(build)));
        return this;
    }

    static String getStatusMessage(Run<?,?> r) {
        Result result = r.getResult();
        if (result == null || !result.isCompleteBuild()) {
            return STARTING_STATUS_MESSAGE;
        }

        Result previousResult;
        Run<?,?> lastBuild = r.getParent().getLastBuild();
        Run<?,?> previousBuild = (lastBuild != null) ? lastBuild.getPreviousBuild() : null;
        Run<?,?> previousSuccessfulBuild = r.getPreviousSuccessfulBuild();
        boolean buildHasSucceededBefore = previousSuccessfulBuild != null;

        /*
         * If the last build was aborted, go back to find the last non-aborted build.
         * This is so that aborted builds do not affect build transitions. I.e. if build
         * 1 was failure, build 2 was aborted and build 3 was a success the transition
         * should be failure -> success (and therefore back to normal) not aborted ->
         * success.
         */
        Run<?,?> lastNonAbortedBuild = previousBuild;
        while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
            lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
        }

        /*
         * If all previous builds have been aborted, then use SUCCESS as a default
         * status so an aborted message is sent
         */
        if (lastNonAbortedBuild == null) {
            previousResult = Result.SUCCESS;
        } else {
            previousResult = lastNonAbortedBuild.getResult();
        }

        /*
         * Back to normal should only be shown if the build has actually succeeded at
         * some point. Also, if a build was previously unstable and has now succeeded
         * the status should be "Back to normal"
         */
        if (result == Result.SUCCESS
                && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                && buildHasSucceededBefore) {
            return BACK_TO_NORMAL_STATUS_MESSAGE;
        }
        if (result == Result.FAILURE && previousResult == Result.FAILURE) {
            return STILL_FAILING_STATUS_MESSAGE;
        }
        if (result == Result.SUCCESS) {
            return SUCCESS_STATUS_MESSAGE;
        }
        if (result == Result.FAILURE) {
            return FAILURE_STATUS_MESSAGE;
        }
        if (result == Result.ABORTED) {
            return ABORTED_STATUS_MESSAGE;
        }
        if (result == Result.NOT_BUILT) {
            return NOT_BUILT_STATUS_MESSAGE;
        }
        if (result == Result.UNSTABLE) {
            return UNSTABLE_STATUS_MESSAGE;
        }
        return UNKNOWN_STATUS_MESSAGE;
    }

    public MessageBuilder append(String string) {
        message.append(this.escape(string));
        return this;
    }

    public MessageBuilder append(Object string) {
        message.append(this.escape(string.toString()));
        return this;
    }

    private MessageBuilder startMessage() {
        if (Jenkins.get().getRootUrl() != null) {
            try {
                URL url = new URL(Jenkins.get().getRootUrl());
                message.append(url.getHost()).append(" ");
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Root URL is not valid!", e);
            }
        }

        message.append(this.escapeDisplayName(build.getParent().getFullDisplayName()));
        message.append(" - ");
        message.append(this.escapeDisplayName(build.getDisplayName()));
        message.append(" ");
        return this;
    }

    public MessageBuilder appendDuration() {
        message.append(" after ");
        String durationString;
        if (message.toString().contains(BACK_TO_NORMAL_STATUS_MESSAGE)) {
            durationString = createBackToNormalDurationString();
        } else {
            durationString = build.getDurationString();
        }
        message.append(durationString);
        return this;
    }

    private String createBackToNormalDurationString() {
        Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
        if (previousSuccessfulBuild == null) {
            return "unknown";
        }
//      long previousSuccessStartTime = previousSuccessfulBuild.getStartTimeInMillis();
//      long previousSuccessDuration = previousSuccessfulBuild.getDuration();
//      long previousSuccessEndTime = previousSuccessStartTime + previousSuccessDuration;
//      long buildStartTime = build.getStartTimeInMillis();
//      long buildDuration = build.getDuration();
//      long buildEndTime = buildStartTime + buildDuration;
//      long backToNormalDuration = buildEndTime - previousSuccessEndTime;
        //TODO CHANGED
        long currentBuildStartTime = build.getTimeInMillis();
        long lastSuccessBuildStartTime = previousSuccessfulBuild.getTimeInMillis();
        long diff = currentBuildStartTime - lastSuccessBuildStartTime;
        return Util.getTimeSpanString(diff);
    }

    public String escape(String string) {
        string = string.replace("&", "&amp;");
        string = string.replace("<", "&lt;");
        string = string.replace(">", "&gt;");

        return string;
    }

    public String escapeDisplayName(String displayName) {
        // escape HTML
        displayName = escape(displayName);

        // escape mattermost markdown which _may_ occur in job display names
        displayName = displayName.replace("~", "\\~");
        displayName = displayName.replace("*", "\\*");
        displayName = displayName.replace("_", "\\_");
        displayName = displayName.replace("`", "\\`");

        return displayName;
    }

    public String toString() {
        return message.toString();
    }
}
