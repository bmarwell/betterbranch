package io.github.bmarwell.betterbranch.it.extension;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public final class WorktreeExtension extends AbstractRepoExtension {

    private static final String SETUP_REPO_UNIX_SCRIPT = "/it/repo-with-workspace.sh";
    private static final Namespace NAMESPACE = Namespace.create(WorktreeExtension.class);

    @Override
    public Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    public @Nullable String setupWindowsScript() {
        return null;
    }

    @Override
    public String setupUnixScript() {
        return SETUP_REPO_UNIX_SCRIPT;
    }

    @Override
    public boolean supportsWindows() {
        return false;
    }

    @Override
    public boolean supportsUnix() {
        return true;
    }
}
