package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Pose;

public class EntityMannequinPose extends EntityProperty<ElementTag> {

    // <--[property]
    // @object EntityTag
    // @name pose
    // @input ElementTag
    // @description
    // For mannequins, the pose the entity is held in.
    // Accepts STANDING, SNEAKING, SWIMMING, FALL_FLYING, or SLEEPING. Defaults to standing.
    // Only available on Minecraft 1.21.9 and above, where mannequins exist.
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public ElementTag getPropertyValue() {
        return new ElementTag(getEntity().getPose());
    }

    @Override
    public boolean isDefaultValue(ElementTag value) {
        return value.asEnum(Pose.class) == Pose.STANDING;
    }

    @Override
    public void setPropertyValue(ElementTag value, Mechanism mechanism) {
        if (!mechanism.requireEnum(Pose.class)) {
            return;
        }
        Pose pose = value.asEnum(Pose.class);
        try {
            PaperAPITools.instance.setMannequinPose(getEntity(), pose);
        }
        catch (IllegalArgumentException ex) {
            mechanism.echoError("Pose '" + pose.name() + "' cannot be used on a mannequin: only STANDING, SNEAKING, SWIMMING, FALL_FLYING, and SLEEPING are valid.");
        }
    }

    @Override
    public String getPropertyId() {
        return "pose";
    }

    public static void register() {
        autoRegister("pose", EntityMannequinPose.class, ElementTag.class, false);
    }
}
