package io.github.bmarwell.betterbranch.mapper;

import io.github.bmarwell.betterbranch.value.BranchInfo;
import io.github.bmarwell.betterbranch.value.ShallowBranch;
import java.io.IOException;
import java.time.Instant;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

public class BranchInfoMapper {

    private BranchInfoMapper() {
        /* utility class */
    }

    public static ShallowBranch toShallowBranch(Ref branchRef, Git git) {
        try (var walk = new RevWalk(git.getRepository())) {
            RevCommit tip = walk.parseCommit(branchRef.getObjectId());
            Instant commitTime = Instant.ofEpochSecond(tip.getCommitTime());

            return new ShallowBranch(branchRef, branchRef.getName(), tip, commitTime);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BranchInfo toBranchInfo(ShallowBranch sb, Git git, RevCommit refCommit) {
        try {
            long ahead = countCommits(git, sb.tip(), refCommit);
            long behind = countCommits(git, refCommit, sb.tip());

            return new BranchInfo.CommitBranchInfo(sb.branchName(), sb.tip(), sb.commitTime(), ahead, behind);
        } catch (Exception ex) {
            return new BranchInfo.MissingBranchInfo(sb.branchName(), ex);
        }
    }

    private static long countCommits(Git git, RevCommit start, RevCommit exclude) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE, true);

            // parse both commits *inside* this walk
            RevCommit startWalk = walk.parseCommit(start);
            RevCommit excludeWalk = walk.parseCommit(exclude);

            walk.markStart(startWalk);
            walk.markUninteresting(excludeWalk);

            long cnt = 0;
            for (RevCommit c : walk) {
                cnt++;
            }
            return cnt;
        }
    }
}
