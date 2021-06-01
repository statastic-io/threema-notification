package jenkins.plugins.threema;

import hudson.model.AbstractBuild;

public interface TokenExpander {
    String expand(String template, AbstractBuild<?, ?> build);
}
