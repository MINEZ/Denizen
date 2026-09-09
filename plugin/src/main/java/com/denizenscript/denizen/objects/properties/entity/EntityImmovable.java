package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Mannequin;

public class EntityImmovable extends EntityProperty<ElementTag> {

    // <--[property]
    // @object EntityTag
    // @name immovable
    // @input ElementTag(Boolean)
    // @description
    // For mannequins, whether the entity refuses to be moved or pushed.
    // Only available on Minecraft 1.21.9 and above, where mannequins exist.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public ElementTag getPropertyValue() {
        return new ElementTag(as(Mannequin.class).isImmovable());
    }

    @Override
    public boolean isDefaultValue(ElementTag value) {
        return !value.asBoolean();
    }

    @Override
    public void setPropertyValue(ElementTag value, Mechanism mechanism) {
        if (!mechanism.requireBoolean()) {
            return;
        }
        as(Mannequin.class).setImmovable(value.asBoolean());
    }

    @Override
    public String getPropertyId() {
        return "immovable";
    }

    public static void register() {
        autoRegister("immovable", EntityImmovable.class, ElementTag.class, false);
    }
}
