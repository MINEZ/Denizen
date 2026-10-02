package com.denizenscript.denizen.paper.properties;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.EntityFormObject;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.paper.utilities.MatcherTargetGoal;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalType;
import io.papermc.paper.entity.Shearable;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.UUID;

public class PaperEntityExtensions {

    public static void register() {

        // <--[tag]
        // @attribute <EntityTag.spawn_reason>
        // @returns ElementTag
        // @group paper
        // @Plugin Paper
        // @description
        // Returns the entity's spawn reason.
        // Valid spawn reasons can be found at <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html>
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "spawn_reason", (attribute, entity) -> {
            return new ElementTag(entity.getBukkitEntity().getEntitySpawnReason());
        });

        // <--[tag]
        // @attribute <EntityTag.xp_spawn_reason>
        // @returns ElementTag
        // @group paper
        // @Plugin Paper
        // @description
        // If the entity is an experience orb, returns its spawn reason.
        // Valid spawn reasons can be found at <@link url https://papermc.io/javadocs/paper/1.17/org/bukkit/entity/ExperienceOrb.SpawnReason.html>
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "xp_spawn_reason", (attribute, entity) -> {
            if (!(entity.getBukkitEntity() instanceof ExperienceOrb experienceOrb)) {
                attribute.echoError("Entity " + entity + " is not an experience orb.");
                return null;
            }
            return new ElementTag(experienceOrb.getSpawnReason());
        });

        // <--[tag]
        // @attribute <EntityTag.xp_trigger>
        // @returns EntityTag
        // @group paper
        // @Plugin Paper
        // @description
        // If the entity is an experience orb, returns the entity that triggered it spawning (if any).
        // For example, if a player killed an entity this would return the player.
        // -->
        EntityTag.tagProcessor.registerTag(EntityFormObject.class, "xp_trigger", (attribute, entity) -> {
            if (!(entity.getBukkitEntity() instanceof ExperienceOrb experienceOrb)) {
                attribute.echoError("Entity " + entity + " is not an experience orb.");
                return null;
            }
            UUID uuid = experienceOrb.getTriggerEntityId();
            if (uuid == null) {
                return null;
            }
            Entity e = EntityTag.getEntityForID(uuid);
            if (e == null) {
                return null;
            }
            return new EntityTag(e).getDenizenObject();
        });

        // <--[tag]
        // @attribute <EntityTag.xp_source>
        // @returns EntityTag
        // @group paper
        // @Plugin Paper
        // @description
        // If the entity is an experience orb, returns the entity that it was created from (if any).
        // For example, if the xp orb was spawned from breeding this would return the baby.
        // -->
        EntityTag.registerSpawnedOnlyTag(EntityFormObject.class, "xp_source", (attribute, entity) -> {
            if (!(entity.getBukkitEntity() instanceof ExperienceOrb experienceOrb)) {
                attribute.echoError("Entity " + entity + " is not an experience orb.");
                return null;
            }
            UUID uuid = experienceOrb.getSourceEntityId();
            if (uuid == null) {
                return null;
            }
            Entity e = EntityTag.getEntityForID(uuid);
            if (e == null) {
                return null;
            }
            return new EntityTag(e).getDenizenObject();
        });

        // <--[tag]
        // @attribute <EntityTag.spawn_location>
        // @returns LocationTag
        // @group paper
        // @Plugin Paper
        // @description
        // Returns the initial spawn location of this entity.
        // -->
        EntityTag.tagProcessor.registerTag(LocationTag.class, "spawn_location", (attribute, entity) -> {
            Location loc = entity.getBukkitEntity().getOrigin();
            return loc != null ? new LocationTag(loc) : null;
        });

        // <--[tag]
        // @attribute <EntityTag.from_spawner>
        // @returns ElementTag(Boolean)
        // @group paper
        // @Plugin Paper
        // @description
        // Returns whether the entity was spawned from a spawner.
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "from_spawner", (attribute, entity) -> {
            return new ElementTag(entity.getBukkitEntity().fromMobSpawner());
        });

        // <--[mechanism]
        // @object EntityTag
        // @name goat_ram
        // @input EntityTag
        // @Plugin Paper
        // @group paper
        // @description
        // Causes a goat to ram the specified entity.
        // -->
        EntityTag.registerSpawnedOnlyMechanism("goat_ram", false, EntityTag.class, (object, mechanism, input) -> {
            if (object.getBukkitEntity() instanceof Goat goat) {
                goat.ram(input.getLivingEntity());
            }
        });

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_19)) {

            // <--[tag]
            // @attribute <EntityTag.collides_at[<location>]>
            // @returns ElementTag(Boolean)
            // @group paper
            // @Plugin Paper
            // @description
            // Returns whether the entity's bounding box would collide if the entity was moved to the given location.
            // This checks for any colliding entities (like boats and shulkers), the world border and regular blocks.
            // (Note that this won't load chunks at the location.)
            // -->
            EntityTag.tagProcessor.registerTag(ElementTag.class, LocationTag.class, "collides_at", (attribute, entity, location) -> {
                return new ElementTag(entity.getBukkitEntity().collidesAt(location));
            });

            // <--[mechanism]
            // @object EntityTag
            // @name damage_item
            // @input MapTag
            // @Plugin Paper
            // @group paper
            // @description
            // Damages the given equipment slot for the given amount.
            // This runs all vanilla logic associated with damaging an item like gamemode and enchantment checks, events, stat changes, advancement triggers, and notifying clients to play break animations.
            // Input is a map with "slot" as a valid equipment slot, and "amount" as the damage amount to be dealt.
            // Valid equipment slot values can be found at <@link url https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/EquipmentSlot.html>.
            //
            // @example
            // # Damages your precious boots! :(
            // - adjust <player> damage_item:[slot=feet;amount=45]
            // -->
            EntityTag.registerSpawnedOnlyMechanism("damage_item", false, MapTag.class, (object, mechanism, input) -> {
                ElementTag slot = input.getElement("slot");
                ElementTag amount = input.getElement("amount");
                if (slot == null || !slot.matchesEnum(EquipmentSlot.class)) {
                    mechanism.echoError("Must specify a valid equipment slot to damage.");
                    return;
                }
                if (amount == null || !amount.isInt()) {
                    mechanism.echoError("Must specify a valid amount to damage this item for.");
                    return;
                }
                object.getLivingEntity().damageItemStack(slot.asEnum(EquipmentSlot.class), amount.asInt());
            });

            if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_21)) {

                // <--[mechanism]
                // @object EntityTag
                // @name restock_trades
                // @input None
                // @Plugin Paper
                // @group paper
                // @description
                // Restocks a villager's trades.
                // Note: this mechanism will fire the <@link event villager replenishes trade> event for every trade the villager is offering.
                // This mechanism also updates the villager's demand for offers, which may cause item trade prices to rise or fall.
                // -->
                EntityTag.registerSpawnedOnlyMechanism("restock_trades", false, (object, mechanism) -> {
                    if (object.getBukkitEntity() instanceof Villager villager) {
                        villager.restock();
                    }
                });
            }

            // <--[mechanism]
            // @object EntityTag
            // @name shear
            // @input None
            // @Plugin paper
            // @group paper
            // @description
            // Shears entities in the same way as a player can do using shears, including drops.
            // If the entity is not ready to be sheared, there will be no drops but the sound will still play.
            //
            // This mech will:
            // - Shear a sheep
            // - harvest a bogged
            // - harvest a mushroom cow (note: entity data will be lost as Minecraft will remove the entity and spawn an entirely new cow instead)
            // - derp a snowman (i.e. remove the pumpkin)
            //
            // Optionally, specify a sound source to change the source of the sound.
            // Valid sound sources can be found here: <@link url https://jd.advntr.dev/api/latest/net/kyori/adventure/sound/Sound.Source.html>.
            //
            // @example
            // # Shears the entity you're looking at.
            // - adjust <player.target> shear
            //
            // -->
            EntityTag.registerSpawnedOnlyMechanism("shear", false, (object, mechanism) -> {
                if (!(object.getBukkitEntity() instanceof Shearable shearable)) {
                    return;
                }
                if (!mechanism.hasValue()) {
                    shearable.shear();
                    return;
                }
                ElementTag input = mechanism.getValue();
                if (!mechanism.requireEnum(Sound.Source.class)) {
                    mechanism.echoError("Invalid sound source specified: " + input);
                    return;
                }
                Sound.Source source = input.asEnum(Sound.Source.class);
                shearable.shear(source);
            });
        }

        // <--[mechanism]
        // @object EntityTag
        // @name add_target_goal
        // @input MapTag
        // @Plugin Paper
        // @group paper
        // @description
        // Gives a mob a target-finding AI goal: the mob will look for nearby living entities that match a given matcher and attack the nearest one,
        // in the same way that vanilla hostile mobs look for players.
        // This makes the mob natively hostile to those entities - it finds, chases, attacks, and forgets them through its normal AI, with no need for a script to keep assigning targets.
        //
        // Input is a map with:
        // "matcher": required, an entity matcher (as in <@link objecttype EntityTag>) for the entities to target, such as "mannequin" or "entity_flagged:my_flag".
        // "id": optional, a name for this goal, made of lowercase letters, digits, underscores, hyphens and dots. Defaults to "default". Adding a goal with an ID the mob already has replaces the old one.
        // "priority": optional, the priority of the goal among the mob's target goals. Defaults to 3.
        // A lower number is a higher priority: a goal can interrupt running goals with a higher number, and cannot start while a goal with the same or a lower number is running.
        // For reference, most vanilla hostile mobs look for players at priority 2 and retaliate against attackers at priority 1, so the default of 3 makes the mob prefer real players.
        // "range": optional, how far away (in blocks) the mob can find and keep a target. Defaults to 16.
        // "must_see": optional, whether the mob needs line of sight to find a target, and gives up after about 3 seconds without it. Defaults to true.
        //
        // The mob never targets invulnerable entities or players in creative or spectator mode, and leaves alone a target that a script or another plugin has set directly (such as via <@link command attack>).
        // This only works for mobs that pick targets through AI goals, like zombies, skeletons, spiders, and creepers.
        // Mobs driven by a brain instead, like piglins, hoglins, and wardens, are not affected.
        //
        // Goals are not saved with the entity: they are lost when the entity is unloaded, so they should be added whenever it enters the world, as in the example.
        //
        // @tags
        // <EntityTag.target_goals>
        //
        // @example
        // # Makes every monster natively hostile to mannequins flagged 'offline_body'.
        // on monster added to world:
        // - adjust <context.entity> add_target_goal:[matcher=entity_flagged:offline_body]
        //
        // @example
        // # Makes the zombie prefer mannequins over players, at up to 24 blocks, even through walls.
        // - adjust <[zombie]> add_target_goal:[id=mannequins;matcher=mannequin;priority=1;range=24;must_see=false]
        // -->
        EntityTag.registerSpawnedOnlyMechanism("add_target_goal", false, MapTag.class, (object, mechanism, input) -> {
            if (!(object.getBukkitEntity() instanceof Mob mob)) {
                mechanism.echoError("Cannot add a target goal to " + object + ": only mobs have AI goals.");
                return;
            }
            ElementTag matcher = input.getElement("matcher");
            if (matcher == null || matcher.asString().isEmpty()) {
                mechanism.echoError("Must specify a 'matcher' for the entities to target.");
                return;
            }
            String id = CoreUtilities.toLowerCase(input.getElement("id", "default").asString());
            if (!isValidTargetGoalId(id)) {
                mechanism.echoError("Invalid target goal ID '" + id + "': must be made of lowercase letters, digits, underscores, hyphens and dots.");
                return;
            }
            ElementTag priority = input.getElement("priority", "3");
            if (!priority.isInt() || priority.asInt() < 0) {
                mechanism.echoError("Invalid target goal priority '" + priority + "': must be a non-negative integer.");
                return;
            }
            ElementTag range = input.getElement("range", "16");
            if (!range.isDouble() || range.asDouble() <= 0) {
                mechanism.echoError("Invalid target goal range '" + range + "': must be a positive number.");
                return;
            }
            ElementTag mustSee = input.getElement("must_see", "true");
            if (!mustSee.isBoolean()) {
                mechanism.echoError("Invalid target goal must_see value '" + mustSee + "': must be a boolean.");
                return;
            }
            removeTargetGoals(mob, id);
            Bukkit.getMobGoals().addGoal(mob, priority.asInt(), new MatcherTargetGoal(mob, id, matcher.asString(), priority.asInt(), range.asDouble(), mustSee.asBoolean()));
        });

        // <--[mechanism]
        // @object EntityTag
        // @name remove_target_goal
        // @input ElementTag
        // @Plugin Paper
        // @group paper
        // @description
        // Removes the target goal with the given ID that was added to a mob by <@link mechanism EntityTag.add_target_goal>, or all of them if no ID is given.
        // If the mob is chasing a target found by a removed goal, it forgets that target.
        // Vanilla goals are never removed by this.
        //
        // @tags
        // <EntityTag.target_goals>
        //
        // @example
        // # Removes the target goal named 'mannequins' from the zombie.
        // - adjust <[zombie]> remove_target_goal:mannequins
        //
        // @example
        // # Removes every target goal that scripts have added to the zombie.
        // - adjust <[zombie]> remove_target_goal
        // -->
        EntityTag.registerSpawnedOnlyMechanism("remove_target_goal", false, (object, mechanism) -> {
            if (!(object.getBukkitEntity() instanceof Mob mob)) {
                mechanism.echoError("Cannot remove a target goal from " + object + ": only mobs have AI goals.");
                return;
            }
            removeTargetGoals(mob, mechanism.hasValue() ? CoreUtilities.toLowerCase(mechanism.getValue().asString()) : null);
        });

        // <--[tag]
        // @attribute <EntityTag.target_goals>
        // @returns MapTag
        // @mechanism EntityTag.add_target_goal
        // @group paper
        // @Plugin Paper
        // @description
        // Returns the target goals that were added to a mob by <@link mechanism EntityTag.add_target_goal>, as a map of goal ID to a map of its settings:
        // "matcher", "priority", "range" and "must_see", plus "target" (the entity being chased) while the goal is the one driving the mob's current target.
        // Vanilla goals are not included. Returns an empty map for a mob with no such goals.
        // -->
        EntityTag.registerSpawnedOnlyTag(MapTag.class, "target_goals", (attribute, object) -> {
            if (!(object.getBukkitEntity() instanceof Mob mob)) {
                attribute.echoError("Entity " + object + " is not a mob.");
                return null;
            }
            MapTag result = new MapTag();
            for (Goal<Mob> goal : Bukkit.getMobGoals().getAllGoals(mob, GoalType.TARGET)) {
                if (!(goal instanceof MatcherTargetGoal targetGoal)) {
                    continue;
                }
                MapTag settings = new MapTag();
                settings.putObject("matcher", new ElementTag(targetGoal.matcher, true));
                settings.putObject("priority", new ElementTag(targetGoal.priority));
                settings.putObject("range", new ElementTag(targetGoal.range));
                settings.putObject("must_see", new ElementTag(targetGoal.mustSee));
                if (targetGoal.getTarget() != null) {
                    settings.putObject("target", new EntityTag(targetGoal.getTarget()).getDenizenObject());
                }
                result.putObject(targetGoal.id, settings);
            }
            return result;
        });
    }

    /** 索敌目标的 ID 会成为 NamespacedKey 的一部分，故只接受其允许的字符。 */
    public static boolean isValidTargetGoalId(String id) {
        if (id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    /** 移除生物身上由脚本添加、ID 相符的索敌目标；ID 为 null 时移除全部。原版目标不受影响。 */
    public static void removeTargetGoals(Mob mob, String id) {
        for (Goal<Mob> goal : new ArrayList<>(Bukkit.getMobGoals().getAllGoals(mob, GoalType.TARGET))) {
            if (goal instanceof MatcherTargetGoal targetGoal && (id == null || targetGoal.id.equals(id))) {
                Bukkit.getMobGoals().removeGoal(mob, goal);
            }
        }
    }
}
