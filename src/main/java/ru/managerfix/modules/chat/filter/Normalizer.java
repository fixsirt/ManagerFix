package ru.managerfix.modules.chat.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class Normalizer {

    private final Map<Character, Character> digitToLetter;
    private final Map<Character, Character> latinToCyrillic;
    private final Pattern separatorPattern;
    private final Pattern repeatPattern;
    private final boolean removeSeparators;
    private final boolean convertLeet;
    private final boolean collapseRepeats;
    private final int repeatThreshold;

    public Normalizer(Map<String, String> digitToLetter,
                      Map<String, String> latinToCyrillic,
                      java.util.List<String> separators,
                      boolean removeSeparators,
                      boolean convertLeet,
                      boolean collapseRepeats,
                      int repeatThreshold) {
        this.digitToLetter = new HashMap<>();
        if (digitToLetter != null) {
            for (Map.Entry<String, String> entry : digitToLetter.entrySet()) {
                if (entry.getKey().length() == 1 && entry.getValue().length() == 1) {
                    this.digitToLetter.put(entry.getKey().charAt(0), entry.getValue().charAt(0));
                }
            }
        }

        this.latinToCyrillic = new HashMap<>();
        if (latinToCyrillic != null) {
            for (Map.Entry<String, String> entry : latinToCyrillic.entrySet()) {
                if (entry.getKey().length() == 1 && entry.getValue().length() == 1) {
                    this.latinToCyrillic.put(entry.getKey().charAt(0), entry.getValue().charAt(0));
                }
            }
        }

        if (separators != null && !separators.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (String sep : separators) {
                if (sep.length() == 1) {
                    sb.append(Pattern.quote(sep));
                }
            }
            sb.append("]+");
            this.separatorPattern = Pattern.compile(sb.toString());
        } else {
            this.separatorPattern = null;
        }

        this.removeSeparators = removeSeparators;
        this.convertLeet = convertLeet;
        this.collapseRepeats = collapseRepeats;
        this.repeatThreshold = repeatThreshold;

        if (repeatThreshold > 1) {
            this.repeatPattern = Pattern.compile("(.)\\1{" + (repeatThreshold - 1) + ",}");
        } else {
            this.repeatPattern = null;
        }
    }

    public String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input.toLowerCase();

        // 1. Convert leet speak (digits to letters)
        if (convertLeet) {
            result = convertDigits(result);
        }

        // 2. Convert Latin to Cyrillic
        if (convertLeet) {
            result = convertLatin(result);
        }

        // 3. Remove separators between letters
        if (removeSeparators && separatorPattern != null) {
            result = separatorPattern.matcher(result).replaceAll("");
        }

        // 4. Collapse repeated letters
        if (collapseRepeats && repeatPattern != null) {
            result = repeatPattern.matcher(result).replaceAll("$1");
        }

        return result;
    }

    private String convertDigits(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            Character replacement = digitToLetter.get(c);
            sb.append(replacement != null ? replacement : c);
        }
        return sb.toString();
    }

    private String convertLatin(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            Character replacement = latinToCyrillic.get(c);
            sb.append(replacement != null ? replacement : c);
        }
        return sb.toString();
    }

    public String removeNonLetters(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Zа-яА-ЯёЁ]", "");
    }
}
