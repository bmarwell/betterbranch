package io.github.bmarwell.betterbranch.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.bmarwell.betterbranch.it.extension.BetterBranchRunner;
import io.github.bmarwell.betterbranch.it.extension.BetterBranchRunner.CommandResult;
import io.github.bmarwell.betterbranch.it.extension.GitWorkspace;
import io.github.bmarwell.betterbranch.it.extension.WorktreeExtension;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorktreeExtension.class)
public class WorktreeIT {

    @Test
    void can_run_on_worktree(GitWorkspace workspace, BetterBranchRunner runner)
            throws IOException, InterruptedException {
        final CommandResult commandResult = runner.runDistroLauncher(Path.of("branch1"));

        assertEquals(
                0,
                commandResult.exitCode(),
                "Expected exit code 0 but was: " + commandResult.exitCode() + "; out:\n" + commandResult.output()
                        + "\n");
    }
}
