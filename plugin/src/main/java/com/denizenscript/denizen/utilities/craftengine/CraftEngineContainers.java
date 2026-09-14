package com.denizenscript.denizen.utilities.craftengine;

import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Reflective access to the container internals of CraftEngine.
 *
 * <p>CraftEngine ships its own Denizen integration, but it exposes nothing for reading the contents of
 * drawer blocks, simple storage blocks or simple storage furniture, and has declined to add it: the request
 * was closed as not planned (<a href="https://github.com/Xiao-MoMi/craft-engine/issues/810">craft-engine
 * issue #810</a>), on the grounds that these containers are custom registered behaviors rather than a
 * general feature. This class is therefore the permanent home for those tags, not a stopgap.
 *
 * <p>Everything is resolved once, when support is initialized, and every value that crosses the boundary is
 * a Bukkit type, a primitive or a String - no CraftEngine type ever leaks out of this class. That keeps
 * Denizen free of any compile time dependency on CraftEngine, and means a CraftEngine update can at worst
 * disable the affected tags instead of breaking the build or throwing at runtime.
 *
 * <p>Because CraftEngine is free to reshuffle these internals at any time, lookups are deliberately
 * forgiving: members are searched for by name first, and then, where the required type identifies them
 * unambiguously, by type. A rename alone will not break this.
 *
 * <p>Lookups go through CraftEngine's own class loader: CraftEngine ships as a Paper plugin, whose classes
 * are not necessarily reachable from another plugin's class loader.
 */
public class CraftEngineContainers {

    /** Matches SimpleStorageFurnitureController.DEFAULT_DATA_KEY, used only if that constant cannot be read. */
    public static final String FURNITURE_FALLBACK_DATA_KEY = "craftengine:simple_storage_contents";

    /** Sanity limit while enumerating behaviors, so a broken controller cannot spin forever. */
    private static final int MAX_BEHAVIORS = 64;

    private static Method adaptWorldMethod;
    private static Method storageWorldMethod;
    private static Constructor<?> blockPosConstructor;
    private static Method blockEntityAtMethod;
    private static Field blockEntityControllerField;
    private static Method blockControllerGetMethod;
    private static Method bukkitItemMethod;

    private static Class<?> drawerClass;
    private static Field drawerBehaviorField;
    private static Field drawerDataKeyField;
    private static Method drawerItemMethod;
    private static Method drawerItemCountMethod;
    private static Method drawerMaxCountMethod;

    private static Class<?> storageBlockClass;
    private static Field storageBlockBehaviorField;
    private static Field storageBlockDataKeyField;
    private static Method storageBlockInventoryMethod;

    private static Method furnitureByMetaEntityMethod;
    private static Method furnitureBySeatMethod;
    private static Method furnitureByColliderMethod;
    private static Field furnitureConfigField;
    private static Field furnitureControllerField;
    private static Method definitionBehaviorsMethod;
    private static Method furnitureControllerGetMethod;

    private static Class<?> storageFurnitureClass;
    private static Field storageFurnitureInventoryField;
    private static Field storageFurnitureTemplateField;
    private static Field storageFurnitureDataKeyField;
    private static String furnitureDefaultDataKey = FURNITURE_FALLBACK_DATA_KEY;

    private static boolean blocksReady = false;
    private static boolean furnitureReady = false;

    /** Whether the drawer and simple storage block tags can be served. */
    public static boolean blocksAvailable() {
        return blocksReady;
    }

    /** Whether the simple storage furniture tags can be served. */
    public static boolean furnitureAvailable() {
        return furnitureReady;
    }

    /** Resolves everything this class needs. A half that fails to resolve simply stays disabled. */
    public static void init(Plugin craftEngine) {
        ClassLoader loader = craftEngine.getClass().getClassLoader();
        blocksReady = initBlocks(loader);
        furnitureReady = initFurniture(loader);
    }

    private static boolean initBlocks(ClassLoader loader) {
        try {
            Class<?> adaptor = lookup(loader, "net.momirealms.craftengine.bukkit.api.BukkitAdaptor");
            adaptWorldMethod = adaptor.getMethod("adapt", org.bukkit.World.class);
            Class<?> ceWorldClass = lookup(loader, "net.momirealms.craftengine.core.world.CEWorld");
            storageWorldMethod = method(lookup(loader, "net.momirealms.craftengine.core.world.World"), ceWorldClass, "storageWorld", "ceWorld");
            Class<?> blockPosClass = lookup(loader, "net.momirealms.craftengine.core.world.BlockPos");
            blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            blockEntityAtMethod = method(ceWorldClass, "getBlockEntityAtIfLoaded",
                    new Class<?>[] { blockPosClass, boolean.class },
                    new Class<?>[] { blockPosClass });
            Class<?> blockControllerClass = lookup(loader, "net.momirealms.craftengine.core.block.entity.BlockEntityController");
            blockEntityControllerField = field(lookup(loader, "net.momirealms.craftengine.core.block.entity.BlockEntity"), blockControllerClass, "controller");
            blockControllerGetMethod = blockControllerClass.getMethod("get", Class.class, int.class);
            bukkitItemMethod = method(lookup(loader, "net.momirealms.craftengine.bukkit.item.BukkitItem"), ItemStack.class, "getBukkitItem");

            drawerClass = lookup(loader, "net.momirealms.craftengine.bukkit.block.entity.DrawerBlockEntityController");
            Class<?> drawerBehaviorClass = lookup(loader, "net.momirealms.craftengine.bukkit.block.behavior.DrawerBlockBehavior");
            drawerBehaviorField = field(drawerClass, drawerBehaviorClass, "behavior");
            drawerDataKeyField = field(drawerBehaviorClass, String.class, "customDataKey");
            drawerItemMethod = method(drawerClass, null, "item");
            drawerItemCountMethod = method(drawerClass, null, "itemCount");
            drawerMaxCountMethod = method(drawerClass, null, "maxCount");

            storageBlockClass = lookup(loader, "net.momirealms.craftengine.bukkit.block.entity.SimpleStorageBlockEntityController");
            Class<?> storageBlockBehaviorClass = lookup(loader, "net.momirealms.craftengine.bukkit.block.behavior.SimpleStorageBlockBehavior");
            storageBlockInventoryMethod = method(storageBlockClass, Inventory.class, "inventory");
            storageBlockBehaviorField = field(storageBlockClass, storageBlockBehaviorClass, "behavior");
            // Two String fields live on this behavior, so a rename here cannot be recovered from by type.
            storageBlockDataKeyField = field(storageBlockBehaviorClass, null, "customDataKey");
            return true;
        }
        catch (Throwable ex) {
            Debug.log("CraftEngine block container tags are unavailable, CraftEngine's internals have changed: " + ex);
            return false;
        }
    }

    private static boolean initFurniture(ClassLoader loader) {
        try {
            Class<?> api = lookup(loader, "net.momirealms.craftengine.bukkit.api.CraftEngineFurniture");
            furnitureByMetaEntityMethod = api.getMethod("getLoadedFurnitureByMetaEntity", Entity.class);
            furnitureBySeatMethod = api.getMethod("getLoadedFurnitureBySeat", Entity.class);
            furnitureByColliderMethod = api.getMethod("getLoadedFurnitureByCollider", Entity.class);

            Class<?> furnitureClass = lookup(loader, "net.momirealms.craftengine.core.entity.furniture.Furniture");
            Class<?> definitionClass = lookup(loader, "net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition");
            Class<?> furnitureControllerClass = lookup(loader, "net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController");
            furnitureConfigField = field(furnitureClass, definitionClass, "config");
            furnitureControllerField = field(furnitureClass, furnitureControllerClass, "controller");
            definitionBehaviorsMethod = method(definitionClass, null, "behaviors");
            furnitureControllerGetMethod = furnitureControllerClass.getMethod("get", Class.class, int.class);

            Class<?> templateClass = lookup(loader, "net.momirealms.craftengine.bukkit.entity.furniture.behavior.SimpleStorageFurnitureBehaviorTemplate");
            storageFurnitureClass = lookup(loader, "net.momirealms.craftengine.bukkit.entity.furniture.behavior.SimpleStorageFurnitureBehaviorTemplate$SimpleStorageFurnitureController");
            // Unlike the simple storage block, the furniture controller has no inventory() method at all.
            storageFurnitureInventoryField = field(storageFurnitureClass, Inventory.class, "inventory");
            storageFurnitureTemplateField = field(storageFurnitureClass, templateClass, "template");
            // Two String fields live on this template, so a rename here cannot be recovered from by type.
            storageFurnitureDataKeyField = field(templateClass, null, "customDataKey");
            // Unlike the two block behaviors, the furniture behavior has no default data key: an unconfigured
            // furniture keeps a null key and only falls back to this constant when its contents are read or written.
            try {
                Field defaultKey = storageFurnitureClass.getDeclaredField("DEFAULT_DATA_KEY");
                defaultKey.setAccessible(true);
                Object value = defaultKey.get(null);
                if (value instanceof String string) {
                    furnitureDefaultDataKey = string;
                }
            }
            catch (Throwable ex) {
                furnitureDefaultDataKey = FURNITURE_FALLBACK_DATA_KEY;
            }
            return true;
        }
        catch (Throwable ex) {
            Debug.log("CraftEngine furniture container tags are unavailable, CraftEngine's internals have changed: " + ex);
            return false;
        }
    }

    private static Class<?> lookup(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name, false, loader);
    }

    /**
     * Finds a field by name, searching superclasses too, and failing that the one and only instance field of
     * the required type. Pass a null type to look the field up by name alone, which is what an ambiguous type
     * calls for.
     */
    static Field field(Class<?> owner, Class<?> type, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
                try {
                    Field found = current.getDeclaredField(name);
                    if (type == null || type.isAssignableFrom(found.getType())) {
                        found.setAccessible(true);
                        return found;
                    }
                }
                catch (NoSuchFieldException ex) {
                    // Keep walking up.
                }
            }
        }
        if (type != null) {
            Field unique = null;
            for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
                for (Field candidate : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(candidate.getModifiers()) && candidate.getType() == type) {
                        if (unique != null) {
                            throw new NoSuchFieldException(owner.getName() + " has more than one field of type " + type.getName());
                        }
                        unique = candidate;
                    }
                }
            }
            if (unique != null) {
                unique.setAccessible(true);
                return unique;
            }
        }
        throw new NoSuchFieldException(owner.getName() + "#" + names[0]);
    }

    /**
     * Finds a no-argument method by name, and failing that the one and only no-argument method returning the
     * required type. Pass a null type to look the method up by name alone.
     */
    static Method method(Class<?> owner, Class<?> type, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            try {
                return owner.getMethod(name);
            }
            catch (NoSuchMethodException ex) {
                // Try the next name.
            }
        }
        if (type != null) {
            Method unique = null;
            for (Method candidate : owner.getMethods()) {
                if (candidate.getParameterCount() == 0 && candidate.getReturnType() == type) {
                    if (unique != null) {
                        throw new NoSuchMethodException(owner.getName() + " has more than one no-argument method returning " + type.getName());
                    }
                    unique = candidate;
                }
            }
            if (unique != null) {
                return unique;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + names[0] + "()");
    }

    /** Finds a method by name, trying each candidate parameter list in turn. */
    static Method method(Class<?> owner, String name, Class<?>[]... parameterSets) throws ReflectiveOperationException {
        for (Class<?>[] parameters : parameterSets) {
            try {
                return owner.getMethod(name, parameters);
            }
            catch (NoSuchMethodException ex) {
                // Try the next parameter list.
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + name);
    }

    // Drawer blocks

    /** Every drawer on the block, in the order the behaviors are declared. */
    public static List<Object> drawersAt(Location location) {
        return blockControllers(location, drawerClass);
    }

    /** The drawer with the given data key, or the first one when the key is null. */
    public static Object drawerAt(Location location, String dataKey) {
        return firstMatching(drawersAt(location), dataKey, CraftEngineContainers::drawerDataKey);
    }

    public static String drawerDataKey(Object drawer) {
        try {
            Object behavior = drawerBehaviorField.get(drawer);
            return behavior == null ? null : (String) drawerDataKeyField.get(behavior);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    /**
     * The item held by the drawer as a Bukkit stack, or null when empty or unreadable.
     * The quantity is meaningless here - the compatible mode implementation stores real stacks, the modern one
     * a single template item - so callers should normalize it and use {@link #drawerItemCount} instead.
     */
    public static ItemStack drawerItem(Object drawer) {
        try {
            Object item = drawerItemMethod.invoke(drawer);
            return item == null ? null : (ItemStack) bukkitItemMethod.invoke(item);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static Integer drawerItemCount(Object drawer) {
        try {
            return (Integer) drawerItemCountMethod.invoke(drawer);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static Integer drawerMaxCount(Object drawer) {
        try {
            return (Integer) drawerMaxCountMethod.invoke(drawer);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    // Simple storage blocks

    /** Every simple storage inventory on the block, in the order the behaviors are declared. */
    public static List<Object> storageBlocksAt(Location location) {
        return blockControllers(location, storageBlockClass);
    }

    /** The simple storage with the given data key, or the first one when the key is null. */
    public static Object storageBlockAt(Location location, String dataKey) {
        return firstMatching(storageBlocksAt(location), dataKey, CraftEngineContainers::storageBlockDataKey);
    }

    public static String storageBlockDataKey(Object storage) {
        try {
            Object behavior = storageBlockBehaviorField.get(storage);
            return behavior == null ? null : (String) storageBlockDataKeyField.get(behavior);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    /** The live container, or null once the block entity has gone invalid. */
    public static Inventory storageBlockInventory(Object storage) {
        try {
            return (Inventory) storageBlockInventoryMethod.invoke(storage);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    // Simple storage furniture

    /** Every simple storage inventory on the furniture, in the order the behaviors are declared. */
    public static List<Object> storageFurnitureOn(Entity entity) {
        if (!furnitureReady || entity == null) {
            return Collections.emptyList();
        }
        Object furniture = furnitureOf(entity);
        if (furniture == null) {
            return Collections.emptyList();
        }
        try {
            Object controller = furnitureControllerField.get(furniture);
            if (controller == null) {
                return Collections.emptyList();
            }
            // Furniture controllers index by behavior slot rather than by "the nth of this type", and the single
            // behavior case ignores the index entirely, so the loop has to be bounded by the behavior count.
            Object definition = furnitureConfigField.get(furniture);
            int behaviors = definition == null ? 1 : ((List<?>) definitionBehaviorsMethod.invoke(definition)).size();
            List<Object> result = new ArrayList<>(2);
            for (int index = 0; index < Math.min(Math.max(behaviors, 1), MAX_BEHAVIORS); index++) {
                Object found = furnitureControllerGetMethod.invoke(controller, storageFurnitureClass, index);
                if (found != null && !result.contains(found)) {
                    result.add(found);
                }
            }
            return result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return Collections.emptyList();
        }
    }

    /** The storage furniture with the given data key, or the first one when the key is null. */
    public static Object storageFurnitureOn(Entity entity, String dataKey) {
        return firstMatching(storageFurnitureOn(entity), dataKey, CraftEngineContainers::storageFurnitureDataKey);
    }

    /** Never null for a valid controller: an unconfigured furniture reports the key its contents actually live under. */
    public static String storageFurnitureDataKey(Object storage) {
        try {
            Object template = storageFurnitureTemplateField.get(storage);
            String key = template == null ? null : (String) storageFurnitureDataKeyField.get(template);
            return key == null ? furnitureDefaultDataKey : key;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    /** The live container. */
    public static Inventory storageFurnitureInventory(Object storage) {
        try {
            return (Inventory) storageFurnitureInventoryField.get(storage);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    /**
     * Resolves any of a furniture's entities to the furniture itself, in the order CraftEngine's own
     * integration uses: the base entity, then a seat, then a collider.
     */
    private static Object furnitureOf(Entity entity) {
        for (Method resolver : new Method[] { furnitureByMetaEntityMethod, furnitureBySeatMethod, furnitureByColliderMethod }) {
            try {
                Object furniture = resolver.invoke(null, entity);
                if (furniture != null) {
                    return furniture;
                }
            }
            catch (Throwable ex) {
                // A resolver that rejects this kind of entity is normal, try the next one.
            }
        }
        return null;
    }

    // Shared helpers

    private static List<Object> blockControllers(Location location, Class<?> type) {
        if (!blocksReady || location == null || location.getWorld() == null) {
            return Collections.emptyList();
        }
        try {
            Object world = storageWorldMethod.invoke(adaptWorldMethod.invoke(null, location.getWorld()));
            if (world == null) {
                return Collections.emptyList();
            }
            Object pos = blockPosConstructor.newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // Never create the block entity: this may run off the main thread, and a block whose contents are
            // worth reading always has one already. Only the fallback one-argument form, which CraftEngine
            // defines as creating on demand, gives that up.
            Object blockEntity = blockEntityAtMethod.getParameterCount() == 2
                    ? blockEntityAtMethod.invoke(world, pos, false)
                    : blockEntityAtMethod.invoke(world, pos);
            if (blockEntity == null) {
                return Collections.emptyList();
            }
            Object controller = blockEntityControllerField.get(blockEntity);
            if (controller == null) {
                return Collections.emptyList();
            }
            List<Object> result = new ArrayList<>(2);
            for (int order = 0; order < MAX_BEHAVIORS; order++) {
                Object found = blockControllerGetMethod.invoke(controller, type, order);
                if (found == null) {
                    break;
                }
                result.add(found);
            }
            return result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return Collections.emptyList();
        }
    }

    private static Object firstMatching(List<Object> controllers, String dataKey, Function<Object, String> keyOf) {
        if (controllers.isEmpty()) {
            return null;
        }
        if (dataKey == null) {
            return controllers.get(0);
        }
        for (Object controller : controllers) {
            if (dataKey.equals(keyOf.apply(controller))) {
                return controller;
            }
        }
        return null;
    }
}
