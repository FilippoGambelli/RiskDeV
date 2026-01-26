package it.unipi.riskDeV.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class Utility {
    private static final Map<String, Integer> VERSION_WEIGHTS = Map.of(
        "dev", -4, "alpha", -3, "a", -3, "beta", -2, "b", -2, 
        "rc", -1, "c", -1, "pre", -1, "post", 1, "pl", 1
    );

    public List<Integer> generateVersionArray(String versionStr) {
        List<Integer> normalized = new ArrayList<>();
        if (versionStr == null) return List.of(0, 0, 0, 0, 0, 0);

        Matcher m = Pattern.compile("(\\d+|[a-z]+)").matcher(versionStr.toLowerCase());
        
        while (m.find()) {
            String part = m.group();
            if (part.matches("\\d+")) {
                normalized.add(Integer.parseInt(part));
            } else {
                normalized.add(VERSION_WEIGHTS.getOrDefault(part, -5));
            }
        }

        while (normalized.size() < 6) {
            normalized.add(0);
        }
        
        return normalized.subList(0, 6);
    }
}
