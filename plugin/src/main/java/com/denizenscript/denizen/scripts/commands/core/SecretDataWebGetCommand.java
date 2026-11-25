package com.denizenscript.denizen.scripts.commands.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.BinaryTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.core.WebGetCommand;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.nio.charset.StandardCharsets;

/**
 * Takes the place of the core 'webget' command, additionally accepting a SecretTag as 'data:'.
 * The secret value is sent as the request body, and only ever leaves this class as raw bytes passed on to the core command, so it never shows up in debug output or tags.
 * Everything else is handled by the core command unchanged.
 */
public class SecretDataWebGetCommand extends WebGetCommand {

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("url") @ArgRaw ElementTag originalUrl,
                                   @ArgPrefixed @ArgName("data") @ArgDefaultNull ObjectTag data,
                                   @ArgPrefixed @ArgName("method") @ArgDefaultNull Method method,
                                   @ArgName("hide_failure") boolean hideFailure,
                                   @ArgPrefixed @ArgName("timeout") @ArgDefaultText("10s") DurationTag timeout,
                                   @ArgPrefixed @ArgName("headers") @ArgDefaultNull ObjectTag headersLegacyCompat,
                                   @ArgPrefixed @ArgName("savefile") @ArgDefaultNull String saveFile) {
        if (data != null && data.canBeType(SecretTag.class)) {
            SecretTag secret = data.asType(SecretTag.class, scriptEntry.context);
            if (secret == null || secret.getValue() == null) {
                Debug.echoError(scriptEntry, "Invalid data SecretTag object '" + data + "' - secret not defined in 'secrets.secret'?");
                return;
            }
            data = new BinaryTag(secret.getValue().getBytes(StandardCharsets.UTF_8));
        }
        WebGetCommand.autoExecute(scriptEntry, originalUrl, data, method, hideFailure, timeout, headersLegacyCompat, saveFile);
    }
}
