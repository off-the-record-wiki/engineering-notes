package com.example.batch.logcollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogStatistics {

    static final List<String> LEVELS = List.of("INFO", "WARN", "ERROR");

    private final Map<String, Map<String, Long>> counts = new TreeMap<>();

    public void add(String server, String level) {
        counts.computeIfAbsent(server, key -> new HashMap<>()).merge(level, 1L, Long::sum);
    }

    public List<String> servers() {
        return List.copyOf(counts.keySet());
    }

    public long count(String server, String level) {
        return counts.getOrDefault(server, Map.of()).getOrDefault(level, 0L);
    }

    public long total(String level) {
        return counts.values().stream().mapToLong(levels -> levels.getOrDefault(level, 0L)).sum();
    }

    public long total() {
        return counts.values().stream()
                .flatMap(levels -> levels.values().stream())
                .mapToLong(Long::longValue)
                .sum();
    }
}