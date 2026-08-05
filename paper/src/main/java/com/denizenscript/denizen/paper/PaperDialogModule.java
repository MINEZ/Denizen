package com.denizenscript.denizen.paper;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.paper.commands.ShowDialogCommand;
import com.denizenscript.denizen.paper.containers.DialogScriptContainer;
import com.denizenscript.denizen.paper.events.PlayerCustomClickScriptEvent;
import com.denizenscript.denizen.paper.objects.ConnectionTag;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import org.bukkit.Bukkit;

/**
 * 对话框相关功能的注册入口。
 * <p>
 * 单独成类是为了让类加载可以推迟到确认 Paper 的 dialog API 存在之后，
 * 老版本 Paper 上不会因为找不到 API 类而抛出 NoClassDefFoundError。
 */
public class PaperDialogModule {

    public static void init() {
        ScriptRegistry._registerType("dialog", DialogScriptContainer.class);
        DenizenCore.commandRegistry.registerCommand(ShowDialogCommand.class);
        ScriptEvent.registerScriptEvent(PlayerCustomClickScriptEvent.class);
        ObjectFetcher.registerWithObjectFetcher(ConnectionTag.class, ConnectionTag.tagProcessor).setAsNOtherCode();
        Bukkit.getPluginManager().registerEvents(new PlayerCustomClickScriptEvent.DialogEvents(), Denizen.getInstance());
        registerPlayerMechanisms();
    }

    public static void registerPlayerMechanisms() {

        // <--[mechanism]
        // @object PlayerTag
        // @name show_dialog
        // @input ScriptTag
        // @Plugin Paper
        // @group paper
        // @description
        // Opens a dialog for the player, using the name of a dialog script container.
        // See <@link language Dialog Script Containers>.
        // Use <@link command showdialog> instead when you need to pass definitions in.
        // @tags
        // <PlayerTag.is_online>
        // -->
        PlayerTag.registerOnlineOnlyMechanism("show_dialog", ScriptTag.class, (object, mechanism, input) -> {
            if (!(input.getContainer() instanceof DialogScriptContainer container)) {
                mechanism.echoError("Invalid script '" + input.getName() + "' for mechanism 'show_dialog': must be a dialog script container.");
                return;
            }
            BukkitTagContext context = new BukkitTagContext(object, null, input);
            container.showTo(object.getPlayerEntity().getConnection(), context);
        });

        // <--[mechanism]
        // @object PlayerTag
        // @name close_dialog
        // @input None
        // @Plugin Paper
        // @group paper
        // @description
        // Closes the player's currently open dialog, if any.
        // -->
        PlayerTag.registerOnlineOnlyMechanism("close_dialog", (object, mechanism) -> {
            object.getPlayerEntity().closeDialog();
        });
    }
}
