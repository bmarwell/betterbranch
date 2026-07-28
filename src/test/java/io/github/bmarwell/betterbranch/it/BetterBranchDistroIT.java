package io.github.bmarwell.betterbranch.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void flatLayoutLauncherPrintsExpectedBranches(IntegrationTestWorkspaceExtension.Workspace workspace)
            throws IOException, InterruptedException {
        CommandResult result = runDistroLauncher(workspace.flatLayoutLauncher(), workspace.repositoryDirectory());
        assertExpectedOutput(result);
    }

    @Test
    void nestedLayoutLauncherPrintsExpectedBranches(IntegrationTestWorkspaceExtension.Workspace workspace)
            throws IOException, InterruptedException {
        CommandResult result = runDistroLauncher(workspace.nestedLayoutLauncher(), workspace.repositoryDirectory());
        assertExpectedOutput(result);
    }

    private static CommandResult runDistroLauncher(Path launcher, Path repositoryDirectory)
            throws IOException, InterruptedException {
        List<String> command = isWindows() ? List.of("cmd", "/c", launcher.toString()) : List.of(launcher.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repositoryDirectory.toFile());
        processBuilder
                .environment()
                .put(
                        "BETTERBRANCH_TEST_JAVA",
                        Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java")
                                .toString());
        processBuilder
                .environment()
                .put(
                        "BETTERBRANCH_TEST_CLASSPATH",
                        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        processBuilder.environment().put("BETTERBRANCH_TEST_MODULE_PATH", System.getProperty("jdk.module.path", ""));

        Process process = processBuilder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (InputStream inputStream = process.getInputStream();
                InputStream errorStream = process.getErrorStream()) {
            inputStream.transferTo(output);
            errorStream.transferTo(output);
        }

        return new CommandResult(process.waitFor(), output.toString(StandardCharsets.UTF_8));
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

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private record CommandResult(int exitCode, String output) {}
}
