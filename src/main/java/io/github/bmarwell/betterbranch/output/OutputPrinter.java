package io.github.bmarwell.betterbranch.output;

import io.github.bmarwell.betterbranch.value.BranchInfo;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

public final class OutputPrinter {

    private static final String RESET = "\33[0m";

    private static final List<Column> COLUMNS = List.of(
            new Column("Ahead", 5, "\33[0;32m", BranchInfo.CommitBranchInfo::ahead),
            new Column("Behind", 5, "\33[0;31m", BranchInfo.CommitBranchInfo::behind),
            new Column("Branch", 50, "\33[0;34m", BranchInfo.CommitBranchInfo::cleanBranchName),
            new Column(
                    "Last Commit",
                    20,
                    "\33[0;33m",
                    (BranchInfo.CommitBranchInfo ci) ->
                            HumanReadableTime.formatElapsed(ci.commitTime(), Instant.now())));

    public static void print(SortedSet<BranchInfo.CommitBranchInfo> branchInfoList) {
        printHeader();

        for (BranchInfo.CommitBranchInfo commitBranchInfo : branchInfoList) {
            printLine(commitBranchInfo);
        }
    }

    public static void printLine(BranchInfo.CommitBranchInfo commitBranchInfo) {
        for (Column column : COLUMNS) {
            if (AnsiSupport.supportsAnsi()) {
                var msgformat = String.format(Locale.ROOT, "%%s%%-%ds%%s ", column.width());
                System.out.printf(
                        Locale.ROOT,
                        msgformat,
                        column.ansiColourCode(),
                        column.valueExtractor().apply(commitBranchInfo),
                        RESET);

                continue;
            }

            var msgformat = String.format(Locale.ROOT, "%%-%ds ", column.width());
            System.out.printf(Locale.ROOT, msgformat, column.valueExtractor().apply(commitBranchInfo));
        }

        System.out.println();
    }

    public static void printHeader() {
        for (Column column : COLUMNS) {
            printHeaderName(column);
        }

        System.out.println();

        for (Column column : COLUMNS) {
            printHeaderDashes(column);
        }

        System.out.println();
    }

    private static void printHeaderDashes(Column column) {
        if (AnsiSupport.supportsAnsi()) {
            var msgformat = String.format(Locale.ROOT, "%%s%%-%ds%%s ", column.width());
            System.out.printf(Locale.ROOT, msgformat, column.ansiColourCode(), "-".repeat(column.width()), RESET);

            return;
        }

        var msgformat = String.format(Locale.ROOT, "%%-%ds ", column.width());
        System.out.printf(Locale.ROOT, msgformat, "-".repeat(column.width()));
    }

    private static void printHeaderName(Column column) {
        if (AnsiSupport.supportsAnsi()) {
            var msgformat = String.format(Locale.ROOT, "%%s%%-%ds%%s ", column.width());
            System.out.printf(Locale.ROOT, msgformat, column.ansiColourCode(), column.name(), RESET);
            return;
        }

        var msgformat = String.format(Locale.ROOT, "%%-%ds ", column.width());
        System.out.printf(Locale.ROOT, msgformat, column.name());
    }
}
