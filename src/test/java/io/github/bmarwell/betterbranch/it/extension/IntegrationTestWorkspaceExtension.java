package io.github.bmarwell.betterbranch.it.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public final class IntegrationTestWorkspaceExtension extends AbstractRepoExtension {

    private static final String SETUP_REPO_WINDOWS_SCRIPT = "/it/setup-repo.cmd";
    private static final String SETUP_REPO_UNIX_SCRIPT = "/it/setup-repo.sh";
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(IntegrationTestWorkspaceExtension.class);

    @Override
    public Namespace getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String setupWindowsScript() {
        return SETUP_REPO_WINDOWS_SCRIPT;
    }

    @Override
    public String setupUnixScript() {
        return SETUP_REPO_UNIX_SCRIPT;
    }

    @Override
    public boolean supportsWindows() {
        return true;
    }

    @Override
    public boolean supportsUnix() {
        return true;
    }
}
