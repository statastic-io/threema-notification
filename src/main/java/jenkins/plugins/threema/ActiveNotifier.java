package jenkins.plugins.threema;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;


@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private final ThreemaNotifier notifier;
    private final BuildListener listener;

    public ActiveNotifier(ThreemaNotifier notifier, BuildListener listener) {
        super();
        this.notifier = notifier;
        this.listener = listener;
    }

    private ThreemaService getThreema(AbstractBuild r) {
        return notifier.newThreemaService(r, listener);
    }

    public void deleted(AbstractBuild r) {
    }

    public void started(AbstractBuild build) {
        getThreema(build).publish(build);
    }

    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {
        AbstractProject<?, ?> project = r.getProject();
        Result result = r.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        if (previousBuild != null) {
            do {
                previousBuild = previousBuild.getPreviousCompletedBuild();
            } while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
        }
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
        if ((result == Result.ABORTED && notifier.getNotifyAborted())
                || (result == Result.FAILURE // notify only on
                // single failed
                // build
                && previousResult != Result.FAILURE
                && notifier.getNotifyFailure())
                || (result == Result.FAILURE // notify only on repeated failures
                && previousResult == Result.FAILURE
                && notifier.getNotifyRepeatedFailure())
                || (result == Result.NOT_BUILT && notifier.getNotifyNotBuilt())
                || (result == Result.SUCCESS
                && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                && notifier.getNotifyBackToNormal())
                || (result == Result.SUCCESS && notifier.getNotifySuccess())
                || (result == Result.UNSTABLE && notifier.getNotifyUnstable())) {
            getThreema(r).publish(r);
        }
    }
}
