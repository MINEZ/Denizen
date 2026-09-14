package com.denizenscript.denizen.utilities.craftengine;

import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Optional support for reading the containers of CraftEngine's custom blocks and furniture.
 *
 * <p>Nothing here is registered unless CraftEngine is installed, and CraftEngine is never linked against at
 * compile time - see {@link CraftEngineContainers} for why.
 */
public class CraftEngineSupport {

    /** Whether the CraftEngine container tags were registered. */
    public static boolean isActive = false;

    public static void init() {
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");
        if (craftEngine == null) {
            return;
        }
        CraftEngineContainers.init(craftEngine);
        if (!CraftEngineContainers.blocksAvailable() && !CraftEngineContainers.furnitureAvailable()) {
            Debug.log("CraftEngine is installed, but none of its container internals could be read - the CraftEngine container tags will not be available.");
            return;
        }
        CraftEngineTags.register();
        isActive = true;
        Debug.log("Loaded CraftEngine container support.");
    }
}
