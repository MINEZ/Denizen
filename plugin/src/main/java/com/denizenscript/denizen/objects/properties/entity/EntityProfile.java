package com.denizenscript.denizen.objects.properties.entity;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.PaperAPITools;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.MapTag;
import org.bukkit.entity.Mannequin;

public class EntityProfile extends EntityProperty<MapTag> {

    // <--[property]
    // @object EntityTag
    // @name profile
    // @input MapTag
    // @description
    // For mannequins, the player profile the entity wears.
    // The map may hold any of the following keys:
    // "name": the name of the player whose skin to wear.
    // "uuid": the UUID of the player whose skin to wear.
    // "texture": a base64 texture blob, of the sort used by player heads.
    // "texture_signature": the signature for that texture blob, if it has one.
    // "body": a texture key such as "minecraft:my_pack/guard", overriding the body texture.
    // "cape": a texture key overriding the cape texture.
    // "elytra": a texture key overriding the elytra texture.
    // "model": either CLASSIC or SLIM, the arm width of the skin.
    // Giving only a name or a uuid leaves the lookup to the client, so the skin appears a moment later rather than at once.
    // Giving a texture blob or a texture key applies immediately, with no lookup at all.
    // At least one key must be given; there is no input that returns the profile to its default.
    // Only supported on Paper servers, and only on Minecraft 1.21.9 and above, where mannequins exist.
    //
    // @mechanism-example
    // # Wear a specific player's skin, looked up by the client.
    // - adjust <[mannequin]> profile:[name=Notch]
    //
    // @mechanism-example
    // # Wear a texture straight out of a resource pack, with slim arms.
    // - adjust <[mannequin]> profile:[body=my_pack:entity/guard;model=SLIM]
    //
    // -->

    public static boolean describes(EntityTag entity) {
        return entity.getBukkitEntity() instanceof Mannequin;
    }

    @Override
    public MapTag getPropertyValue() {
        return PaperAPITools.instance.getMannequinProfile(getEntity());
    }

    @Override
    public boolean isDefaultValue(MapTag value) {
        return value == null;
    }

    @Override
    public void setPropertyValue(MapTag value, Mechanism mechanism) {
        try {
            PaperAPITools.instance.setMannequinProfile(getEntity(), value);
        }
        catch (UnsupportedOperationException ex) {
            mechanism.echoError("Mannequin profiles can only be set on a Paper server.");
        }
        catch (IllegalArgumentException ex) {
            mechanism.echoError("Invalid mannequin profile: " + ex.getMessage());
        }
    }

    @Override
    public String getPropertyId() {
        return "profile";
    }

    public static void register() {
        autoRegister("profile", EntityProfile.class, MapTag.class, false);
    }
}
