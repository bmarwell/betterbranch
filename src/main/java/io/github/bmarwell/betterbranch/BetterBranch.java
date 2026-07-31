package io.github.bmarwell.betterbranch;

import io.github.bmarwell.betterbranch.mapper.BranchInfoMapper;
import io.github.bmarwell.betterbranch.output.OutputPrinter;
import io.github.bmarwell.betterbranch.value.BranchInfo;
import io.github.bmarwell.betterbranch.value.ShallowBranch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class BetterBranch {

    private static final Comparator<ShallowBranch> BY_COMMITTERDATE_SHALLOW =
            Comparator.comparing(ShallowBranch::commitTime).reversed();

    static void main() {
        final BetterBranch betterBranch = new BetterBranch();
        betterBranch.listAllBranches();
    }

    public BetterBranch() {
        super();
    }

    public void listAllBranches() {
        try (var repository = getRepository();
                var git = new Git(repository);
                var walk = new RevWalk(repository)) {
            // Referenz-Branch (für behind / ahead)
            String currentBranchName = repository.getBranch();
            Ref currentBranchRef = repository.findRef("refs/heads/" + currentBranchName);

            if (currentBranchRef == null) {
                throw new IllegalStateException(
                        "Can't find currently checked out branch. Bare-Repos are not supported. CurrentBranchName: "
                                + currentBranchName + " dir: " + repository.getDirectory());
            }

            OutputPrinter.printHeader();

            RevCommit refCommit = walk.parseCommit(currentBranchRef.getObjectId());

            Stream<ShallowBranch> allBranches =
                    git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().stream()
                            .filter(branchRef -> branchRef.getName().startsWith("refs/heads/"))
                            .map(branchRef -> BranchInfoMapper.toShallowBranch(branchRef, git))
                            .sorted(BY_COMMITTERDATE_SHALLOW);

            allBranches
                    .map(sb -> BranchInfoMapper.toBranchInfo(sb, git, refCommit))
                    .filter(bi -> bi instanceof BranchInfo.CommitBranchInfo)
                    .map(bi -> (BranchInfo.CommitBranchInfo) bi)
                    .forEach(OutputPrinter::printLine);

        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        } catch (GitAPIException gitAPIException) {
            throw new RuntimeException(gitAPIException);
        }
    }

    private Repository getRepository() throws IOException {
        final Path pwd = Paths.get(".").toAbsolutePath();
        final Path pwdDotGit = pwd.resolve(".git");

        if (Files.exists(pwdDotGit) && Files.isDirectory(pwdDotGit)) {
            return new FileRepositoryBuilder()
                    .setGitDir(pwdDotGit.toFile())
                    .readEnvironment()
                    .build();
        }

        if (Files.exists(pwdDotGit) && Files.isRegularFile(pwdDotGit)) {
            return getRepositoryFromWorktree(pwdDotGit, pwd);
        }

        throw new IllegalStateException("Can't find a valid git repository in the current directory.");
    }

    private static Repository getRepositoryFromWorktree(Path pwdDotGit, Path pwd) throws IOException {
        // Worktree
        final List<String> gitFileContents = Files.readAllLines(pwdDotGit);

        if (!gitFileContents.getFirst().contains(": ")) {
            throw new IllegalArgumentException(
                    ".git does not contain a correct configuration, expected key-value pairs, but was: "
                            + gitFileContents);
        }

        final String gitDirValue = gitFileContents.stream()
                .filter(line -> line.startsWith("gitdir: "))
                .findFirst()
                .map(line -> line.substring("gitdir: ".length()).strip())
                .orElseThrow(() -> new IllegalArgumentException(
                        ".git does not contain a correct configuration, expected a 'gitdir' entry, but was: "
                                + gitFileContents));

        final Path gitDirPath = Paths.get(gitDirValue);

        if (!Files.isDirectory(gitDirPath)) {
            throw new IllegalArgumentException(
                    ".git does not contain a correct path to a git repository, was: [" + gitFileContents + "].");
        }

        return new FileRepositoryBuilder()
                .readEnvironment()
                .setWorkTree(pwd.toFile())
                .setGitDir(gitDirPath.toFile())
                .build();
    }
}
