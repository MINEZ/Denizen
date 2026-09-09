package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Mannequin;

public class EntityHideDescription extends EntityProperty<ElementTag> {

    // <--[property]
    // @object EntityTag
    // @name hide_description
    // @input ElementTag(Boolean)
    // @description
    // For mannequins, whether the description shown where a player's below-name score would be is hidden.
    // Mannequins show "NPC" there unless a description is set or this is enabled.
    // Only available on Minecraft 1.21.9 and above, where mannequins exist.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public ElementTag getPropertyValue() {
        return new ElementTag(PaperAPITools.instance.isMannequinDescriptionHidden(getEntity()));
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
        PaperAPITools.instance.setMannequinDescriptionHidden(getEntity(), value.asBoolean());
    }

    @Override
    public String getPropertyId() {
        return "hide_description";
    }

    public static void register() {
        autoRegister("hide_description", EntityHideDescription.class, ElementTag.class, false);
    }
}
