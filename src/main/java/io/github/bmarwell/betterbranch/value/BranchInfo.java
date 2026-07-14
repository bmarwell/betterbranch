package io.github.bmarwell.betterbranch.value;

import java.time.Instant;
import org.eclipse.jgit.revwalk.RevCommit;

public sealed interface BranchInfo permits BranchInfo.CommitBranchInfo, BranchInfo.MissingBranchInfo {

    String branchName();

    default String cleanBranchName() {
        if (branchName().startsWith("refs/heads/")) {
            return branchName().substring("refs/heads/".length());
        }

        return branchName();
    }

    record CommitBranchInfo(String branchName, RevCommit tip, Instant commitTime, long ahead, long behind)
            implements BranchInfo {}

    record MissingBranchInfo(String branchName, Throwable exception) implements BranchInfo {}
}
