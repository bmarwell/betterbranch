package io.github.bmarwell.betterbranch.it.extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class BetterBranchRunner {

    private final GitWorkspace workspace;
    private final Path launcher;
    private final Path repositoryDirectory;

    public BetterBranchRunner(GitWorkspace workspace, Path launcher, Path repositoryDirectory) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.repositoryDirectory = repositoryDirectory;
    }

    public CommandResult runDistroLauncher()
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
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (InputStream inputStream = process.getInputStream()) {
            inputStream.transferTo(output);
        }

        return new CommandResult(process.waitFor(), output.toString(StandardCharsets.UTF_8));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public record CommandResult(int exitCode, String output) {}
}
