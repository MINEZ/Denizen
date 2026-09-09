package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.entity.Mannequin;

public class EntityDescription extends EntityProperty<ElementTag> {

    // <--[property]
    // @object EntityTag
    // @name description
    // @input ElementTag
    // @description
    // For mannequins, the description shown where a player's below-name score would go.
    // Mannequins show "NPC" there when no description is set. Provide no input to return to that default.
    // Use <@link property EntityTag.hide_description> to show nothing there at all.
    // Only available on Minecraft 1.21.9 and above, where mannequins exist.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public ElementTag getPropertyValue() {
        String description = as(Mannequin.class).getDescription();
        return description == null ? null : new ElementTag(description, true);
    }

    @Override
    public boolean isDefaultValue(ElementTag value) {
        return value == null || value.asString().isEmpty();
    }

    @Override
    public void setPropertyValue(ElementTag value, Mechanism mechanism) {
        String description = value == null ? null : CoreUtilities.clearNBSPs(value.asString());
        as(Mannequin.class).setDescription(description == null || description.isEmpty() ? null : description);
    }

    @Override
    public String getPropertyId() {
        return "description";
    }

    public static void register() {
        autoRegister("description", EntityDescription.class, ElementTag.class, false);
    }
}
