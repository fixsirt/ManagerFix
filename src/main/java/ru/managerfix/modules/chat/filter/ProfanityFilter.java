package ru.managerfix.modules.chat.filter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.utils.MessageUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfanityFilter {

    public enum Action {
        BLOCK,
        CENSOR,
        WARN
    }

    public enum Strictness {
        SOFT,
        NORMAL,
        STRICT
    }

    public static class FilterResult {
        private final boolean blocked;
        private final String censoredMessage;
        private final String matchedWord;
        private final Action action;

        public FilterResult(boolean blocked, String censoredMessage, String matchedWord, Action action) {
            this.blocked = blocked;
            this.censoredMessage = censoredMessage;
            this.matchedWord = matchedWord;
            this.action = action;
        }

        public boolean isBlocked() { return blocked; }
        public String getCensoredMessage() { return censoredMessage; }
        public String getMatchedWord() { return matchedWord; }
        public Action getAction() { return action; }
    }

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final File logFile;

    private boolean enabled;
    private Action action;
    private String censorSymbol;
    private Strictness strictness;

    private Normalizer normalizer;
    private WhitelistChecker whitelistChecker;
    private DictionaryMatcher dictionaryMatcher;

    private String msgBlocked;
    private String msgCensored;
    private String msgWarned;

    private boolean loggingEnabled;
    private String logFormat;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public ProfanityFilter(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;

        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        this.logFile = new File(logsDir, "profanity.log");

        load();
    }

    public void load() {
        enabled = config.getBoolean("enabled", true);
        action = Action.valueOf(config.getString("action", "BLOCK").toUpperCase());
        censorSymbol = config.getString("censor-symbol", "***");
        strictness = Strictness.valueOf(config.getString("settings.strictness", "NORMAL").toUpperCase());

        // Load normalization settings
        Map<String, String> digitToLetter = null;
        Map<String, String> latinToCyrillic = null;
        List<String> separators = null;

        if (config.contains("normalization.digit-to-letter")) {
            digitToLetter = config.getConfigurationSection("normalization.digit-to-letter")
                    .getValues(false)
                    .entrySet()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey(),
                            e -> String.valueOf(e.getValue())
                    ));
        }

        if (config.contains("normalization.latin-to-cyrillic")) {
            latinToCyrillic = config.getConfigurationSection("normalization.latin-to-cyrillic")
                    .getValues(false)
                    .entrySet()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey(),
                            e -> String.valueOf(e.getValue())
                    ));
        }

        if (config.contains("normalization.separators")) {
            separators = config.getStringList("normalization.separators");
        }

        boolean removeSeparators = config.getBoolean("settings.remove-separators", true);
        boolean convertLeet = config.getBoolean("settings.convert-leet", true);
        boolean collapseRepeats = config.getBoolean("settings.collapse-repeats", true);
        int repeatThreshold = config.getInt("settings.repeat-threshold", 2);

        normalizer = new Normalizer(digitToLetter, latinToCyrillic, separators,
                removeSeparators, convertLeet, collapseRepeats, repeatThreshold);

        // Load whitelist
        boolean whitelistEnabled = config.getBoolean("whitelist.enabled", true);
        List<String> whitelistWords = config.getStringList("whitelist.words");
        List<String> whitelistPhrases = config.getStringList("whitelist.phrases");
        List<String> whitelistPatterns = config.getStringList("whitelist.patterns");

        whitelistChecker = new WhitelistChecker(whitelistEnabled, whitelistWords, whitelistPhrases, whitelistPatterns);

        // Load dictionaries based on strictness
        List<String> exactWords = config.getStringList("exact-words");
        List<String> rootPatterns = config.getStringList("root-patterns");
        List<String> maskedPatterns = config.getStringList("masked-patterns");

        int minWordLength = config.getInt("settings.min-word-length", 3);
        boolean checkTypos = config.getBoolean("settings.check-typos", false);
        int levenshteinMax = config.getInt("settings.levenshtein-max", 1);

        dictionaryMatcher = new DictionaryMatcher(exactWords, rootPatterns, maskedPatterns,
                minWordLength, checkTypos, levenshteinMax);

        // Load messages
        msgBlocked = config.getString("messages.blocked", "<#FF3366>Ваше сообщение заблокировано.</#FF3366>");
        msgCensored = config.getString("messages.censored", "<#FF3366>Ваше сообщение содержит нецензурные слова.</#FF3366>");
        msgWarned = config.getString("messages.warned", "<#FF3366>⚠️ Зафиксировано нарушение.</#FF3366>");

        // Load logging
        loggingEnabled = config.getBoolean("logging.enabled", true);
        logFormat = config.getString("logging.format",
                "[{date} {time}] [{player}] \"{message}\" -> {matched}");
    }

    public FilterResult check(String playerName, String message) {
        if (!enabled) {
            return new FilterResult(false, message, null, null);
        }

        if (message == null || message.isEmpty()) {
            return new FilterResult(false, message, null, null);
        }

        // Check whitelist first
        if (whitelistChecker.isAllowed(message)) {
            return new FilterResult(false, message, null, null);
        }

        // Normalize the message
        String normalized = normalizer.normalize(message);

        // Check if any word is whitelisted after normalization
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            if (whitelistChecker.isAllowedWord(word)) {
                return new FilterResult(false, message, null, null);
            }
        }

        // Find matches based on strictness
        List<DictionaryMatcher.Match> matches = dictionaryMatcher.findMatches(normalized);

        if (matches.isEmpty()) {
            return new FilterResult(false, message, null, null);
        }

        // Log the violation
        if (loggingEnabled) {
            logViolation(playerName, message, matches);
        }

        // Apply action
        switch (action) {
            case BLOCK:
                return new FilterResult(true, null, formatMatches(matches), Action.BLOCK);

            case CENSOR:
                String censored = censor(message, normalized, matches);
                return new FilterResult(true, censored, formatMatches(matches), Action.CENSOR);

            case WARN:
                return new FilterResult(false, message, formatMatches(matches), Action.WARN);

            default:
                return new FilterResult(false, message, null, null);
        }
    }

    public net.kyori.adventure.text.Component getMessageForAction(Action action) {
        switch (action) {
            case BLOCK:
                return MessageUtil.parse(msgBlocked);
            case CENSOR:
                return MessageUtil.parse(msgCensored);
            case WARN:
                return MessageUtil.parse(msgWarned);
            default:
                return net.kyori.adventure.text.Component.empty();
        }
    }

    private String censor(String original, String normalized, List<DictionaryMatcher.Match> matches) {
        String result = original;

        for (DictionaryMatcher.Match match : matches) {
            String badWord = match.getWord();
            if (badWord != null && !badWord.isEmpty()) {
                String regex = "\\b" + Pattern.quote(badWord) + "\\b";
                result = result.replaceAll("(?i)" + regex, censorSymbol);
            }
        }

        return result;
    }

    private String formatMatches(List<DictionaryMatcher.Match> matches) {
        return matches.stream()
                .map(m -> m.getWord() + "(" + m.getType() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void logViolation(String playerName, String message, List<DictionaryMatcher.Match> matches) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String logEntry = logFormat
                    .replace("{date}", dateFormat.format(new Date()))
                    .replace("{time}", timeFormat.format(new Date()))
                    .replace("{player}", playerName)
                    .replace("{message}", message)
                    .replace("{matched}", formatMatches(matches));

            writer.println(logEntry);
        } catch (IOException ignored) {
            plugin.getLogger().warning("Failed to write to profanity log: " + ignored.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Action getAction() {
        return action;
    }
}

class Pattern {
    public static java.util.regex.Pattern compile(String regex, int flags) {
        return java.util.regex.Pattern.compile(regex, flags);
    }

    public static String quote(String s) {
        return java.util.regex.Pattern.quote(s);
    }
}
