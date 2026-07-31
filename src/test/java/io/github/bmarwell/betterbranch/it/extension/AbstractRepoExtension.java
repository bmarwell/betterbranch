package io.github.bmarwell.betterbranch.it.extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRepoExtension
        implements BeforeEachCallback, AfterEachCallback, ExecutionCondition, ParameterResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRepoExtension.class);

    public abstract ExtensionContext.Namespace getNamespace();

    public abstract @Nullable String setupWindowsScript();

    public abstract @Nullable String setupUnixScript();

    public abstract boolean supportsWindows();

    public abstract boolean supportsUnix();

    @Override
    public void beforeEach(ExtensionContext context) {
        //noinspection resource
        context.getStore(getNamespace())
                .computeIfAbsent(context.getUniqueId(), _ -> createWorkspace(), GitWorkspace.class);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final var store = context.getStore(getNamespace());

        final GitWorkspace workspace = store.get(context.getUniqueId(), GitWorkspace.class);

        if (workspace != null) {
            workspace.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, @NonNull ExtensionContext extensionContext) {
        final Class<?> parameterType = parameterContext.getParameter().getType();

        return parameterType.equals(GitWorkspace.class) || parameterType.equals(BetterBranchRunner.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        final var store = extensionContext.getStore(getNamespace());

        final Class<?> parameterType = parameterContext.getParameter().getType();

        if (parameterType.equals(GitWorkspace.class)) {
            return store.get(extensionContext.getUniqueId(), parameterType);
        }

        if (parameterType.equals(BetterBranchRunner.class)) {
            final GitWorkspace workspace = store.get(extensionContext.getUniqueId(), GitWorkspace.class);

            if (workspace == null) {
                throw new IllegalStateException(
                        "GitWorkspace not found in store for unique ID: " + extensionContext.getUniqueId());
            }

            return new BetterBranchRunner(workspace, workspace.launcher(), workspace.repositoryDirectory());
        }

        throw new IllegalArgumentException("unsupported parameter type: " + parameterType);
    }

    @Override
    public @NonNull ConditionEvaluationResult evaluateExecutionCondition(@NonNull ExtensionContext context) {
        if (isWindows() && !supportsWindows()) {
            return ConditionEvaluationResult.disabled("Test disabled on Windows.");
        }

        if (!isWindows() && !supportsUnix()) {
            return ConditionEvaluationResult.disabled("Test disabled on Unix.");
        }

        return ConditionEvaluationResult.enabled("Test enabled.");
    }

    private GitWorkspace createWorkspace() {
        try {
            Path rootDirectory = Files.createTempDirectory("betterbranch-it-");
            Path repositoryDirectory = rootDirectory.resolve("repo");

            runSetupScript(rootDirectory, repositoryDirectory);

            Path launcher = createLauncher();

            return new GitWorkspace(rootDirectory, repositoryDirectory, launcher, isWindows());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while preparing integration test workspace.", e);
        }
    }

    private void runSetupScript(Path rootDirectory, Path repositoryDirectory) throws IOException, InterruptedException {
        String resourceName = isWindows() ? setupWindowsScript() : setupUnixScript();
        Path setupScript = rootDirectory.resolve(isWindows() ? "setup-repo.cmd" : "setup-repo.sh");
        Files.writeString(setupScript, readResource(resourceName), StandardCharsets.UTF_8);
        markExecutable(setupScript);

        runCommand(commandForScript(setupScript, repositoryDirectory), rootDirectory, "setting up git repository");
    }

    private static Path createLauncher() throws IOException {
        final String betterbranchBin = System.getenv("BETTERBRANCH_DIR");
        if (isWindows()) {
            return Path.of(betterbranchBin).resolve("betterbranch.bar");
        }

        return Path.of(betterbranchBin).resolve("betterbranch");
    }

    private static void runCommand(List<String> command, Path workingDirectory, String action)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream inputStream = process.getInputStream()) {
            inputStream.transferTo(output);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String commandLine = String.join(" ", command);
            String combinedOutput = output.toString(StandardCharsets.UTF_8);
            throw new IllegalStateException(
                    "Failed " + action + " with command " + commandLine + ":\n" + combinedOutput);
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
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
