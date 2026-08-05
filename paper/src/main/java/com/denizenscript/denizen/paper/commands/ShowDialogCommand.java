package com.denizenscript.denizen.paper.commands;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.paper.containers.DialogScriptContainer;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.List;

public class ShowDialogCommand extends AbstractCommand {

    public ShowDialogCommand() {
        setName("showdialog");
        setSyntax("showdialog [<dialog>] (def:<ListTag>)");
        setRequiredArguments(1, 2);
        autoCompile();
    }

    // <--[command]
    // @Name showdialog
    // @Syntax showdialog [<dialog>] (def:<ListTag>)
    // @Required 1
    // @Maximum 2
    // @Short Opens a custom Paper dialog for a player.
    // @Group player
    // @Plugin Paper
    //
    // @Description
    // Opens a Paper-native dialog window for the player linked to the script queue.
    // The dialog layout must be defined in a dialog script container,
    // see <@link language Dialog Script Containers>.
    //
    // Optionally specify a list of definitions to pass in, named by the container's 'definitions' key,
    // in the same way <@link command run> passes definitions to a task script.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to open a simple dialog script with no definitions.
    // - showdialog MySimpleDialog
    //
    // @Usage
    // Use to pass definitions (like a quest ID or a reward amount) into the dialog.
    // - showdialog QuestConfirmDialog def:quest_01|1000
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("dialog") @ArgLinear ScriptTag dialog,
                                   @ArgName("def") @ArgPrefixed @ArgDefaultNull ListTag definitions) {
        if (!(dialog.getContainer() instanceof DialogScriptContainer container)) {
            Debug.echoError(scriptEntry, "Invalid dialog script: '" + dialog.getName() + "'.");
            return;
        }
        PlayerTag player = Utilities.getEntryPlayer(scriptEntry);
        if (player == null || !player.isOnline()) {
            Debug.echoError(scriptEntry, "The 'showdialog' command requires an online linked player.");
            return;
        }
        if (definitions != null) {
            DefinitionProvider provider = scriptEntry.context.definitionProvider;
            List<String> definitionNames = null;
            if (container.contains("definitions", String.class)) {
                definitionNames = CoreUtilities.split(container.getString("definitions"), '|');
            }
            int x = 1;
            for (ObjectTag definition : definitions.objectForms) {
                String name = definitionNames != null && definitionNames.size() >= x ? definitionNames.get(x - 1).trim() : String.valueOf(x);
                int squareBracket = name.indexOf('[');
                if (squareBracket != -1) {
                    name = name.substring(0, squareBracket).trim();
                }
                provider.addDefinition(name, definition);
                x++;
            }
        }
        container.showTo(player.getPlayerEntity().getConnection(), scriptEntry.context);
    }
}
