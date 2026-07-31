module io.github.bmarwell.betterbranch {
    requires org.eclipse.jgit;

    exports io.github.bmarwell.betterbranch;
    exports io.github.bmarwell.betterbranch.value;
    exports io.github.bmarwell.betterbranch.mapper;

    requires transitive org.slf4j;
}
