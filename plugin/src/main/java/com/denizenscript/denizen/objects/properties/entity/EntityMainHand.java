package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.MainHand;

public class EntityMainHand extends EntityProperty<ElementTag> {

    // <--[property]
    // @object EntityTag
    // @name main_hand
    // @input ElementTag
    // @description
    // For mannequins, which hand holds the main-hand item, either "left" or "right". Defaults to right.
    // Only available on Minecraft 1.21.9 and above, where mannequins exist.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public ElementTag getPropertyValue() {
        return new ElementTag(as(Mannequin.class).getMainHand());
    }

    @Override
    public boolean isDefaultValue(ElementTag value) {
        return value.asEnum(MainHand.class) == MainHand.RIGHT;
    }

    @Override
    public void setPropertyValue(ElementTag value, Mechanism mechanism) {
        if (!mechanism.requireEnum(MainHand.class)) {
            return;
        }
        as(Mannequin.class).setMainHand(value.asEnum(MainHand.class));
    }

    @Override
    public String getPropertyId() {
        return "main_hand";
    }

    public static void register() {
        autoRegister("main_hand", EntityMainHand.class, ElementTag.class, false);
    }
}
