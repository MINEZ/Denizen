package com.denizenscript.denizen.paper.properties;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.WorldTag;
import com.denizenscript.denizen.utilities.BukkitImplDeprecations;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.world.WorldClock;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.DragonBattle;

public class PaperWorldExtensions  {

    public static void register() {

        // <--[tag]
        // @attribute <WorldTag.no_tick_view_distance>
        // @returns ElementTag(Number)
        // @mechanism WorldTag.no_tick_view_distance
        // @group paper
        // @deprecated replaced by Minecraft's simulation_distance and view_distance config pairing
        // @Plugin Paper
        // @description
        // Deprecated: replaced by Minecraft's simulation_distance and view_distance config pairing
        // -->
        WorldTag.tagProcessor.registerTag(ElementTag.class, "no_tick_view_distance", (attribute, world) -> {
            BukkitImplDeprecations.paperNoTickViewDistance.warn(attribute.context);
            return new ElementTag(world.getWorld().getNoTickViewDistance());
        });

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_20)) {

            // <--[tag]
            // @attribute <WorldTag.gateway_count>
            // @returns ElementTag(Number)
            // @group paper
            // @Plugin Paper
            // @description
            // Returns the number of end gateway portals.
            // Only works if the world is an end world.
            // -->
            WorldTag.tagProcessor.registerTag(ElementTag.class, "gateway_count", (attribute, object) -> {
                DragonBattle battle = object.getWorld().getEnderDragonBattle();
                if (battle == null) {
                    attribute.echoError("Provided world is not an end world!");
                    return null;
                }
                return new ElementTag(battle.getGatewayCount());
            });

            // <--[tag]
            // @attribute <WorldTag.healing_crystals>
            // @returns ListTag(EntityTag)
            // @group paper
            // @Plugin Paper
            // @description
            // Returns a ListTag of the healing end crystals located on top of the obsidian towers.
            // Only works if the world is an end world.
            // -->
            WorldTag.tagProcessor.registerTag(ListTag.class, "healing_crystals", (attribute, object) -> {
                DragonBattle battle = object.getWorld().getEnderDragonBattle();
                if (battle == null) {
                    attribute.echoError("Provided world is not an end world!");
                    return null;
                }
                return new ListTag(battle.getHealingCrystals(), EntityTag::new);
            });

            // <--[tag]
            // @attribute <WorldTag.respawn_crystals>
            // @returns ListTag(EntityTag)
            // @group paper
            // @Plugin Paper
            // @description
            // Returns a ListTag of the respawn end crystals located at the end exit portal.
            // Only works if the world is an end world.
            // -->
            WorldTag.tagProcessor.registerTag(ListTag.class, "respawn_crystals", (attribute, object) -> {
                DragonBattle battle = object.getWorld().getEnderDragonBattle();
                if (battle == null) {
                    attribute.echoError("Provided world is not an end world!");
                    return null;
                }
                return new ListTag(battle.getRespawnCrystals(), EntityTag::new);
            });

            // <--[mechanism]
            // @object WorldTag
            // @name spawn_gateway
            // @input LocationTag
            // @group paper
            // @Plugin Paper
            // @description
            // Spawns a new end gateway portal at the specified location.
            // If no location is specified, tries to spawn a new end gateway using default game mechanics.
            // Only works if the world is an end world.
            // -->
            WorldTag.tagProcessor.registerMechanism("spawn_gateway", false, (object, mechanism) -> {
                DragonBattle battle = object.getWorld().getEnderDragonBattle();
                if (battle == null) {
                    mechanism.echoError("Cannot spawn gateway in non-end world!");
                    return;
                }
                if (!mechanism.hasValue()) {
                    battle.spawnNewGateway();
                }
                else if (mechanism.requireObject(LocationTag.class)) {
                    battle.spawnNewGateway(mechanism.valueAsType(LocationTag.class));
                }
            });
        }

        // <--[mechanism]
        // @object WorldTag
        // @name view_distance
        // @input ElementTag(Number)
        // @Plugin Paper
        // @group paper
        // @description
        // Sets this world's view distance. All chunks within this radius of a player will be visible to that player.
        // Input should be a number from 2 to 32.
        // See also <@link mechanism WorldTag.simulation_distance>
        // @tags
        // <WorldTag.view_distance>
        // <server.view_distance>
        // -->
        WorldTag.tagProcessor.registerMechanism("view_distance", false, ElementTag.class, (object, mechanism, input) -> {
            if (mechanism.requireInteger()) {
                int distance = input.asInt();
                if (distance < 2 || distance > 32) {
                    mechanism.echoError("View distance must be a number from 2 to 32!");
                }
                else {
                    object.getWorld().setViewDistance(distance);
                }
            }
        });

        // <--[mechanism]
        // @object WorldTag
        // @name simulation_distance
        // @input ElementTag(Number)
        // @Plugin Paper
        // @group paper
        // @description
        // Sets this world's view distance. All chunks within this radius will be tracked by the server.
        // Input should be a number from 2 to 32.
        // See also <@link mechanism WorldTag.view_distance>
        // @tags
        // <WorldTag.view_distance>
        // <server.view_distance>
        // -->
        WorldTag.tagProcessor.registerMechanism("simulation_distance", false, ElementTag.class, (object, mechanism, input) -> {
            if (mechanism.requireInteger()) {
                int distance = input.asInt();
                if (distance < 2 || distance > 32) {
                    mechanism.echoError("View distance must be a number from 2 to 32!");
                }
                else {
                    object.getWorld().setSimulationDistance(distance);
                }
            }
        });

        // <--[mechanism]
        // @object WorldTag
        // @name no_tick_view_distance
        // @input ElementTag(Number)
        // @Plugin Paper
        // @group paper
        // @deprecated replaced by Minecraft's simulation_distance and view_distance config pairing
        // @description
        // Deprecated: replaced by Minecraft's simulation_distance and view_distance config pairing
        // @tags
        // <WorldTag.no_tick_view_distance>
        // -->
        WorldTag.tagProcessor.registerMechanism("no_tick_view_distance", false, (object, mechanism) -> {
            BukkitImplDeprecations.paperNoTickViewDistance.warn(mechanism.context);
            if (!mechanism.hasValue()) {
                object.getWorld().setNoTickViewDistance(-1);
            }
            else if (mechanism.requireInteger()) {
                int distance = mechanism.getValue().asInt();
                if (distance < 2 || distance > 32) {
                    mechanism.echoError("View distance must be a number from 2 to 32!");
                }
                else {
                    object.getWorld().setNoTickViewDistance(distance);
                }
            }
        });

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v26_1)) {

            // <--[tag]
            // @attribute <WorldTag.clock_time[<clock>]>
            // @returns ElementTag(Number)
            // @mechanism WorldTag.clock
            // @group paper
            // @Plugin Paper
            // @description
            // Returns the total ticks of the specified world clock (for example "minecraft:overworld", or one defined by a datapack) in this world.
            // World clocks were added in Minecraft 26.1, see also the vanilla "/time of <clock>" command.
            // Returns null if no such clock exists (for example the datapack defining it is not loaded).
            // -->
            WorldTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "clock_time", (attribute, object, param) -> {
                WorldClock clock = getClock(param.asString());
                if (clock == null) {
                    attribute.echoError("Unknown world clock '" + param + "'.");
                    return null;
                }
                return new ElementTag(object.getWorld().getFullTime(clock));
            });

            // <--[tag]
            // @attribute <WorldTag.clock_paused[<clock>]>
            // @returns ElementTag(Boolean)
            // @mechanism WorldTag.clock
            // @group paper
            // @Plugin Paper
            // @description
            // Returns whether the specified world clock is paused in this world. See <@link tag WorldTag.clock_time>.
            // -->
            WorldTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "clock_paused", (attribute, object, param) -> {
                WorldClock clock = getClock(param.asString());
                if (clock == null) {
                    attribute.echoError("Unknown world clock '" + param + "'.");
                    return null;
                }
                return new ElementTag(object.getWorld().isClockPaused(clock));
            });

            // <--[tag]
            // @attribute <WorldTag.clock_rate[<clock>]>
            // @returns ElementTag(Decimal)
            // @mechanism WorldTag.clock
            // @group paper
            // @Plugin Paper
            // @description
            // Returns how many ticks the specified world clock advances per game tick in this world. See <@link tag WorldTag.clock_time>.
            // -->
            WorldTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "clock_rate", (attribute, object, param) -> {
                WorldClock clock = getClock(param.asString());
                if (clock == null) {
                    attribute.echoError("Unknown world clock '" + param + "'.");
                    return null;
                }
                return new ElementTag(object.getWorld().getClockRate(clock));
            });

            // <--[mechanism]
            // @object WorldTag
            // @name clock
            // @input MapTag
            // @group paper
            // @Plugin Paper
            // @description
            // Adjusts a world clock (see <@link tag WorldTag.clock_time>) in this world, like the vanilla "/time of <clock>" command.
            // The input map needs the key "clock", the ID of the clock (for example "minez:blood_moon"), and may have any of the following keys:
            // - "time", the total ticks to set the clock to.
            // - "paused", whether the clock is paused.
            // - "rate", how many ticks the clock advances per game tick.
            // For example: - adjust <world[world]> clock:[clock=minez:blood_moon;time=24000;paused=false]
            // @tags
            // <WorldTag.clock_time[<clock>]>
            // <WorldTag.clock_paused[<clock>]>
            // <WorldTag.clock_rate[<clock>]>
            // -->
            WorldTag.tagProcessor.registerMechanism("clock", false, MapTag.class, (object, mechanism, input) -> {
                ElementTag clockId = input.getElement("clock");
                if (clockId == null) {
                    mechanism.echoError("Invalid map input '" + input.debuggable() + "<W>' specified: must have a 'clock' key.");
                    return;
                }
                WorldClock clock = getClock(clockId.asString());
                if (clock == null) {
                    mechanism.echoError("Unknown world clock '" + clockId + "'.");
                    return;
                }
                ElementTag time = input.getElement("time");
                ElementTag paused = input.getElement("paused");
                ElementTag rate = input.getElement("rate");
                if (time != null && !time.isInt()) {
                    mechanism.echoError("Invalid time '" + time + "' specified: must be a whole number.");
                    return;
                }
                if (paused != null && !paused.isBoolean()) {
                    mechanism.echoError("Invalid paused '" + paused + "' specified: must be a boolean.");
                    return;
                }
                if (rate != null && !rate.isFloat()) {
                    mechanism.echoError("Invalid rate '" + rate + "' specified: must be a number.");
                    return;
                }
                if (rate != null) {
                    object.getWorld().setClockRate(clock, rate.asFloat());
                }
                if (time != null) {
                    object.getWorld().setFullTime(clock, time.asLong());
                }
                if (paused != null) {
                    object.getWorld().setClockPaused(clock, paused.asBoolean());
                }
            });
        }
    }

    // Gets a world clock (26.1+, from the registry, may be defined by datapacks) by ID, or null if there is none.
    public static WorldClock getClock(String id) {
        NamespacedKey key = NamespacedKey.fromString(CoreUtilities.toLowerCase(id));
        if (key == null) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.WORLD_CLOCK).get(key);
    }
}
