package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.utilities.BukkitImplDeprecations;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnsScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player respawns
    //
    // @Switch spawn_type:<type> to only process the event when the respawn point is of the specified type: bed, anchor, or world.
    // @Switch reason:<reason> to only process the event when the respawn happened for the specified reason: death, end_portal, or plugin.
    //
    // @Group Player
    //
    // @Triggers when a player respawns.
    //
    // @Context
    // <context.location> returns a LocationTag of the respawn location.
    // <context.spawn_type> returns the type of respawn point being used: bed, anchor, or world.
    // <context.reason> returns why the respawn happened: death, end_portal, or plugin.
    // <context.is_missing_respawn_block> returns a boolean indicating whether the player's bed or respawn anchor was gone, sending them to the world spawn instead. Requires Paper, and is always false otherwise.
    // <context.is_bed_spawn> returns a boolean indicating whether the player is about to respawn at their bed. Prefer <context.spawn_type>.
    //
    // @Determine
    // LocationTag to change the respawn location.
    //
    // @Player Always.
    //
    // @Example
    // # Announces where the player came back.
    // on player respawns:
    // - narrate "You respawned at your <context.spawn_type>."
    //
    // @Example
    // # Only runs when a player's bed or anchor was destroyed while they were away.
    // on player respawns spawn_type:world:
    // - if <context.is_missing_respawn_block>:
    //   - narrate "<&c>Your respawn point is gone."
    //
    // -->

    public PlayerRespawnsScriptEvent() {
        registerCouldMatcher("player respawns");
        registerSwitches("spawn_type", "reason");
        // 旧写法保留以便过渡，见 matches 中的废弃提示。
        registerCouldMatcher("player respawns (at bed)");
        registerCouldMatcher("player respawns elsewhere");
    }

    /** 重生点的类型，与 Paper 提供的 isBedSpawn / isAnchorSpawn 对应。 */
    public String getSpawnType() {
        if (event.isBedSpawn()) {
            return "bed";
        }
        if (event.isAnchorSpawn()) {
            return "anchor";
        }
        return "world";
    }

    public PlayerRespawnEvent event;

    @Override
    public boolean matches(ScriptPath path) {
        String loc = path.eventArgLowerAt(2);
        if (loc.equals("at") || loc.equals("elsewhere")) {
            BukkitImplDeprecations.playerRespawnsAtBedEvent.warn(path.container);
            if (loc.equals("at") && !event.isBedSpawn()) {
                return false;
            }
            if (loc.equals("elsewhere") && event.isBedSpawn()) {
                return false;
            }
        }
        if (!path.checkSwitch("spawn_type", getSpawnType())) {
            return false;
        }
        if (!path.checkSwitch("reason", CoreUtilities.toLowerCase(event.getRespawnReason().name()))) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String determination = determinationObj.toString();
        if (!CoreUtilities.equalsIgnoreCase(determination, "none")) {
            LocationTag loc = LocationTag.valueOf(determination, getTagContext(path));
            if (loc != null) {
                event.setRespawnLocation(loc);
                return true;
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(event.getPlayer());
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("location")) {
            return new LocationTag(event.getRespawnLocation());
        }
        else if (name.equals("is_bed_spawn")) {
            return new ElementTag(event.isBedSpawn());
        }
        else if (name.equals("spawn_type")) {
            return new ElementTag(getSpawnType());
        }
        else if (name.equals("reason")) {
            return new ElementTag(CoreUtilities.toLowerCase(event.getRespawnReason().name()));
        }
        else if (name.equals("is_missing_respawn_block")) {
            return new ElementTag(PaperAPITools.instance.isMissingRespawnBlock(event));
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onPlayerRespawns(PlayerRespawnEvent event) {
        if (EntityTag.isNPC(event.getPlayer())) {
            return;
        }
        this.event = event;
        fire(event);
    }
}
