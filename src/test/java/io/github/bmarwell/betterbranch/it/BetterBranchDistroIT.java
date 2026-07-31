package io.github.bmarwell.betterbranch.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bmarwell.betterbranch.it.extension.BetterBranchRunner;
import io.github.bmarwell.betterbranch.it.extension.BetterBranchRunner.CommandResult;
import io.github.bmarwell.betterbranch.it.extension.GitWorkspace;
import io.github.bmarwell.betterbranch.it.extension.IntegrationTestWorkspaceExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(IntegrationTestWorkspaceExtension.class)
class BetterBranchDistroIT {

    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m");

    @Test
    void flatLayoutLauncherPrintsExpectedBranches(GitWorkspace workspace, BetterBranchRunner runner)
            throws IOException, InterruptedException {
        CommandResult result = runner.runDistroLauncher();
        assertExpectedOutput(result);
    }

    @Test
    void nestedLayoutLauncherPrintsExpectedBranches(GitWorkspace workspace, BetterBranchRunner runner)
            throws IOException, InterruptedException {
        CommandResult result = runner.runDistroLauncher();
        assertExpectedOutput(result);
    }


    private static void assertExpectedOutput(CommandResult commandResult) {
        String output = stripAnsi(commandResult.output());

        assertEquals(0, commandResult.exitCode(), () -> "Unexpected process output:\n" + output);
        assertContains(output, "(?m)^Ahead\\s+Behind\\s+Branch\\s+Last Commit\\s*$");
        assertContains(output, "(?m)^0\\s+0\\s+main\\s+.+ago\\s*$");
        assertContains(output, "(?m)^1\\s+1\\s+feature-one\\s+.+ago\\s*$");
        assertContains(output, "(?m)^1\\s+0\\s+feature-two\\s+.+ago\\s*$");
    }


    private static void assertContains(String output, String regex) {
        assertTrue(
            Pattern.compile(regex).matcher(output).find(),
            () -> "Output did not match regex: " + regex + "\n" + output);
    }


    private static String stripAnsi(String output) {
        return ANSI_ESCAPE_PATTERN.matcher(output).replaceAll("");
    }

}
