package io.github.bmarwell.betterbranch;

import io.github.bmarwell.betterbranch.mapper.BranchInfoMapper;
import io.github.bmarwell.betterbranch.output.OutputPrinter;
import io.github.bmarwell.betterbranch.value.BranchInfo;
import io.github.bmarwell.betterbranch.value.ShallowBranch;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class BetterBranch {

    private static final Comparator<ShallowBranch> BY_COMMITTERDATE_SHALLOW =
            Comparator.comparing(ShallowBranch::commitTime).reversed();

    public static void main(String[] args) {
        main();
    }

    static void main() {
        try (var repository = new FileRepositoryBuilder()
                        .setGitDir(Paths.get(".", ".git").toFile())
                        .readEnvironment()
                        .findGitDir()
                        .build();
                var git = new Git(repository);
                var walk = new RevWalk(repository)) {
            // Referenz-Branch (für behind / ahead)
            String currentBranchName = repository.getBranch();
            Ref currentBranchRef = repository.findRef("refs/heads/" + currentBranchName);

            if (currentBranchRef == null) {
                throw new IllegalStateException(
                        "Can't find currently checked out branch. Bare-Repos are not supported.");
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

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
