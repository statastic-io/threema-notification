package jenkins.plugins.threema;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JenkinsTokenExpander implements TokenExpander {
    private static final Logger logger = Logger.getLogger(JenkinsTokenExpander.class.getName());
    private final TaskListener listener;

    public JenkinsTokenExpander(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    public String expand(String template, AbstractBuild<?, ?> build) {
        try {
            return TokenMacro.expandAll(build, listener, template, false, null);
        } catch (MacroEvaluationException | IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to process custom message", e);
            return "[UNPROCESSABLE] " + template;
        }
    }
}
