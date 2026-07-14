package io.github.bmarwell.betterbranch.output;

import io.github.bmarwell.betterbranch.value.BranchInfo;
import java.util.function.Function;

public record Column(
        String name, int width, String ansiColourCode, Function<BranchInfo.CommitBranchInfo, Object> valueExtractor) {}
