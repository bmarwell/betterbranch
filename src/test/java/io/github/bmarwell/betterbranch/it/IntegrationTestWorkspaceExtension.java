package io.github.bmarwell.betterbranch.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class IntegrationTestWorkspaceExtension implements BeforeEachCallback, ParameterResolver {

    private static final String SETUP_REPO_WINDOWS_SCRIPT = "/it/setup-repo.cmd";
    private static final String SETUP_REPO_UNIX_SCRIPT = "/it/setup-repo.sh";
    private static final int WINDOWS_DELETE_RETRIES = 10;
    private static final long WINDOWS_DELETE_RETRY_DELAY_MILLIS = 100L;
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(IntegrationTestWorkspaceExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).getOrComputeIfAbsent(context.getUniqueId(), unused -> createWorkspace(), Workspace.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(Workspace.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), Workspace.class);
    }

    private static Workspace createWorkspace() {
        try {
            Path rootDirectory = Files.createTempDirectory("betterbranch-it-");
            Path repositoryDirectory = rootDirectory.resolve("repo");

            runSetupScript(rootDirectory, repositoryDirectory);

            Path flatLayoutLauncher = createLauncher(rootDirectory.resolve("flat-layout"), false);
            Path nestedLayoutLauncher = createLauncher(rootDirectory.resolve("nested-layout"), true);

            return new Workspace(rootDirectory, repositoryDirectory, flatLayoutLauncher, nestedLayoutLauncher);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while preparing integration test workspace.", e);
        }
    }

    private static void runSetupScript(Path rootDirectory, Path repositoryDirectory)
            throws IOException, InterruptedException {
        String resourceName = isWindows() ? SETUP_REPO_WINDOWS_SCRIPT : SETUP_REPO_UNIX_SCRIPT;
        Path setupScript = rootDirectory.resolve(isWindows() ? "setup-repo.cmd" : "setup-repo.sh");
        Files.writeString(setupScript, readResource(resourceName), StandardCharsets.UTF_8);
        markExecutable(setupScript);

        runCommand(commandForScript(setupScript, repositoryDirectory), rootDirectory, "setting up git repository");
    }

    private static Path createLauncher(Path rootDirectory, boolean nestedLayout) throws IOException {
        Path launcherDirectory = nestedLayout ? rootDirectory.resolve("bin") : rootDirectory;
        Files.createDirectories(launcherDirectory);

        Path launcherPath = launcherDirectory.resolve(isWindows() ? "betterbranch.bat" : "betterbranch");
        String launcherContent = isWindows()
                ? "@echo off\n\"%BETTERBRANCH_TEST_JAVA%\" --module-path \"%BETTERBRANCH_TEST_MODULE_PATH%\" -cp \"%BETTERBRANCH_TEST_CLASSPATH%\" --module io.github.bmarwell.betterbranch/io.github.bmarwell.betterbranch.BetterBranch\nexit /b %ERRORLEVEL%\n"
                : "#!/usr/bin/env sh\nset -eu\nexec \"$BETTERBRANCH_TEST_JAVA\" --module-path \"$BETTERBRANCH_TEST_MODULE_PATH\" -cp \"$BETTERBRANCH_TEST_CLASSPATH\" --module io.github.bmarwell.betterbranch/io.github.bmarwell.betterbranch.BetterBranch\n";
        Files.writeString(launcherPath, launcherContent, StandardCharsets.UTF_8);
        markExecutable(launcherPath);
        return launcherPath;
    }

    private static void runCommand(List<String> command, Path workingDirectory, String action)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(workingDirectory.toFile()).start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream inputStream = process.getInputStream(); InputStream errorStream = process.getErrorStream()) {
            inputStream.transferTo(output);
            errorStream.transferTo(output);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String commandLine = String.join(" ", command);
            String combinedOutput = output.toString(StandardCharsets.UTF_8);
            throw new IllegalStateException("Failed " + action + " with command " + commandLine + ":\n" + combinedOutput);
        }
    }

    private static String readResource(String resourceName) throws IOException {
        try (InputStream input = IntegrationTestWorkspaceExtension.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + resourceName);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> commandForScript(Path scriptPath, Path repositoryDirectory) {
        if (isWindows()) {
            return List.of("cmd", "/c", scriptPath.toString(), repositoryDirectory.toString());
        }
        return List.of("sh", scriptPath.toString(), repositoryDirectory.toString());
    }

    private static void markExecutable(Path path) throws IOException {
        if (isWindows()) {
            return;
        }

        try {
            Files.setPosixFilePermissions(
                    path,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    public record Workspace(
            Path rootDirectory, Path repositoryDirectory, Path flatLayoutLauncher, Path nestedLayoutLauncher)
            implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (!Files.exists(rootDirectory)) {
                return;
            }

            try (var stream = Files.walk(rootDirectory)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try {
                        deleteWithRetry(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                IOException cause = e.getCause();
                if (isWindows() && cause instanceof AccessDeniedException) {
                    System.err.println("Skipping best-effort IT workspace cleanup on Windows: " + cause.getMessage());
                    return;
                }
                throw cause;
            }
        }

        private static void deleteWithRetry(Path path) throws IOException {
            boolean isWindowsPlatform = isWindows();
            int attempts = isWindowsPlatform ? WINDOWS_DELETE_RETRIES : 1;
            for (int attempt = 0; attempt < attempts; attempt++) {
                try {
                    Files.deleteIfExists(path);
                    return;
                } catch (IOException e) {
                    if (attempt + 1 >= attempts) {
                        throw e;
                    }
                    try {
                        Thread.sleep(WINDOWS_DELETE_RETRY_DELAY_MILLIS);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while deleting " + path, interruptedException);
                    }
                }
            }
        }
    }
}
