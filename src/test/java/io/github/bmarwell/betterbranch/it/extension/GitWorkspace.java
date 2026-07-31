package io.github.bmarwell.betterbranch.it.extension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GitWorkspace(Path rootDirectory, Path repositoryDirectory, Path launcher, boolean isWindows)
        implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitWorkspace.class);

    private static final int WINDOWS_DELETE_RETRIES = 10;
    private static final long WINDOWS_DELETE_RETRY_DELAY_MILLIS = 100L;

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
                LOGGER.warn("Skipping best-effort IT workspace cleanup on Windows: {}", cause.getMessage(), cause);
                return;
            }
            throw cause;
        }
    }

    private void deleteWithRetry(Path path) throws IOException {
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
