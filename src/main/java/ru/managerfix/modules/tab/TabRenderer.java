package ru.managerfix.modules.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders tab header, footer, player list names, and team-based sort order.
 * <p>
 * Visual order in Minecraft tab list is controlled by Scoreboard Team names (alphabetical).
 * This renderer assigns each player to a team named by their sorted position (tab_000, tab_001, ...)
 * so the actual visual order matches LuckPerms weight sorting.
 * <p>
 * Also handles NAME_TAG_VISIBILITY for NamesModule (NEVER when player has custom nick TextDisplay).
 */
public final class TabRenderer {

    private static final String TEAM_PREFIX = "tab_";
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final TabConfig config;
    private final TabModule module;
    private final LuckPermsSortService sortService;

    /** Cache: viewer UUID -> last sent header+footer plain text to detect changes. */
    private final Map<UUID, String> headerFooterCache = new ConcurrentHashMap<>();
    /** Cache: player UUID -> last set playerListName plain text. */
    private final Map<UUID, String> playerNameCache = new ConcurrentHashMap<>();
    /** Cache: player UUID -> currently assigned team name. */
    private final Map<UUID, String> teamAssignments = new ConcurrentHashMap<>();

    public TabRenderer(TabConfig config, TabModule module, LuckPermsSortService sortService) {
        this.config = config;
        this.module = module;
        this.sortService = sortService;
    }

    // ========================= Header / Footer =========================

    public Component buildHeader(Player viewer) {
        List<String> lines = config.getHeader();
        if (lines.isEmpty()) return Component.empty();
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            String resolved = replaceBuiltinPlaceholders(viewer, line);
            resolved = MessageUtil.setPlaceholders(viewer, resolved);
            components.add(MessageUtil.parse(resolved));
        }
        return Component.join(JoinConfiguration.newlines(), components);
    }

    public Component buildFooter(Player viewer) {
        List<String> lines = config.getFooter();
        if (lines.isEmpty()) return Component.empty();
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            String resolved = replaceBuiltinPlaceholders(viewer, line);
            resolved = MessageUtil.setPlaceholders(viewer, resolved);
            components.add(MessageUtil.parse(resolved));
        }
        return Component.join(JoinConfiguration.newlines(), components);
    }

    public void updateViewerTab(Player viewer) {
        Component header = buildHeader(viewer);
        Component footer = buildFooter(viewer);
        String cacheKey = PLAIN.serialize(header) + "\0" + PLAIN.serialize(footer);
        if (!cacheKey.equals(headerFooterCache.put(viewer.getUniqueId(), cacheKey))) {
            viewer.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    // ========================= Player list name =========================

    /**
     * Builds player list name Component. Uses {displayName} from player.displayName()
     * (NamesModule nick with colors — never touched by Tab). Uses afk-format when AFK.
     */
    public Component buildPlayerListName(Player player) {
        String format = isAfk(player) ? config.getAfkFormat() : config.getPlayerFormat();
        if (format == null || format.isEmpty()) return player.displayName();

        String resolved = format.replace("{name}", player.getName());
        resolved = replaceBuiltinPlaceholders(player, resolved);
        resolved = MessageUtil.setPlaceholders(player, resolved);

        // Insert displayName as Component to preserve nick colors (NamesModule).
        // IMPORTANT: use player.displayName() — never player.playerListName() (we set that).
        String[] parts = resolved.split("\\{displayName\\}", 2);
        if (parts.length == 2) {
            Component before = MessageUtil.parse(parts[0]);
            Component display = player.displayName();
            Component after = MessageUtil.parse(parts[1]);
            return Component.empty().append(before).append(display).append(after);
        }
        return MessageUtil.parse(resolved);
    }

    public void updatePlayerListName(Player player) {
        Component name = buildPlayerListName(player);
        String cacheKey = PLAIN.serialize(name);
        if (!cacheKey.equals(playerNameCache.put(player.getUniqueId(), cacheKey))) {
            player.playerListName(name);
        }
    }

    // ========================= Team-based sort order =========================

    /**
     * Assigns each player to a scoreboard team named by their sorted position (tab_000, tab_001, ...).
     * Minecraft sorts tab list by team name alphabetically, so this controls the visual order.
     * Also sets NAME_TAG_VISIBILITY = NEVER for players with custom NamesModule nicks.
     */
    private void updateSortTeams(List<Player> sorted) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        ru.managerfix.modules.names.NamesModule namesModule = getNamesModule();
        Set<String> usedTeams = new HashSet<>();

        for (int i = 0; i < sorted.size(); i++) {
            Player player = sorted.get(i);
            String teamName = String.format("%s%03d", TEAM_PREFIX, i);
            usedTeams.add(teamName);

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.prefix(Component.empty());
                team.suffix(Component.empty());
            }

            // Hide vanilla nametag if player has custom TextDisplay nick (NamesModule)
            boolean hideNametag = namesModule != null && namesModule.hasNickDisplay(player.getUniqueId());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                    hideNametag ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);

            String entry = player.getName();

            // Check if player is already in the correct team
            String currentTeam = teamAssignments.get(player.getUniqueId());
            if (teamName.equals(currentTeam) && team.hasEntry(entry)) {
                continue; // already in the right team, skip
            }

            // addEntry auto-removes the entry from any previous team on this scoreboard
            team.addEntry(entry);
            teamAssignments.put(player.getUniqueId(), teamName);
        }

        // Clean up stale tab_ teams (from players who left, or fewer players online)
        for (Team team : List.copyOf(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !usedTeams.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    // ========================= Full update =========================

    /**
     * Runs full tab update: header/footer, player list names, team sort order, nametag refresh.
     */
    public void updateAll() {
        List<Player> viewers = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> sorted = config.isSortByLuckPerms() && sortService != null
                ? sortService.getSortedPlayers(viewers)
                : new ArrayList<>(viewers);

        for (Player viewer : viewers) {
            updateViewerTab(viewer);
        }
        for (Player player : sorted) {
            updatePlayerListName(player);
        }

        // Assign teams for visual sort order in tab list
        updateSortTeams(sorted);

        // Refresh TextDisplay nametag text (prefix above head) so LuckPerms/Vault prefix changes are live
        refreshNametags(viewers);
    }

    // ========================= Nametag refresh =========================

    private void refreshNametags(List<Player> players) {
        ru.managerfix.modules.names.NamesModule namesModule = getNamesModule();
        if (namesModule == null) return;
        for (Player player : players) {
            if (namesModule.hasNickDisplay(player.getUniqueId())) {
                namesModule.refreshNametagText(player);
            }
        }
    }

    // ========================= Cleanup =========================

    /** Removes all cached data for a player and removes them from their sort team (on quit). */
    public void invalidate(Player player) {
        headerFooterCache.remove(player.getUniqueId());
        playerNameCache.remove(player.getUniqueId());

        String teamName = teamAssignments.remove(player.getUniqueId());
        if (teamName != null && Bukkit.getScoreboardManager() != null) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }
    }

    /** Cleans up all tab_ teams from scoreboard (on module disable). */
    public void cleanupAllTeams() {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : List.copyOf(board.getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
        teamAssignments.clear();
    }

    // ========================= Helpers =========================

    private ru.managerfix.modules.names.NamesModule getNamesModule() {
        if (!(module.getPlugin() instanceof ru.managerfix.ManagerFix mf)) return null;
        var opt = mf.getModuleManager().getEnabledModule("names");
        return opt.filter(m -> m instanceof ru.managerfix.modules.names.NamesModule)
            .map(m -> (ru.managerfix.modules.names.NamesModule) m)
            .orElse(null);
    }

    private String replaceBuiltinPlaceholders(Player player, String text) {
        if (text == null) return "";
        text = text.replace("%player_ping%", String.valueOf(player.getPing()));
        text = text.replace("%player_world%", player.getWorld().getName());
        long time = System.currentTimeMillis();
        int h = (int) ((time / (1000 * 60 * 60)) % 24);
        int m = (int) ((time / (1000 * 60)) % 60);
        text = text.replace("%server_time%", String.format("%02d:%02d", h, m));
        if (config.isClusterPlaceholders() && module.getPlugin() instanceof ru.managerfix.ManagerFix mf
                && mf.getConfigManager().isClusterEnabled()) {
            String online = String.valueOf(Bukkit.getOnlinePlayers().size());
            text = text.replace("%cluster_total_online%", online);
        }
        return text;
    }

    private boolean isAfk(Player player) {
        if (player == null || !(module.getPlugin() instanceof ru.managerfix.ManagerFix mf)) return false;
        var opt = mf.getModuleManager().getEnabledModule("afk");
        if (opt.isEmpty()) return false;
        ru.managerfix.modules.afk.AfkManager am = opt.filter(m -> m instanceof ru.managerfix.modules.afk.AfkModule)
            .map(m -> (ru.managerfix.modules.afk.AfkModule) m)
            .map(ru.managerfix.modules.afk.AfkModule::getAfkManager)
            .orElse(null);
        return am != null && am.isAfk(player);
    }
}
