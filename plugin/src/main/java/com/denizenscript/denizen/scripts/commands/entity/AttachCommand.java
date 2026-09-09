package com.denizenscript.denizen.scripts.commands.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.entity.EntityAttachmentHelper;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class AttachCommand extends AbstractCommand {

    public AttachCommand() {
        setName("attach");
        setSyntax("attach [<entity>|...] [to:<entity>/cancel] (offset:<offset>) (relative) (yaw_offset:<#.#>) (pitch_offset:<#.#>) (sync_server) (no_rotate/no_pitch) (for:<player>|...)");
        setRequiredArguments(2, 9);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name attach
    // @Syntax attach [<entity>|...] [to:<entity>/cancel] (offset:<offset>) (relative) (yaw_offset:<#.#>) (pitch_offset:<#.#>) (sync_server) (no_rotate/no_pitch) (for:<player>|...)
    // @Required 2
    // @Maximum 9
    // @Short Attaches a list of entities to another entity, for client-visible motion sync.
    // @Group entity
    //
    // @Description
    // Attaches a list of entities to another entity, for client-visible motion sync.
    //
    // You must specify the entity or list of entities to be attached.
    // You must specify the entity that they will be attached to, or 'cancel' to end attachment.
    //
    // Optionally, specify an offset location vector to be a positional offset. This can include a yaw/pitch to offset those as well.
    // Note that setting an offset of 0,0,0 will produce slightly different visual results from not setting any offset.
    //
    // Optionally, specify 'relative' to indicate that the offset vector should rotate with the target entity.
    // If relative is used, optionally specify yaw_offset and/or pitch_offset to add an offset to rotation of the target entity when calculating the attachment offset.
    //
    // Optionally, specify 'for' with a player or list of players to only sync motion for those players.
    // If unspecified, will sync for everyone.
    //
    // Optionally, specify 'sync_server' to keep the serverside position of the attached entities near the target entity.
    // This can reduce some visual artifacts (such as entity unloading at distance), but may produce unintended functional artifacts.
    // Note that you should generally only use 'sync_server' when you exclude the 'for' argument.
    // Fake entities from <@link command fakespawn> always sync serverside, whether or not 'sync_server' is given.
    // The server never tracks them, so their movement is only ever sent by Denizen itself, and without that sync an attachment would not be visible to the target entity's own player.
    //
    // Optionally specify 'no_rotate' to retain the attached entity's own rotation and ignore the target rotation.
    // Optionally instead specify 'no_pitch' to retain the attached entity's own pitch, but use the target yaw.
    //
    // Note that attaches involving a player will not be properly visible to that player, but will still be visible to *other* players.
    // This does not apply to fake entities, which are visible to everyone the fake entity was shown to.
    //
    // It may be ideal to change setting "Packets.Auto init" in the Denizen config to "true" to guarantee this command functions as expected.
    //
    // @Tags
    // <EntityTag.attached_entities[(<player>)]>
    // <EntityTag.attached_to[(<player>)]>
    // <EntityTag.attached_offset[(<player>)]>
    //
    // @Usage
    // Use to attach random NPC to the air 3 blocks above a linked NPC.
    // - attach <server.list_npcs.random> to:<npc> offset:0,3,0
    // -->

    public static void autoExecute(@ArgName("entities") @ArgLinear @ArgSubType(EntityTag.class) List<EntityTag> entities,
                                   @ArgName("to") @ArgPrefixed @ArgDefaultNull EntityTag target,
                                   @ArgName("cancel") boolean cancel,
                                   @ArgName("offset") @ArgPrefixed @ArgDefaultNull LocationTag offset,
                                   @ArgName("relative") boolean relative,
                                   @ArgName("yaw_offset") @ArgPrefixed @ArgDefaultText("0") float yawOffset,
                                   @ArgName("pitch_offset") @ArgPrefixed @ArgDefaultText("0") float pitchOffset,
                                   @ArgName("sync_server") boolean syncServer,
                                   @ArgName("no_rotate") boolean noRotate,
                                   @ArgName("no_pitch") boolean noPitch,
                                   @ArgName("for") @ArgPrefixed @ArgDefaultNull @ArgSubType(PlayerTag.class) List<PlayerTag> forPlayers) {
        if (target == null && !cancel) {
            throw new InvalidArgumentsRuntimeException("Must specify a target entity, or 'cancel'!");
        }
        for (EntityTag entity : entities) {
            if (!entity.isSpawned() && !entity.isFake && !cancel) {
                Debug.echoError("Cannot attach entity '" + entity + "': entity is not spawned.");
                continue;
            }
            // 假实体不在服务端的跟踪之列，其移动只由 Denizen 自行发出，
            // 只改写目标实体的移动数据包便不足以带动它——目标若是玩家本人，
            // 服务端根本不会向其发送自身的移动数据包，附着也就无从谈起。
            // 服务端同步对假实体并无副作用，故一律启用。
            boolean sync = syncServer || entity.isFake;
            Consumer<UUID> procPlayer = (player) -> {
                if (cancel) {
                    EntityAttachmentHelper.removeAttachment(entity.getUUID(), player);
                }
                else {
                    EntityAttachmentHelper.AttachmentData attachment = new EntityAttachmentHelper.AttachmentData();
                    attachment.attached = entity;
                    attachment.to = target;
                    attachment.positionalOffset = offset == null ? null : offset.clone();
                    attachment.offsetRelative = relative;
                    attachment.yawAngleOffset = yawOffset;
                    attachment.pitchAngleOffset = pitchOffset;
                    attachment.syncServer = sync;
                    attachment.forPlayer = player;
                    attachment.noRotate = noRotate;
                    attachment.noPitch = noPitch;
                    EntityAttachmentHelper.registerAttachment(attachment);
                }
            };
            if (forPlayers == null || (forPlayers.isEmpty() && sync)) {
                procPlayer.accept(null);
            }
            else {
                for (PlayerTag player : forPlayers) {
                    procPlayer.accept(player.getUUID());
                }
            }
        }
    }
}
