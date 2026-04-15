package ru.managerfix.modules.chat.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class WhitelistChecker {

    private final boolean enabled;
    private final List<String> whitelistWords;
    private final List<String> whitelistPhrases;
    private final List<Pattern> whitelistPatterns;

    public WhitelistChecker(boolean enabled,
                           List<String> whitelistWords,
                           List<String> whitelistPhrases,
                           List<String> whitelistPatterns) {
        this.enabled = enabled;
        this.whitelistWords = whitelistWords != null ? whitelistWords : new ArrayList<>();
        this.whitelistPhrases = whitelistPhrases != null ? whitelistPhrases : new ArrayList<>();
        this.whitelistPatterns = new ArrayList<>();

        if (whitelistPatterns != null) {
            for (String pattern : whitelistPatterns) {
                try {
                    this.whitelistPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean isAllowed(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return !enabled;
        }

        String normalizedText = text.toLowerCase().trim();

        // Check exact word matches
        for (String word : whitelistWords) {
            if (normalizedText.equalsIgnoreCase(word)) {
                return true;
            }
            // Also check if the word is surrounded by word boundaries
            String pattern = "\\b" + Pattern.quote(word) + "\\b";
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(normalizedText).find()) {
                return true;
            }
        }

        // Check phrase matches (full message)
        for (String phrase : whitelistPhrases) {
            if (normalizedText.contains(phrase.toLowerCase())) {
                return true;
            }
        }

        // Check regex patterns
        for (Pattern pattern : whitelistPatterns) {
            if (pattern.matcher(normalizedText).find()) {
                return true;
            }
        }

        return false;
    }

    public boolean isAllowedWord(String word) {
        if (!enabled || word == null || word.isEmpty()) {
            return !enabled;
        }

        String normalizedWord = word.toLowerCase().trim();

        // Check exact matches
        for (String whitelistWord : whitelistWords) {
            if (normalizedWord.equalsIgnoreCase(whitelistWord)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
