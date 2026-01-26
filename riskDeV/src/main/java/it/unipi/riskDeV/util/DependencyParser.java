package it.unipi.riskDeV.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unipi.riskDeV.model.Constraints;

public class DependencyParser {

    // Pattern per versioni
    private static final Pattern VERSION_GTE = Pattern.compile(">=[ ]*([0-9a-zA-Z\\._-]+)");
    private static final Pattern VERSION_LTE = Pattern.compile("<=[ ]*([0-9a-zA-Z\\._-]+)");
    private static final Pattern VERSION_GT  = Pattern.compile(">[ ]*([0-9a-zA-Z\\._-]+)");
    private static final Pattern VERSION_LT  = Pattern.compile("<[ ]*([0-9a-zA-Z\\._-]+)");
    private static final Pattern VERSION_EQ  = Pattern.compile("==[ ]*([0-9a-zA-Z\\._-]+)");
    private static final Pattern VERSION_NEQ = Pattern.compile("!=[ ]*([0-9a-zA-Z\\._-]+)");

    private static final Pattern NAME_PATTERN = Pattern.compile("^([a-zA-Z0-9\\-_.]+)");

    public static Constraints parseFullString(String full) {
        Constraints constraint = new Constraints();
        constraint.setFull(full);

        if (full == null || full.isEmpty()) {
            return constraint;
        }

        // Separiamo la parte principale da eventuali condizioni (dopo ;)
        String mainPart;
        if (full.contains(";")) {
            String[] parts = full.split(";", 2);
            mainPart = parts[0].trim();
        } else {
            mainPart = full.trim();
        }

        // Nome pacchetto
        Matcher nameMatcher = NAME_PATTERN.matcher(mainPart);
        if (nameMatcher.find()) {
            constraint.setName(nameMatcher.group(1));
        }

        // Versioni
        constraint.setVersionGte(findLastMatch(VERSION_GTE, mainPart));
        constraint.setVersionLte(findLastMatch(VERSION_LTE, mainPart));
        constraint.setVersionGt(findLastMatch(VERSION_GT, mainPart));
        constraint.setVersionLt(findLastMatch(VERSION_LT, mainPart));
        constraint.setVersionEq(findLastMatch(VERSION_EQ, mainPart));
        constraint.setVersionNeq(findLastMatch(VERSION_NEQ, mainPart));

        return constraint;
    }

    private static String findLastMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        return lastMatch;
    }
}