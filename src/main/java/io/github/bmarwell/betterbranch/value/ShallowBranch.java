package io.github.bmarwell.betterbranch.value;

import java.time.Instant;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

public record ShallowBranch(Ref branch, String branchName, RevCommit tip, Instant commitTime) {}
