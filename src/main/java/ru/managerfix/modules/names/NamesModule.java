package ru.managerfix.modules.names;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.event.EventBus;
import ru.managerfix.event.PlayerNickChangeEvent;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ServiceRegistry;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names module: /nick, display name, nametag above head, cooldown, admin GUI, EventBus, Cluster sync.
 * <p>
 * Nametag above head: the vanilla nametag is HIDDEN via Scoreboard Team (NAME_TAG_VISIBILITY = NEVER).
 * A TextDisplay entity riding the player shows only the nick — no real name visible.
 * Tab list and chat use player.playerListName / player.displayName (fully replaces name).
 */
public final class NamesModule extends AbstractModule implements Listener {

    private static final String MODULE_NAME = "names";
    private static final String CONFIG_FILE = "names/config.yml";
    private static final String METADATA_NICK = "nick";

    /** Identity rotation (no rotation). */
    private static final AxisAngle4f NO_ROTATION = new AxisAngle4f(0, 0, 0, 1);
    /** Identity scale. */
    private static final Vector3f UNIT_SCALE = new Vector3f(1, 1, 1);

    private FileConfiguration moduleConfig;
    private NamesListener namesListener;

    /** Admin UUID -> target player UUID (pending nick from chat). */
    private final Map<UUID, UUID> pendingNickTarget = new ConcurrentHashMap<>();
    /** Player UUID -> TextDisplay entity UUID used for the nick nametag. */
    private final Map<UUID, UUID> nickDisplays = new ConcurrentHashMap<>();
    /** Player UUID -> true = nick hidden, false = nick visible */
    private final Map<UUID, Boolean> hiddenNicks = new ConcurrentHashMap<>();
    /** Глобальное состояние скрытия ников для всех игроков */
    private boolean globalNickHidden = false;

    public NamesModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    // ========================= Module lifecycle =========================

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    protected void enable() {
        LoggerUtil.debug("[Names] NamesModule.enable() called");
        try {
            moduleConfig = configManager.getModuleConfig(CONFIG_FILE);
            
            // Инициализация команд
            initCommandConfig(MODULE_NAME);
            
            if (moduleConfig == null) {
                plugin.getLogger().warning("[Names] modules/names/config.yml not loaded — using defaults");
            }

            ProfileManager profileManager = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
            EventBus eventBus = plugin instanceof ManagerFix mf ? mf.getEventBus() : null;

            if (profileManager != null) {
                namesListener = new NamesListener(this, profileManager);
                plugin.getServer().getPluginManager().registerEvents(namesListener, plugin);
                if (eventBus != null) {
                    eventBus.registerListener(namesListener);
                }
            }

            // Register this module as listener for potion effects
            plugin.getServer().getPluginManager().registerEvents(this, plugin);

            if (plugin instanceof ManagerFix mf) {
                NamesService namesService = new NamesService(mf, this, profileManager);
                NickCommand nickCommand = new NickCommand(mf, this, namesService);
                NickAdminCommand nickAdminCommand = new NickAdminCommand(mf, this, namesService);
                NamesCommand namesCommand = new NamesCommand(mf, this);
                HideNickCommand hidenickCommand = new HideNickCommand(mf, this);

                registerCommand("nick", nickCommand, nickCommand);
                registerCommand("nickadmin", nickAdminCommand, nickAdminCommand);
                registerCommand("names", namesCommand, namesCommand);
                registerCommand("hidenick", hidenickCommand, hidenickCommand);
            }

            // Re-apply labels for all online players (important after /managerfix reload)
            if (profileManager != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    profileManager.getCachedProfile(online.getUniqueId()).ifPresent(profile -> {
                        Object nick = profile.getMetadata(METADATA_NICK).orElse(null);
                        if (nick instanceof String s && !s.isEmpty()) {
                            applyNick(online, s);
                            LoggerUtil.debug("[Names] Re-applied nick on enable for " + online.getName());
                        } else {
                            applyDefaultDisplay(online);
                            LoggerUtil.debug("[Names] Applied default display on enable for " + online.getName());
                        }
                    });
                }
            }

            LoggerUtil.debug("Names module enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("[Names] NamesModule failed to enable");
            e.printStackTrace();
            throw e;
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
                                 org.bukkit.command.TabCompleter completer) {
        org.bukkit.command.PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().severe("[Names] Command '" + name + "' is NULL! Check plugin.yml");
        } else {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(completer);
            LoggerUtil.debug("[Names] Command '" + name + "' successfully registered.");
        }
    }

    @Override
    protected void disable() {
        pendingNickTarget.clear();

        // Remove all TextDisplay entities and reset nicks
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeNickDisplay(p.getUniqueId());
            resetNick(p);
        }

        // Unregister all our scoreboard teams (prefixed with "mf_")
        if (Bukkit.getScoreboardManager() != null) {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team team : List.copyOf(board.getTeams())) {
                if (team.getName().startsWith("mf_")) {
                    team.unregister();
                }
            }
        }

        nickDisplays.clear();

        if (namesListener != null) {
            HandlerList.unregisterAll(namesListener);
            if (plugin instanceof ManagerFix mf && mf.getEventBus() != null) {
                mf.getEventBus().unregisterListener(namesListener);
            }
        }
        namesListener = null;
        moduleConfig = null;
        LoggerUtil.debug("Names module disabled.");
    }

    // ========================= Config getters =========================

    public int getNicknameCooldownSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("nickname-cooldown-seconds", 10) : 10;
    }

    public boolean isAllowHex() {
        return moduleConfig != null && moduleConfig.getBoolean("allow-hex", true);
    }

    public int getMaxLength() {
        return moduleConfig != null ? moduleConfig.getInt("max-length", 16) : 16;
    }

    public String getDisplayFormat() {
        return moduleConfig != null
                ? moduleConfig.getString("display-format", "{prefix} {displayName}")
                : "{prefix} {displayName}";
    }

    public boolean isAdminChangeBroadcast() {
        return moduleConfig != null && moduleConfig.getBoolean("admin-change-broadcast", false);
    }

    /** Y offset for the TextDisplay above the player's head. */
    public float getNameTagOffsetY() {
        return moduleConfig != null ? (float) moduleConfig.getDouble("nametag-offset-y", 0.3) : 0.3f;
    }

    public static String getNickMetadataKey() {
        return METADATA_NICK;
    }

    // ========================= Vault integration =========================

    private String getVaultPrefix(Player player) {
        if (player == null || plugin.getServer().getPluginManager().getPlugin("Vault") == null) return "";
        try {
            var reg = plugin.getServer().getServicesManager()
                    .getRegistration(Class.forName("net.milkbowl.vault.chat.Chat"));
            if (reg != null) {
                Object chat = reg.getProvider();
                String prefix = (String) chat.getClass()
                        .getMethod("getPlayerPrefix", Player.class)
                        .invoke(chat, player);
                return prefix != null ? prefix : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    // ========================= Nick parsing =========================

    /** Parses raw nick string to Component (& codes always; HEX if allowHex or bypassFormat). */
    public Component parseNickToComponent(String raw) {
        return parseNickToComponent(raw, false);
    }

    /** bypassFormat: if true, always allow HEX/any colors (admin bypass). */
    public Component parseNickToComponent(String raw, boolean bypassFormat) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        if (bypassFormat || isAllowHex()) {
            return MessageUtil.parse(raw);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    // ========================= Apply / Reset nick =========================

    /**
     * Applies nick to player:
     * <ul>
     *   <li>displayName (chat rendering)</li>
     *   <li>playerListName (tab list — fully replaces name)</li>
     *   <li>Scoreboard Team with NAME_TAG_VISIBILITY = NEVER (hides vanilla nametag)</li>
     *   <li>TextDisplay entity riding the player (shows only the nick above head)</li>
     * </ul>
     * Must be called on the main thread.
     */
    public void applyNick(Player player, String rawNick) {
        if (player == null || !player.isOnline()) return;

        // Если ники скрыты глобально - скрываем ВСЕ ники (и кастомные, и обычные)
        if (globalNickHidden) {
            hideVanillaNameTag(player);
            removeNickDisplay(player.getUniqueId());
            // displayName и playerListName оставляем как есть (для чата и таба)
            return;
        }

        if (rawNick == null || rawNick.isEmpty()) {
            resetNick(player);
            return;
        }

        // --- Pure nick (no prefix) — used for chat, tab, commands ---
        Component nickComp = parseNickToComponent(rawNick);

        // Chat display name = pure nick (so /pm <nick> works correctly)
        player.displayName(nickComp);
        // Tab list = pure nick (no prefix)
        player.playerListName(nickComp);

        // --- Full display with prefix (for nametag above head only) ---
        String vaultPrefix = getVaultPrefix(player);
        String displayFormat = getDisplayFormat();
        String fullRaw = displayFormat
                .replace("{prefix}", vaultPrefix != null ? vaultPrefix : "")
                .replace("{displayName}", rawNick)
                .replace("{nick}", rawNick);
        Component nametagComp = MessageUtil.parse(fullRaw);

        // 1) Hide vanilla nametag via team
        hideVanillaNameTag(player);
        // 2) Show prefix + nick above head via TextDisplay
        createOrUpdateNickDisplay(player, nametagComp);

        updateTabName(player);
        LoggerUtil.debug("[Names] Applied nick for " + player.getName() + ": " + rawNick);
    }

    /**
     * Resets player to real name: displayName, playerListName, vanilla nametag restored.
     * Must be called on the main thread.
     */
    public void resetNick(Player player) {
        if (player == null || !player.isOnline()) return;

        Component real = Component.text(player.getName());
        player.displayName(real);
        player.playerListName(real);

        // Remove TextDisplay and restore vanilla nametag
        removeNickDisplay(player.getUniqueId());
        showVanillaNameTag(player);

        updateTabName(player);
        LoggerUtil.debug("[Names] Reset nick for " + player.getName());
    }

    /**
     * Applies default display for players без кастомного ника:
     * - displayName/playerListName = реальное имя
     * - скрывает ванильный неймтаг
     * - создаёт TextDisplay с форматом {prefix} {displayName} (где displayName = реальное имя)
     */
    public void applyDefaultDisplay(Player player) {
        if (player == null || !player.isOnline()) return;

        Component real = Component.text(player.getName());
        player.displayName(real);
        player.playerListName(real);

        String vaultPrefix = getVaultPrefix(player);
        String displayFormat = getDisplayFormat();
        String fullRaw = displayFormat
                .replace("{prefix}", vaultPrefix != null ? vaultPrefix : "")
                .replace("{displayName}", player.getName())
                .replace("{nick}", player.getName());
        Component nametagComp = MessageUtil.parse(fullRaw);

        hideVanillaNameTag(player);
        createOrUpdateNickDisplay(player, nametagComp);
        updateTabName(player);
    }

    // ========================= Scoreboard Team (hide / show vanilla nametag) =========================

    /**
     * Hides the vanilla nametag completely: Team with NAME_TAG_VISIBILITY = NEVER.
     * Empty prefix/suffix so nothing team-related renders.
     * <p>
     * If TabModule is active, it manages teams for sorting + nametag visibility,
     * so NamesModule skips its own team management (TabRenderer handles it on next tick).
     */
    private void hideVanillaNameTag(Player player) {
        if (isTabModuleActive()) return; // TabRenderer handles teams + nametag visibility
        Team team = getOrCreateTeam(player);
        team.prefix(Component.empty());
        team.suffix(Component.empty());
        team.color(NamedTextColor.WHITE);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        ensureSingleEntry(team, player.getName());
    }

    /**
     * Restores the vanilla nametag: Team with NAME_TAG_VISIBILITY = ALWAYS.
     * Real name shows as-is.
     * <p>
     * If TabModule is active, it handles nametag visibility via its own teams.
     */
    private void showVanillaNameTag(Player player) {
        if (isTabModuleActive()) return; // TabRenderer handles teams + nametag visibility
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(player.getUniqueId());

        Team team = board.getTeam(teamName);
        if (team == null) return;

        team.prefix(Component.empty());
        team.suffix(Component.empty());
        team.color(NamedTextColor.WHITE);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        ensureSingleEntry(team, player.getName());
    }

    /** Returns true if TabModule is active (it manages teams for sort + nametag visibility). */
    private boolean isTabModuleActive() {
        if (!(plugin instanceof ManagerFix mf)) return false;
        return mf.getModuleManager().getEnabledModule("tab").isPresent();
    }

    private Team getOrCreateTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(player.getUniqueId());
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        return team;
    }

    private void ensureSingleEntry(Team team, String playerName) {
        if (team.hasEntry(playerName) && team.getEntries().size() == 1) return;
        for (String entry : List.copyOf(team.getEntries())) {
            if (!entry.equals(playerName)) {
                team.removeEntry(entry);
            }
        }
        if (!team.hasEntry(playerName)) {
            team.addEntry(playerName);
        }
    }

    public void cleanupTeam(UUID uuid) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(uuid);
        Team team = board.getTeam(teamName);
        if (team != null) team.unregister();
    }

    private String getTeamName(UUID uuid) {
        return "mf_" + uuid.toString().substring(0, 8);
    }

    private void updateTabName(Player player) {
        if (!(plugin instanceof ManagerFix mf)) return;
        var opt = mf.getModuleManager().getEnabledModule("tab");
        if (opt.isEmpty()) return;
        ru.managerfix.modules.tab.TabModule tab = (ru.managerfix.modules.tab.TabModule) opt.get();
        ru.managerfix.modules.tab.TabRenderer renderer = tab.getTabRenderer();
        if (renderer == null) return;
        renderer.invalidate(player);
        renderer.updatePlayerListName(player);
    }

    // ========================= TextDisplay nametag =========================

    /**
     * Creates or updates a TextDisplay entity riding the player that shows the nick.
     * - Billboard = CENTER (always faces the viewer, like a vanilla nametag)
     * - Transparent background
     * - Text shadow enabled
     * - Hidden from the player themselves via player.hideEntity()
     */
    private void createOrUpdateNickDisplay(Player player, Component nickComp) {
        // Если ники скрыты глобально - не создаём TextDisplay
        if (globalNickHidden) {
            removeNickDisplay(player.getUniqueId());
            return;
        }
        
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            removeNickDisplay(player.getUniqueId());
            return;
        }
        UUID playerUuid = player.getUniqueId();

        UUID existingUuid = nickDisplays.get(playerUuid);
        if (existingUuid != null) {
            Entity existing = Bukkit.getEntity(existingUuid);
            if (existing instanceof TextDisplay td && !td.isDead()) {
                td.text(nickComp);
                if (!player.getPassengers().contains(td)) {
                    player.addPassenger(td);
                }
                purgeNickPassengers(player, td.getUniqueId());
                return;
            }
            nickDisplays.remove(playerUuid);
        }

        purgeNickPassengers(player, null);
        TextDisplay display = (TextDisplay) player.getWorld().spawnEntity(
                player.getLocation(), EntityType.TEXT_DISPLAY);

        display.text(nickComp);

        display.setBillboard(Display.Billboard.CENTER);

        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

        display.setShadowed(true);

        display.setAlignment(TextDisplay.TextAlignment.CENTER);

        display.setPersistent(false);

        float offsetY = getNameTagOffsetY();
        display.setTransformation(new Transformation(
                new Vector3f(0, offsetY, 0), NO_ROTATION,
                UNIT_SCALE, NO_ROTATION
        ));

        display.addScoreboardTag("mf_nick");

        player.addPassenger(display);

        player.hideEntity(plugin, display);

        nickDisplays.put(playerUuid, display.getUniqueId());
        LoggerUtil.debug("[Names] Created TextDisplay nametag for " + player.getName());
    }

    /**
     * Removes the TextDisplay entity for the given player.
     */
    public void removeNickDisplay(UUID playerUuid) {
        UUID displayUuid = nickDisplays.remove(playerUuid);
        if (displayUuid == null) return;
        Entity entity = Bukkit.getEntity(displayUuid);
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    /**
     * Refreshes the text of the existing TextDisplay nametag for a player (e.g. when prefix changes).
     * Does NOT recreate the entity — only updates the text content.
     * Called periodically by TabRenderer to keep nametag in sync with current Vault/LuckPerms prefix.
     */
    public void refreshNametagText(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID displayUuid = nickDisplays.get(player.getUniqueId());
        if (displayUuid == null) return;
        Entity entity = Bukkit.getEntity(displayUuid);
        if (!(entity instanceof TextDisplay td) || td.isDead()) return;

        // Rebuild the nametag component with the current prefix
        if (!(plugin instanceof ManagerFix mf)) return;
        ProfileManager pm = mf.getProfileManager();
        Object nick = pm.getProfile(player).getMetadata(METADATA_NICK).orElse(null);
        if (!(nick instanceof String rawNick) || rawNick.isEmpty()) return;

        String vaultPrefix = getVaultPrefix(player);
        String displayFormat = getDisplayFormat();
        String fullRaw = displayFormat
                .replace("{prefix}", vaultPrefix != null ? vaultPrefix : "")
                .replace("{displayName}", rawNick)
                .replace("{nick}", rawNick);
        Component nametagComp = MessageUtil.parse(fullRaw);
        td.text(nametagComp);
    }

    /**
     * Re-mounts the TextDisplay if it was dismounted (e.g. after entering a vehicle).
     * Called from NamesListener.
     */
    public void remountNickDisplay(Player player) {
        UUID displayUuid = nickDisplays.get(player.getUniqueId());
        if (displayUuid == null) return;
        Entity entity = Bukkit.getEntity(displayUuid);
        if (entity instanceof TextDisplay td && !td.isDead()) {
            if (!player.getPassengers().contains(td)) {
                player.addPassenger(td);
            }
        }
    }

    private void purgeNickPassengers(Player player, UUID keepUuid) {
        java.util.List<Entity> passengers = new java.util.ArrayList<>(player.getPassengers());
        for (Entity e : passengers) {
            if (e instanceof TextDisplay td) {
                if (!td.getScoreboardTags().contains("mf_nick")) continue;
                if (keepUuid != null && td.getUniqueId().equals(keepUuid)) continue;
                try { player.removePassenger(td); } catch (Throwable ignored) {}
                try { td.remove(); } catch (Throwable ignored) {}
            }
        }
    }

    /** Returns true if this player currently has a TextDisplay nick. */
    public boolean hasNickDisplay(UUID playerUuid) {
        return nickDisplays.containsKey(playerUuid);
    }

    // ========================= Profile storage =========================

    /** Gets current display nick from profile (raw string) or null. */
    public String getStoredNick(Player player, ProfileManager profileManager) {
        if (player == null || profileManager == null) return null;
        Object v = profileManager.getProfile(player).getMetadata(METADATA_NICK).orElse(null);
        return v instanceof String s ? s : null;
    }

    /** Sets nick in profile and applies; fires PlayerNickChangeEvent. Call from main thread. */
    public void setNick(Player player, ProfileManager profileManager, String newNickRaw, Runnable onSaved) {
        setNick(player, profileManager, newNickRaw, null, onSaved);
    }

    /** Sets nick in profile and applies; fires PlayerNickChangeEvent with changedBy (null = self). */
    public void setNick(Player player, ProfileManager profileManager, String newNickRaw, UUID changedBy, Runnable onSaved) {
        if (player == null || profileManager == null) return;

        String oldNick = getStoredNick(player, profileManager);
        if (newNickRaw != null && !newNickRaw.isEmpty()) {
            profileManager.getProfile(player).setMetadata(METADATA_NICK, newNickRaw);
            applyNick(player, newNickRaw);
        } else {
            profileManager.getProfile(player).setMetadata(METADATA_NICK, null);
            applyDefaultDisplay(player);
        }

        if (plugin instanceof ManagerFix mf) {
            mf.getProfileManager().saveProfileAsync(player.getUniqueId());
            if (mf.getEventBus() != null) {
                mf.getEventBus().callEvent(new PlayerNickChangeEvent(
                        player.getUniqueId(), oldNick, newNickRaw, changedBy));
            }
        }

        if (onSaved != null) onSaved.run();
    }

    /** Sets nick for offline player (profile only, no apply). Fires event, saves profile. */
    public void setNickOffline(UUID targetUuid, ProfileManager profileManager, String newNickRaw,
                               UUID changedBy, Runnable onSaved) {
        if (targetUuid == null || profileManager == null) return;

        PlayerProfile profile = profileManager.getProfile(targetUuid);
        String oldNick = null;
        Object v = profile.getMetadata(METADATA_NICK).orElse(null);
        if (v instanceof String s) oldNick = s;

        if (newNickRaw != null && !newNickRaw.isEmpty()) {
            profile.setMetadata(METADATA_NICK, newNickRaw);
        } else {
            profile.setMetadata(METADATA_NICK, null);
        }

        if (plugin instanceof ManagerFix mf) {
            mf.getProfileManager().saveProfileAsync(targetUuid);
            if (mf.getEventBus() != null) {
                mf.getEventBus().callEvent(new PlayerNickChangeEvent(
                        targetUuid, oldNick, newNickRaw, changedBy));
            }
        }

        if (onSaved != null) onSaved.run();
    }

    // ========================= Pending admin nick =========================

    public void setPendingNickTarget(UUID adminUuid, UUID targetUuid) {
        if (targetUuid == null) pendingNickTarget.remove(adminUuid);
        else pendingNickTarget.put(adminUuid, targetUuid);
    }

    public UUID getPendingNickTarget(UUID adminUuid) {
        return pendingNickTarget.get(adminUuid);
    }

    public void clearPendingNickTarget(UUID adminUuid) {
        pendingNickTarget.remove(adminUuid);
    }

    public NamesListener getNamesListener() {
        return namesListener;
    }

    // ========================= Hide Nick API =========================

    /**
     * Check if player's nick is hidden.
     */
    public boolean isNickHidden(UUID playerUuid) {
        return hiddenNicks.getOrDefault(playerUuid, false);
    }

    /**
     * Set nick hidden state for player.
     */
    public void setNickHidden(UUID playerUuid, boolean hidden) {
        hiddenNicks.put(playerUuid, hidden);

        // Обновляем отображение для всех игроков
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            if (hidden) {
                // Скрываем ник
                hidePlayerNametag(player);
            } else {
                // Показываем ник
                showPlayerNametag(player);
            }
        }
    }

    /**
     * Check if global nick hide is enabled.
     */
    public boolean isGlobalNickHidden() {
        return globalNickHidden;
    }

    /**
     * Set global nick hide state for all players.
     */
    public void setGlobalNickHidden(boolean hidden) {
        globalNickHidden = hidden;
        
        // Применяем ко всем онлайн игрокам
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hidden) {
                hidePlayerNametag(player);
            } else {
                showPlayerNametag(player);
                // При показе - обновляем ники
                ProfileManager pm = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
                if (pm != null) {
                    pm.getCachedProfile(player.getUniqueId()).ifPresent(profile -> {
                        Object nick = profile.getMetadata(METADATA_NICK).orElse(null);
                        if (nick instanceof String s && !s.isEmpty()) {
                            applyNick(player, s);
                        } else {
                            applyDefaultDisplay(player);
                        }
                    });
                }
            }
        }
    }

    /**
     * Скрывает ник игрока через Scoreboard Team (полностью скрывает над головой).
     */
    public void hidePlayerNametag(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("NamesHidden");
        if (team == null) {
            team = scoreboard.registerNewTeam("NamesHidden");
            team.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.NEVER);
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        
        // Удаляем TextDisplay если есть
        removeNickDisplay(player.getUniqueId());
        
        // НЕ восстанавливаем vanilla nametag - он должен быть скрыт
    }

    /**
     * Показывает ник игрока через Scoreboard Team (восстанавливаем vanilla nametag).
     */
    public void showPlayerNametag(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("NamesHidden");
        if (team != null && team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
        }
        
        // Восстанавливаем vanilla nametag
        showVanillaNameTag(player);
    }

    /** Hide/show nametag when invisibility potion changes. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getModifiedType() != org.bukkit.potion.PotionEffectType.INVISIBILITY) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            boolean isInvisible = player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            if (isInvisible) {
                // Hide nametag by removing TextDisplay
                removeNickDisplay(player.getUniqueId());
            } else {
                // Show nametag by re-applying display
                ProfileManager pm = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
                if (pm != null) {
                    pm.getCachedProfile(player.getUniqueId()).ifPresent(profile -> {
                        Object nick = profile.getMetadata(METADATA_NICK).orElse(null);
                        if (nick instanceof String s && !s.isEmpty()) {
                            applyNick(player, s);
                        } else {
                            applyDefaultDisplay(player);
                        }
                    });
                }
            }
        });
    }
}
