package com.example.batch.dynamicdata;

import java.util.List;
import java.util.Map;

public class RegionDirectory {

    private static final Map<String, List<String>> REGIONS_BY_GROUP = Map.of(
            "capital", List.of("SEOUL", "INCHEON"),
            "south", List.of("BUSAN", "DAEGU", "ULSAN"));

    public List<String> findRegions(String group) {
        return REGIONS_BY_GROUP.getOrDefault(group, List.of());
    }
}