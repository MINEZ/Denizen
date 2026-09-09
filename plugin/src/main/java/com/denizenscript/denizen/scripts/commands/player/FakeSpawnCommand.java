package com.denizenscript.denizen.scripts.commands.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.command.TabCompleteHelper;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.entity.FakeEntity;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FakeSpawnCommand extends AbstractCommand {

    public FakeSpawnCommand() {
        setName("fakespawn");
        setSyntax("fakespawn [<entity>] [<location>/cancel] (players:<player>|...) (duration:<duration>{10s}) (mount_to:<entity>)");
        setRequiredArguments(2, 5);
        isProcedural = false;
        addRemappedPrefixes("duration", "d");
        addRemappedPrefixes("players", "to");
        autoCompile();
    }

    // <--[command]
    // @Name FakeSpawn
    // @Syntax fakespawn [<entity>] [<location>/cancel] (players:<player>|...) (duration:<duration>{10s}) (mount_to:<entity>)
    // @Required 2
    // @Maximum 5
    // @Short Makes players see a fake entity spawn that didn't actually happen.
    // @Group player
    //
    // @Description
    // Makes players see a fake entity spawn that didn't actually happen.
    // This means that the server will not track the entity, and players not shown the entity will not see it.
    //
    // You must specify an entity to spawn and a location to spawn it at, or to remove a fake entity, specify the fake entity object and 'cancel' instead of a location.
    //
    // Optionally, specify a list of players to show the entity to.
    // If unspecified, the entity will be shown to every player in the world it is spawned in, including any player that joins the server or enters that world while it is still around.
    //
    // Optionally, specify how long the fake entity should remain for. If unspecified, will default to 10 seconds.
    // After the duration is up, the entity will be removed from the player(s).
    //
    // Optionally, specify an entity to mount the fake entity to via mount_to:<entity>.
    //
    // @Tags
    // <PlayerTag.fake_entities>
    // <entry[saveName].faked_entity> returns the spawned faked entity.
    //
    // @Usage
    // Use to show a fake creeper in front of the linked player, to that player alone.
    // - fakespawn creeper <player.location.forward[5]> players:<player>
    //
    // @Usage
    // Use to show a fake creeper to everyone in the world it is spawned in.
    // - fakespawn creeper <player.location.forward[5]>
    //
    // @Usage
    // Use to spawn and mount a fake armor stand to the player.
    // - fakespawn armor_stand <player.location> mount_to:<player>
    //
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        TabCompleteHelper.tabCompleteEntityTypes(tab);
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("entity") @ArgLinear ObjectTag entityObj,
                                   @ArgName("location") @ArgLinear @ArgDefaultNull ObjectTag locationObj,
                                   @ArgName("cancel") boolean cancel,
                                   @ArgName("players") @ArgPrefixed @ArgDefaultNull @ArgSubType(PlayerTag.class) List<PlayerTag> players,
                                   @ArgName("duration") @ArgPrefixed @ArgDefaultText("10s") DurationTag duration,
                                   @ArgName("mount_to") @ArgPrefixed @ArgDefaultNull EntityTag vehicle) {
        if (locationObj != null && entityObj.identify().startsWith("l@")) { // Compensate for legacy entity/location out-of-order support
            ObjectTag swap = locationObj;
            locationObj = entityObj;
            entityObj = swap;
            Deprecations.outOfOrderArgs.warn(scriptEntry);
        }
        LocationTag location = locationObj == null ? null : locationObj.asType(LocationTag.class, scriptEntry.context);
        EntityTag entity = entityObj.asType(EntityTag.class, scriptEntry.context);
        if (entity == null) {
            throw new InvalidArgumentsRuntimeException("Must specify a valid entity!");
        }
        if (location == null && !cancel) {
            throw new InvalidArgumentsRuntimeException("Must specify a valid location!");
        }
        if (vehicle != null && !vehicle.isValid()) {
            throw new InvalidArgumentsRuntimeException("Must specify a valid entity to mount to!");
        }
        boolean forAllPlayers = players == null;
        if (forAllPlayers) {
            // 未指定观看者时展示给假实体所在世界的全部玩家，其后加入或进入该世界的玩家由 FakeEntity 补发。
            players = new ArrayList<>();
            if (location != null && location.getWorld() != null) {
                for (Player onlinePlayer : location.getWorld().getPlayers()) {
                    players.add(new PlayerTag(onlinePlayer));
                }
            }
        }
        if (!cancel) {
            FakeEntity created = FakeEntity.showFakeEntityTo(players, entity, location, duration, vehicle);
            created.refreshOnJoin = true;
            created.forAllPlayers = forAllPlayers;
            scriptEntry.saveObject("faked_entity", created.entity);
            return;
        }
        if (entity.isFake) {
            FakeEntity fakeEnt = FakeEntity.idsToEntities.get(entity.getUUID());
            if (fakeEnt != null) {
                fakeEnt.cancelEntity();
            }
            else {
                Debug.echoDebug(scriptEntry, "Entity '" + entity + "' cannot be cancelled: not listed in fake-entity map.");
            }
        }
    }
}
