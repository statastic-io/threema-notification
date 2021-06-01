package jenkins.plugins.threema;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;

public interface ThreemaService {
  boolean publish(@NonNull Run<?, ?> run);
  boolean publish(@NonNull Run<?, ?> run, String message);
}
