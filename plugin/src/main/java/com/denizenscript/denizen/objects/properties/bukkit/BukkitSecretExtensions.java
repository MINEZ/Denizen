package com.denizenscript.denizen.objects.properties.bukkit;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.SecretTag;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.HexFormat;

public class BukkitSecretExtensions {

    public static void register() {

        // <--[tag]
        // @attribute <SecretTag.ed25519_sign[<text>]>
        // @returns ElementTag
        // @description
        // Signs the given text (as UTF-8) with Ed25519, using the secret value as the private key seed, and returns the 64-byte signature as lowercase hex.
        // The seed is the secret's UTF-8 bytes, repeated until there are at least 32 bytes and then cut to 32, which is how QQ official bots derive their key from the bot secret.
        // A secret of exactly 32 bytes is used as-is.
        // Ed25519 signatures are deterministic, so a signature received from someone holding the same key can be checked by signing the same text and comparing the results.
        // The secret value itself is never exposed.
        // @example
        // # Answers the callback URL check of a QQ official bot.
        // - define signature <secret[qq_bot_secret].ed25519_sign[<[event_ts]><[plain_token]>]>
        // -->
        SecretTag.tagProcessor.registerTag(ElementTag.class, ElementTag.class, "ed25519_sign", (attribute, object, text) -> {
            String value = object.getValue();
            if (value == null || value.isEmpty()) {
                attribute.echoError("Secret '" + object.key + "' is not defined in 'secrets.secret' or is empty.");
                return null;
            }
            byte[] secret = value.getBytes(StandardCharsets.UTF_8);
            byte[] seed = new byte[32];
            for (int i = 0; i < seed.length; i++) {
                seed[i] = secret[i % secret.length];
            }
            try {
                PrivateKey key = KeyFactory.getInstance("Ed25519").generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, seed));
                Signature signature = Signature.getInstance("Ed25519");
                signature.initSign(key);
                signature.update(text.asString().getBytes(StandardCharsets.UTF_8));
                return new ElementTag(HexFormat.of().formatHex(signature.sign()));
            }
            catch (Exception ex) {
                attribute.echoError("Failed to sign with secret '" + object.key + "': " + ex.getMessage());
                return null;
            }
        });
    }
}
