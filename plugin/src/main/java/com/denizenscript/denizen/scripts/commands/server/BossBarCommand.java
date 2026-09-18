package com.denizenscript.denizen.scripts.commands.server;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class BossBarCommand extends AbstractCommand {

    public BossBarCommand() {
        setName("bossbar");
        setSyntax("bossbar ({auto}/create/update/remove) [<id>] (players:<player>|...) (title:<title>) (progress:<#.#>) (color:<color>) (style:<style>) (options:<option>|...) (uuid:<uuid>)");
        setRequiredArguments(1, 9);
        isProcedural = false;
        autoCompile();
        addRemappedPrefixes("title", "t");
        addRemappedPrefixes("progress", "health", "p", "h");
        addRemappedPrefixes("style", "s");
        addRemappedPrefixes("options", "option", "opt", "o", "flags", "flag", "f");
    }

    // <--[command]
    // @Name BossBar
    // @Syntax bossbar ({auto}/create/update/remove) [<id>] (players:<player>|...) (title:<title>) (progress:<#.#>) (color:<color>) (style:<style>) (options:<option>|...) (uuid:<uuid>)
    // @Required 1
    // @Maximum 9
    // @Short Shows players a boss bar.
    // @Group server
    //
    // @Description
    // Displays a boss bar at the top of the screen of the specified player(s).
    // You can also update the values and remove the bar.
    //
    // You can CREATE a new bossbar, UPDATE an existing one, or REMOVE an existing one.
    // The default 'auto' will either 'create' or 'update' depending on whether it already exists.
    //
    // Requires an ID.
    //
    // Optionally, specify a list of players to show the bar to.
    // If unspecified when creating a bar, it is shown to every player on the server, including any player
    // that joins while it is still around.
    //
    // Viewers keep the bar across a relog: a player that is shown a bar and then disconnects is shown it
    // again when they rejoin, for as long as the bar is around.
    //
    // Note that using 'remove' with a 'players' list on a server-wide bar narrows the bar down to the
    // players that can see it at that moment, so it stops following new joins.
    //
    // Progress must be between 0 and 1.
    //
    // Valid colors: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW.
    // Valid styles: SEGMENTED_10, SEGMENTED_12, SEGMENTED_20, SEGMENTED_6, SOLID.
    // Valid options: CREATE_FOG, DARKEN_SKY, PLAY_BOSS_MUSIC.
    //
    // The UUID can optionally be specified, and will be sent to the client. Be careful to not overlap multiple bars with the same UUID.
    // If not specified, it will be random.
    //
    // @Tags
    // <server.current_bossbars>
    // <server.bossbar_viewers[<bossbar_id>]>
    // <PlayerTag.bossbar_ids>
    // <entry[saveName].bar_uuid> returns the bossbar's UUID.
    //
    // @Usage
    // Shows a message to every player on the server, including those that join later.
    // - bossbar MyMessageID "title:HI GUYS" color:red
    //
    // @Usage
    // Shows a message to the linked player alone.
    // - bossbar MyMessageID players:<player> "title:HI" color:red
    //
    // @Usage
    // Update the boss bar's color and progress.
    // - bossbar update MyMessageID color:blue progress:0.2
    //
    // @Usage
    // Add more players to the boss bar.
    // - bossbar update MyMessageID players:<server.flag[new_players]>
    //
    // @Usage
    // Remove a player from the boss bar.
    // - bossbar remove MyMessageID players:<[player]>
    //
    // @Usage
    // Delete the boss bar.
    // - bossbar remove MyMessageID
    // -->

    public enum Action {
        AUTO, CREATE, UPDATE, REMOVE
    }

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("id:", bossBarMap.keySet());
        tab.addWithPrefix("style:", BarStyle.values());
        tab.addWithPrefix("color:", BarColor.values());
        tab.addWithPrefix("options:", BarFlag.values());
    }

    public final static Map<String, BossBar> bossBarMap = new HashMap<>();

    /**
     * 每个 bossbar 的观看者名册。
     * <p>
     * Bukkit 的 BossBar 按玩家实例记录观看者，玩家退出后该实例即作废，重进时换成另一个实例，
     * 故须另存一份按 UUID 记录的名册，据此在重进后补发。
     */
    public final static Map<String, BossBarViewers> viewerMap = new HashMap<>();

    public static class BossBarViewers {

        /** 已登记的观看者，无论其当下是否在线。 */
        public final Set<UUID> players = new HashSet<>();

        /** 是否展示给全服玩家，包括其后才加入的玩家。 */
        public boolean forAllPlayers;
    }

    public static boolean refreshListenersRegistered = false;

    /** 首次创建 bossbar 时才注册监听器，未用到该命令的服务器不必承担其开销。 */
    public static void enableRefreshListeners() {
        if (refreshListenersRegistered) {
            return;
        }
        refreshListenersRegistered = true;
        Bukkit.getPluginManager().registerEvents(new RefreshListener(), Denizen.getInstance());
    }

    public static class RefreshListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            for (Map.Entry<String, BossBar> entry : bossBarMap.entrySet()) {
                BossBarViewers viewers = viewerMap.get(entry.getKey());
                if (viewers == null || (!viewers.forAllPlayers && !viewers.players.contains(player.getUniqueId()))) {
                    continue;
                }
                viewers.players.add(player.getUniqueId());
                entry.getValue().addPlayer(player);
            }
        }

        /** 退出时撤下 Bukkit 侧的观看者，其玩家实例已然作废，留着只会拖住不放。名册不动，重进后据此补发。 */
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            for (BossBar bossBar : bossBarMap.values()) {
                bossBar.removePlayer(event.getPlayer());
            }
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("action") @ArgDefaultText("auto") Action action,
                                   @ArgName("id") @ArgLinear ElementTag id,
                                   @ArgName("players") @ArgPrefixed @ArgDefaultNull @ArgSubType(PlayerTag.class) List<PlayerTag> players,
                                   @ArgName("title") @ArgPrefixed @ArgDefaultNull String title,
                                   @ArgName("progress") @ArgPrefixed @ArgDefaultNull ElementTag progress,
                                   @ArgName("color") @ArgPrefixed @ArgDefaultText("white") BarColor color,
                                   @ArgName("style") @ArgPrefixed @ArgDefaultText("solid") BarStyle style,
                                   @ArgName("options") @ArgPrefixed @ArgDefaultNull @ArgSubType(BarFlag.class) List<BarFlag> options,
                                   @ArgName("uuid") @ArgPrefixed @ArgDefaultNull String uuid) {
        String idString = id.asLowerString();
        if (action == Action.AUTO) {
            action = bossBarMap.containsKey(idString) ? Action.UPDATE : Action.CREATE;
        }
        boolean forAllPlayers = players == null && action == Action.CREATE;
        if (forAllPlayers) {
            // 未指定观看者时展示给全服玩家，其后加入的玩家由 RefreshListener 补发。
            players = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                players.add(new PlayerTag(onlinePlayer));
            }
        }
        BossBar bossBar = null;
        switch (action) {
            case CREATE: {
                if (bossBarMap.containsKey(idString)) {
                    Debug.echoError("BossBar '" + idString + "' already exists!");
                    return;
                }
                List<PlayerTag> barPlayers = players;
                double barProgress = progress != null ? progress.asDouble() : 1D;
                if (title == null) {
                    title = "";
                }
                bossBar = Bukkit.createBossBar(title, color, style, options == null ? new BarFlag[0] : options.toArray(new BarFlag[0]));
                NMSHandler.playerHelper.setBossBarTitle(bossBar, title);
                bossBar.setProgress(barProgress);
                if (uuid != null) {
                    NMSHandler.instance.setBossbarUUID(bossBar, UUID.fromString(uuid));
                }
                BossBarViewers viewers = new BossBarViewers();
                viewers.forAllPlayers = forAllPlayers;
                for (PlayerTag player : barPlayers) {
                    if (!player.isOnline()) {
                        Debug.echoError("Player must be online to show a BossBar to them!");
                        continue;
                    }
                    bossBar.addPlayer(player.getPlayerEntity());
                    viewers.players.add(player.getUUID());
                }
                bossBar.setVisible(true);
                bossBarMap.put(idString, bossBar);
                viewerMap.put(idString, viewers);
                enableRefreshListeners();
                break;
            }
            case UPDATE: {
                if (!bossBarMap.containsKey(idString)) {
                    Debug.echoError("BossBar '" + idString + "' does not exist!");
                    return;
                }
                bossBar = bossBarMap.get(idString);
                if (title != null) {
                    NMSHandler.playerHelper.setBossBarTitle(bossBar, title);
                }
                if (progress != null) {
                    bossBar.setProgress(progress.asDouble());
                }
                if (color != null) {
                    bossBar.setColor(color);
                }
                if (style != null) {
                    bossBar.setStyle(style);
                }
                if (options != null) {
                    HashSet<BarFlag> oldFlags = new HashSet<>(Arrays.asList(BarFlag.values()));
                    HashSet<BarFlag> newFlags = new HashSet<>(options.size());
                    for (BarFlag flag : options) {
                        newFlags.add(flag);
                        oldFlags.remove(flag);
                    }
                    for (BarFlag flag : oldFlags) {
                        bossBar.removeFlag(flag);
                    }
                    for (BarFlag flag : newFlags) {
                        bossBar.addFlag(flag);
                    }
                }
                if (players != null) {
                    BossBarViewers viewers = viewerMap.get(idString);
                    for (PlayerTag player : players) {
                        if (!player.isOnline()) {
                            Debug.echoError("Player must be online to show a BossBar to them!");
                            continue;
                        }
                        bossBar.addPlayer(player.getPlayerEntity());
                        if (viewers != null) {
                            viewers.players.add(player.getUUID());
                        }
                    }
                }
                break;
            }
            case REMOVE: {
                bossBar = bossBarMap.get(idString);
                if (bossBar == null) {
                    Debug.echoError("BossBar '" + idString + "' does not exist!");
                    return;
                }
                if (players != null) {
                    BossBarViewers viewers = viewerMap.get(idString);
                    if (viewers != null && viewers.forAllPlayers) {
                        // 全服可见的 bossbar 一旦要剔除个别玩家，就只能落到当下这批观看者上，不再跟随其后加入的玩家。
                        viewers.forAllPlayers = false;
                        for (Player viewer : bossBar.getPlayers()) {
                            viewers.players.add(viewer.getUniqueId());
                        }
                    }
                    for (PlayerTag player : players) {
                        if (viewers != null) {
                            viewers.players.remove(player.getUUID());
                        }
                        if (player.isOnline()) {
                            bossBar.removePlayer(player.getPlayerEntity());
                        }
                    }
                    break;
                }
                bossBar.setVisible(false);
                bossBarMap.remove(idString);
                viewerMap.remove(idString);
                break;
            }
        }
        if (bossBar != null) {
            UUID actualUuid = NMSHandler.instance.getBossbarUUID(bossBar);
            if (actualUuid != null) {
                scriptEntry.saveObject("bar_uuid", new ElementTag(actualUuid.toString()));
            }
        }
    }
}
