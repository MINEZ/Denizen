package com.denizenscript.denizen.utilities.craftengine;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The container tags CraftEngine's own Denizen integration does not provide.
 *
 * <p>Names and behaviour follow the request filed as craft-engine issue #810. A data key matches the
 * 'data_key' option of the corresponding behavior exactly as written, with no namespace completion; leaving
 * it out takes the first container, meaning the first in the order the behaviors are declared.
 */
public class CraftEngineTags {

    public static void register() {
        if (CraftEngineContainers.blocksAvailable()) {
            registerBlockTags();
        }
        if (CraftEngineContainers.furnitureAvailable()) {
            registerFurnitureTags();
        }
    }

    public static void registerBlockTags() {

        // <--[tag]
        // @attribute <LocationTag.has_ce_drawer[(<data_key>)]>
        // @returns ElementTag(Boolean)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns whether the block at the location is a CraftEngine drawer.
        // If a data key is specified, returns whether a drawer with that data key exists at the location.
        // Otherwise, returns whether any drawer exists at the location.
        // @Example
        // # Narrate what is stored in the drawer the player is looking at.
        // - if <player.cursor_on.has_ce_drawer>:
        //     - narrate "This drawer holds <player.cursor_on.ce_drawer_item_quantity> of <player.cursor_on.ce_drawer_item.material.name>."
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "has_ce_drawer", (attribute, object) -> {
            return new ElementTag(CraftEngineContainers.drawerAt(object, param(attribute)) != null);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_drawer_item[(<data_key>)]>
        // @returns ItemTag
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns the item currently stored in the CraftEngine drawer at the location, or air if the drawer is empty.
        // The returned item always has a quantity of 1 - use <@link tag LocationTag.ce_drawer_item_quantity> for the
        // real amount, which may far exceed a single stack.
        // Returns nothing if the block is not a CraftEngine drawer.
        // @Example
        // # Give the player one of whatever the drawer holds.
        // - define item <player.cursor_on.ce_drawer_item>
        // - if <[item].material.name> != air:
        //     - give <[item]>
        // -->
        LocationTag.tagProcessor.registerTag(ItemTag.class, "ce_drawer_item", (attribute, object) -> {
            Object drawer = CraftEngineContainers.drawerAt(object, param(attribute));
            if (drawer == null) {
                return null;
            }
            ItemStack item = CraftEngineContainers.drawerItem(drawer);
            if (item == null || item.getType() == Material.AIR) {
                return new ItemTag(Material.AIR);
            }
            ItemStack single = item.clone();
            single.setAmount(1);
            return new ItemTag(single);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_drawer_item_quantity[(<data_key>)]>
        // @returns ElementTag(Number)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns how many items the CraftEngine drawer at the location currently holds.
        // Returns nothing if the block is not a CraftEngine drawer.
        // @Example
        // # Warn the player when the drawer is running low.
        // - if <player.cursor_on.ce_drawer_item_quantity.if_null[0]> < 64:
        //     - narrate "<&c>This drawer is running low."
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "ce_drawer_item_quantity", (attribute, object) -> {
            Object drawer = CraftEngineContainers.drawerAt(object, param(attribute));
            if (drawer == null) {
                return null;
            }
            Integer count = CraftEngineContainers.drawerItemCount(drawer);
            return count == null ? null : new ElementTag(count);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_drawer_max_quantity[(<data_key>)]>
        // @returns ElementTag(Number)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns how many items the CraftEngine drawer at the location can hold in total.
        // Note that this depends on the stored item: a drawer configured for 32 stacks reports 32 while empty,
        // and 32 times the stored item's max stack size once something is put in. Drawers in compatible mode
        // always report the configured stack count instead.
        // Returns nothing if the block is not a CraftEngine drawer.
        // @Example
        // # Show the drawer's fill level as a percentage.
        // - define location <player.cursor_on>
        // - narrate "This drawer is <[location].ce_drawer_item_quantity.div[<[location].ce_drawer_max_quantity>].mul[100].round>% full."
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "ce_drawer_max_quantity", (attribute, object) -> {
            Object drawer = CraftEngineContainers.drawerAt(object, param(attribute));
            if (drawer == null) {
                return null;
            }
            Integer max = CraftEngineContainers.drawerMaxCount(drawer);
            return max == null ? null : new ElementTag(max);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_drawer_data_keys>
        // @returns ListTag(ElementTag)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns a list of the data keys of every CraftEngine drawer at the location, in the order the behaviors
        // are declared in the block configuration.
        // Returns an empty list if the block has no CraftEngine drawer.
        // @Example
        // # Narrate the contents of every drawer on the block.
        // - foreach <player.cursor_on.ce_drawer_data_keys> as:key:
        //     - narrate "<[key]>: <player.cursor_on.ce_drawer_item_quantity[<[key]>]>"
        // -->
        LocationTag.tagProcessor.registerTag(ListTag.class, "ce_drawer_data_keys", (attribute, object) -> {
            List<Object> drawers = CraftEngineContainers.drawersAt(object);
            ListTag keys = new ListTag(drawers.size());
            for (Object drawer : drawers) {
                String key = CraftEngineContainers.drawerDataKey(drawer);
                if (key != null) {
                    keys.addObject(new ElementTag(key, true));
                }
            }
            return keys;
        });

        // <--[tag]
        // @attribute <LocationTag.has_ce_storage_inventory[(<data_key>)]>
        // @returns ElementTag(Boolean)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns whether the block at the location is a CraftEngine simple storage block.
        // If a data key is specified, returns whether a storage inventory with that data key exists at the location.
        // Otherwise, returns whether any storage inventory exists at the location.
        // @Example
        // # Narrate how many items the storage block holds.
        // - if <player.cursor_on.has_ce_storage_inventory>:
        //     - narrate "This storage block holds <player.cursor_on.ce_storage_inventory.list_contents.size> stacks."
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "has_ce_storage_inventory", (attribute, object) -> {
            return new ElementTag(CraftEngineContainers.storageBlockAt(object, param(attribute)) != null);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_storage_inventory[(<data_key>)]>
        // @returns InventoryTag
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns the inventory of the CraftEngine simple storage block at the location.
        // The returned inventory is the live container, so changes made to it apply immediately.
        // Returns nothing if the block is not a CraftEngine simple storage block, or if no storage inventory with
        // the specified data key exists.
        // @Example
        // # Take one diamond out of the storage block.
        // - take item:diamond quantity:1 from:<player.cursor_on.ce_storage_inventory>
        // -->
        LocationTag.tagProcessor.registerTag(InventoryTag.class, "ce_storage_inventory", (attribute, object) -> {
            Object storage = CraftEngineContainers.storageBlockAt(object, param(attribute));
            if (storage == null) {
                return null;
            }
            Inventory inventory = CraftEngineContainers.storageBlockInventory(storage);
            return inventory == null ? null : InventoryTag.mirrorBukkitInventory(inventory);
        });

        // <--[tag]
        // @attribute <LocationTag.ce_storage_inventory_data_keys>
        // @returns ListTag(ElementTag)
        // @group world
        // @plugin CraftEngine
        // @description
        // Returns a list of the data keys of every CraftEngine simple storage inventory at the location, in the
        // order the behaviors are declared in the block configuration.
        // Returns an empty list if the block has no CraftEngine storage inventory.
        // @Example
        // # Narrate the size of every storage inventory on the block.
        // - foreach <player.cursor_on.ce_storage_inventory_data_keys> as:key:
        //     - narrate "<[key]>: <player.cursor_on.ce_storage_inventory[<[key]>].list_contents.size> stacks"
        // -->
        LocationTag.tagProcessor.registerTag(ListTag.class, "ce_storage_inventory_data_keys", (attribute, object) -> {
            List<Object> storages = CraftEngineContainers.storageBlocksAt(object);
            ListTag keys = new ListTag(storages.size());
            for (Object storage : storages) {
                String key = CraftEngineContainers.storageBlockDataKey(storage);
                if (key != null) {
                    keys.addObject(new ElementTag(key, true));
                }
            }
            return keys;
        });
    }

    public static void registerFurnitureTags() {

        // <--[tag]
        // @attribute <EntityTag.has_ce_storage_inventory[(<data_key>)]>
        // @returns ElementTag(Boolean)
        // @group inventory
        // @plugin CraftEngine
        // @description
        // Returns whether the entity is a CraftEngine furniture with a simple storage inventory.
        // If a data key is specified, returns whether a storage inventory with that data key exists on the furniture.
        // Otherwise, returns whether any storage inventory exists on it.
        // Any of a furniture's entities will do: the base entity, a seat, or a collider.
        // @Example
        // # Narrate how many items the furniture holds.
        // - if <player.precise_target.has_ce_storage_inventory.if_null[false]>:
        //     - narrate "This furniture holds <player.precise_target.ce_storage_inventory.list_contents.size> stacks."
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "has_ce_storage_inventory", (attribute, object) -> {
            return new ElementTag(CraftEngineContainers.storageFurnitureOn(object.getBukkitEntity(), param(attribute)) != null);
        });

        // <--[tag]
        // @attribute <EntityTag.ce_storage_inventory[(<data_key>)]>
        // @returns InventoryTag
        // @group inventory
        // @plugin CraftEngine
        // @description
        // Returns the inventory of the CraftEngine simple storage furniture.
        // The returned inventory is the live container, so changes made to it apply immediately.
        // Returns nothing if the entity is not a CraftEngine simple storage furniture, or if no storage inventory
        // with the specified data key exists.
        // @Example
        // # Put a diamond into the furniture.
        // - give <item[diamond]> to:<player.precise_target.ce_storage_inventory>
        // -->
        EntityTag.tagProcessor.registerTag(InventoryTag.class, "ce_storage_inventory", (attribute, object) -> {
            Object storage = CraftEngineContainers.storageFurnitureOn(object.getBukkitEntity(), param(attribute));
            if (storage == null) {
                return null;
            }
            Inventory inventory = CraftEngineContainers.storageFurnitureInventory(storage);
            return inventory == null ? null : InventoryTag.mirrorBukkitInventory(inventory);
        });

        // <--[tag]
        // @attribute <EntityTag.ce_storage_inventory_data_keys>
        // @returns ListTag(ElementTag)
        // @group inventory
        // @plugin CraftEngine
        // @description
        // Returns a list of the data keys of every CraftEngine simple storage inventory on the furniture, in the
        // order the behaviors are declared in the furniture configuration.
        // A furniture that leaves 'data_key' unset reports the key its contents actually live under, which is
        // 'craftengine:simple_storage_contents'.
        // Returns an empty list if the entity has no CraftEngine storage inventory.
        // @Example
        // # Narrate the size of every storage inventory on the furniture.
        // - foreach <player.precise_target.ce_storage_inventory_data_keys> as:key:
        //     - narrate "<[key]>: <player.precise_target.ce_storage_inventory[<[key]>].list_contents.size> stacks"
        // -->
        EntityTag.tagProcessor.registerTag(ListTag.class, "ce_storage_inventory_data_keys", (attribute, object) -> {
            List<Object> storages = CraftEngineContainers.storageFurnitureOn(object.getBukkitEntity());
            ListTag keys = new ListTag(storages.size());
            for (Object storage : storages) {
                String key = CraftEngineContainers.storageFurnitureDataKey(storage);
                if (key != null) {
                    keys.addObject(new ElementTag(key, true));
                }
            }
            return keys;
        });
    }

    private static String param(Attribute attribute) {
        return attribute.hasParam() ? attribute.getParam() : null;
    }
}
