package com.denizenscript.denizen.paper.objects;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.paper.PaperModule;
import com.denizenscript.denizen.paper.containers.DialogScriptContainer;
import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

// <--[ObjectType]
// @name ConnectionTag
// @prefix connection
// @base ElementTag
// @format
// The identity format for connections is the UUID of the player profile on the other end.
// For example: 'connection@dfc67056-b15d-45dd-b239-482d92e482e5'.
//
// @Plugin Paper
// @description
// A ConnectionTag represents a single player connection to the server.
// This is most often obtained from <@link event player custom click> as '<context.connection>',
// and exists mainly so dialog buttons can be handled uniformly.
//
// -->

@SuppressWarnings("UnstableApiUsage")
public class ConnectionTag implements ObjectTag, Adjustable {

    public final PlayerCommonConnection connection;

    public UUID uuid;

    public ConnectionTag(PlayerCommonConnection connection) {
        this.connection = connection;
        if (connection instanceof PlayerGameConnection gameConnection) {
            this.uuid = gameConnection.getPlayer().getUniqueId();
        }
        else if (connection instanceof PlayerConfigurationConnection configurationConnection) {
            this.uuid = configurationConnection.getProfile().getId();
        }
    }

    @Fetchable("connection")
    public static ConnectionTag valueOf(String string, TagContext context) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        if (matches(string)) {
            string = string.substring("connection@".length());
        }
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(string));
            if (player != null) {
                return new ConnectionTag(player.getConnection());
            }
        }
        catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    public static boolean matches(String input) {
        return input != null && CoreUtilities.toLowerCase(input).startsWith("connection@");
    }

    public String prefix = "Connection";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String identify() {
        return "connection@" + uuid;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    public static ObjectTagProcessor<ConnectionTag> tagProcessor = new ObjectTagProcessor<>();

    public static void register() {

        // <--[tag]
        // @attribute <ConnectionTag.is_connected>
        // @returns ElementTag(Boolean)
        // @Plugin Paper
        // @description
        // Returns whether this connection is currently open and active.
        // -->
        tagProcessor.registerTag(ElementTag.class, "is_connected", (attribute, object) -> {
            return new ElementTag(object.connection.isConnected());
        });

        // <--[tag]
        // @attribute <ConnectionTag.uuid>
        // @returns ElementTag
        // @Plugin Paper
        // @description
        // Returns the UUID of the player profile associated with this connection.
        // -->
        tagProcessor.registerTag(ElementTag.class, "uuid", (attribute, object) -> {
            return object.uuid == null ? null : new ElementTag(object.uuid.toString());
        });

        // <--[tag]
        // @attribute <ConnectionTag.name>
        // @returns ElementTag
        // @Plugin Paper
        // @description
        // Returns the name of the player profile associated with this connection.
        // -->
        tagProcessor.registerTag(ElementTag.class, "name", (attribute, object) -> {
            if (object.connection instanceof PlayerConfigurationConnection configurationConnection) {
                return new ElementTag(configurationConnection.getProfile().getName());
            }
            else if (object.connection instanceof PlayerGameConnection gameConnection) {
                return new ElementTag(gameConnection.getPlayer().getName());
            }
            return null;
        });

        // <--[tag]
        // @attribute <ConnectionTag.player>
        // @returns PlayerTag
        // @Plugin Paper
        // @description
        // Returns the player on the other end of this connection, if the player is already in the game.
        // Returns null for a connection that is still in the configuration phase.
        // -->
        tagProcessor.registerTag(PlayerTag.class, "player", (attribute, object) -> {
            if (object.connection instanceof PlayerGameConnection gameConnection) {
                return new PlayerTag(gameConnection.getPlayer());
            }
            return null;
        });

        // <--[mechanism]
        // @object ConnectionTag
        // @name disconnect
        // @input ElementTag
        // @Plugin Paper
        // @description
        // Disconnects the connection with a specified reason.
        // -->
        tagProcessor.registerMechanism("disconnect", false, ElementTag.class, (object, mechanism, input) -> {
            object.connection.disconnect(PaperModule.parseFormattedText(input.asString(), ChatColor.WHITE));
        });

        // <--[mechanism]
        // @object ConnectionTag
        // @name close_dialog
        // @input None
        // @Plugin Paper
        // @description
        // Closes any currently open dialog for this connection.
        // -->
        tagProcessor.registerMechanism("close_dialog", false, (object, mechanism) -> {
            if (object.connection instanceof PlayerConfigurationConnection configurationConnection) {
                configurationConnection.getAudience().closeDialog();
            }
            else if (object.connection instanceof PlayerGameConnection gameConnection) {
                gameConnection.getPlayer().closeDialog();
            }
        });

        // <--[mechanism]
        // @object ConnectionTag
        // @name show_dialog
        // @input ScriptTag
        // @Plugin Paper
        // @description
        // Shows a dialog to this connection, using the name of a dialog script container.
        // See <@link language Dialog Script Containers>.
        // -->
        tagProcessor.registerMechanism("show_dialog", false, ScriptTag.class, (object, mechanism, input) -> {
            if (input.getContainer() instanceof DialogScriptContainer container) {
                container.showTo(object.connection, mechanism.context);
                return;
            }
            mechanism.echoError("Invalid script '" + input.getName() + "' for mechanism 'show_dialog': must be a dialog script container.");
        });
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        mechanism.echoError("Cannot apply properties to a ConnectionTag!");
    }

    @Override
    public void adjust(Mechanism mechanism) {
        tagProcessor.processMechanism(this, mechanism);
    }
}
