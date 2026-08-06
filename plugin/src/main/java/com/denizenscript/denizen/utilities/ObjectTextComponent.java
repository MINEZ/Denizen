package com.denizenscript.denizen.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.BaseComponentSerializer;
import net.md_5.bungee.chat.VersionedComponentSerializer;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * 1.21.9 引入的 object 类型文本组件，可在文本中内联显示图集精灵或玩家头像。
 * <p>
 * BungeeCord 的 Chat API 已被冻结（其 ChatVersion 停在 V1_21_5），不认识这一类型，
 * 因此这里自建组件并配套序列化器，由 {@link HoverFormatHelper} 注入到序列化用的 gson 中。
 * <p>
 * 字段与 JSON 的对应关系依据 Adventure 的序列化实现确定：不写 type 与 object 字段，
 * 由客户端按已有字段自行判定；玩家 UUID 写成四个 int 组成的数组；命名空间 ID 一律补全。
 */
public class ObjectTextComponent extends BaseComponent {

    /** 玩家头像的三种来源，互斥。 */
    public enum PlayerSource {
        NAME, ID, TEXTURE
    }

    /** 图集 ID，为空表示使用客户端默认的 minecraft:blocks。 */
    public String atlas;

    /** 精灵 ID。 */
    public String sprite;

    /** 为 true 时表示玩家头像，否则表示图集精灵。 */
    public boolean isPlayerHead;

    public PlayerSource playerSource;

    public String playerValue;

    /** 是否绘制皮肤的帽子层。 */
    public boolean hat = true;

    public ObjectTextComponent() {
    }

    @Override
    public BaseComponent duplicate() {
        ObjectTextComponent result = new ObjectTextComponent();
        result.copyFormatting(this);
        result.atlas = atlas;
        result.sprite = sprite;
        result.isPlayerHead = isPlayerHead;
        result.playerSource = playerSource;
        result.playerValue = playerValue;
        result.hat = hat;
        return result;
    }

    /** 玩家 UUID 在文本组件中以四个 int 的数组表示，而非字符串。 */
    public static JsonArray uuidToIntArray(UUID id) {
        JsonArray array = new JsonArray();
        array.add((int) (id.getMostSignificantBits() >> 32));
        array.add((int) (id.getMostSignificantBits() & 0xFFFFFFFFL));
        array.add((int) (id.getLeastSignificantBits() >> 32));
        array.add((int) (id.getLeastSignificantBits() & 0xFFFFFFFFL));
        return array;
    }

    /** 补全命名空间，与 Adventure 的输出保持一致。 */
    public static String fullKey(String key) {
        return key == null || key.indexOf(':') != -1 ? key : "minecraft:" + key;
    }

    public static class Serializer extends BaseComponentSerializer implements JsonSerializer<ObjectTextComponent> {

        public Serializer(VersionedComponentSerializer versionedSerializer) {
            super(versionedSerializer);
        }

        @Override
        public JsonElement serialize(ObjectTextComponent src, Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            serialize(object, src, context);
            if (src.isPlayerHead) {
                if (src.playerSource == PlayerSource.NAME || src.playerSource == null) {
                    object.addProperty("player", src.playerValue == null ? "" : src.playerValue);
                }
                else {
                    JsonObject profile = new JsonObject();
                    if (src.playerSource == PlayerSource.ID) {
                        UUID id = null;
                        try {
                            id = UUID.fromString(src.playerValue);
                        }
                        catch (IllegalArgumentException ex) {
                            // 无效的 UUID 退化为按名称处理，避免发出客户端无法解析的组件。
                        }
                        if (id == null) {
                            profile.addProperty("name", src.playerValue == null ? "" : src.playerValue);
                        }
                        else {
                            profile.add("id", uuidToIntArray(id));
                        }
                    }
                    else {
                        profile.addProperty("texture", fullKey(src.playerValue));
                    }
                    object.add("player", profile);
                }
                object.addProperty("hat", src.hat);
            }
            else {
                if (src.atlas != null && !src.atlas.isEmpty()) {
                    object.addProperty("atlas", fullKey(src.atlas));
                }
                object.addProperty("sprite", fullKey(src.sprite));
            }
            return object;
        }
    }
}
