/*
 * Portions of this file are derived from denizen-utilities
 * (https://github.com/isnsest/denizen-utilities), Copyright (c) isnsest,
 * licensed under the Apache License, Version 2.0. A copy of that license is included at
 * licenses/denizen-utilities-LICENSE.txt in this repository.
 *
 * This file has been modified from the original: it was adapted to this project's package
 * layout and registration flow, and its behaviour differs as described in the README.
 */
package com.denizenscript.denizen.paper.events;

import com.denizenscript.denizen.paper.containers.DialogScriptContainer;
import com.denizenscript.denizen.paper.containers.DialogScriptHelper;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.JavaReflectedObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;

public class PlayerCustomClickScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // player custom click
    //
    // @Group Player
    //
    // @Plugin Paper
    //
    // @Switch button_id:<id> to only process the event if the clicked button has the specified ID.
    // @Switch namespace:<namespace> to only process the event if the click action has the specified namespace.
    //
    // @Triggers when a player clicks a custom click action, most commonly a button in a dialog.
    // Note that buttons defined by a dialog script container run their own 'script' section as well,
    // see <@link language Dialog Buttons>.
    //
    // @Context
    // <context.button_id> returns the ID of the clicked button.
    // <player> returns the player who clicked, when the click came from an in-game player.
    // <context.namespace> returns the namespace of the click action, 'denizen' for dialog script buttons.
    // <context.inputs> returns a MapTag of all input values in the dialog.
    // <context.[input_id]> returns the value of a specific input field.
    // <context.reflect_event> returns a JavaReflectedObjectTag of the internal event.
    //
    // -->

    public static PlayerCustomClickScriptEvent instance;

    public PlayerCustomClickEvent event;

    public DialogScriptHelper.DialogData dialogData;

    public String buttonId;

    public PlayerCustomClickScriptEvent() {
        instance = this;
        registerCouldMatcher("player custom click");
        registerSwitches("button_id", "namespace");
    }

    public record ButtonPathInfo(String scriptName, String buttonPath, String buttonId) {
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "button_id", buttonId)) {
            return false;
        }
        if (event == null || !runGenericSwitchCheck(path, "namespace", event.getIdentifier().namespace())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "inputs" -> getInputs(dialogData, event.getDialogResponseView());
            case "reflect_event" -> new JavaReflectedObjectTag(event);
            case "namespace" -> new ElementTag(event.getIdentifier().namespace());
            case "button_id" -> new ElementTag(buttonId);
            default -> {
                ObjectTag value = getResponseValue(dialogData, event.getDialogResponseView(), name);
                yield value != null ? value : super.getContext(name);
            }
        };
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        if (event != null && event.getCommonConnection() instanceof PlayerGameConnection gameConnection) {
            return new BukkitScriptEntryData(gameConnection.getPlayer());
        }
        return super.getScriptEntryData();
    }

    /**
     * 客户端点击后只回传一个 key，这里先触发世界脚本事件，
     * 再按 key 反查对话框容器中对应按钮的 script 段并执行。
     */
    public static class DialogEvents implements Listener {

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            DialogScriptHelper.dialogDataMap.remove(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void onDialogClick(PlayerCustomClickEvent event) {
            ButtonPathInfo pathInfo = parseButtonKey(event.getIdentifier());
            DialogScriptHelper.DialogData dialogData = DialogScriptHelper.getDialogData(event.getCommonConnection());
            instance.event = event;
            instance.dialogData = dialogData;
            instance.buttonId = pathInfo.buttonId();
            instance.fire();
            if (pathInfo.scriptName() == null) {
                return;
            }
            DialogScriptContainer container = ScriptRegistry.getScriptContainerAs(pathInfo.scriptName(), DialogScriptContainer.class);
            if (container == null) {
                Debug.echoError("Dialog button click refers to dialog script '" + pathInfo.scriptName() + "', which no longer exists.");
                return;
            }
            YamlConfiguration buttonSection = getButtonSection(container, dialogData, pathInfo.buttonPath());
            if (buttonSection == null) {
                Debug.echoError("Dialog script '" + pathInfo.scriptName() + "' has no button at '" + pathInfo.buttonPath() + "' to run for a click.");
                return;
            }
            BukkitScriptEntryData entryData = event.getCommonConnection() instanceof PlayerGameConnection gameConnection
                    ? new BukkitScriptEntryData(gameConnection.getPlayer())
                    : new BukkitScriptEntryData(null, null);
            List<ScriptEntry> entries = container.getEntries(buttonSection, entryData, "script");
            if (entries == null || entries.isEmpty()) {
                Debug.echoError("Dialog script '" + pathInfo.scriptName() + "' has a button at '" + pathInfo.buttonPath() + "' whose script section is empty or failed to load.");
                return;
            }
            InstantQueue queue = new InstantQueue(container.getName());
            queue.addEntries(entries);
            queue.setContextSource(name -> switch (name) {
                case "button_id" -> new ElementTag(pathInfo.buttonId());
                case "inputs" -> getInputs(dialogData, event.getDialogResponseView());
                case "namespace" -> new ElementTag(event.getIdentifier().namespace());
                default -> getResponseValue(dialogData, event.getDialogResponseView(), name);
            });
            if (dialogData != null && dialogData.definitions != null) {
                for (Map.Entry<StringHolder, ObjectTag> definition : dialogData.definitions.map.entrySet()) {
                    queue.addDefinition(definition.getKey().str, definition.getValue());
                }
            }
            queue.start(true);
        }
    }

    public static ObjectTag getResponseValue(DialogScriptHelper.DialogData dialogData, DialogResponseView responseView, String name) {
        if (responseView == null) {
            return null;
        }
        if (dialogData != null && dialogData.getInputs() != null) {
            DialogScriptHelper.InputType type = dialogData.getInputs().get(name);
            if (type != null) {
                return switch (type) {
                    case TEXT, SINGLE -> {
                        String text = responseView.getText(name);
                        yield text != null ? new ElementTag(text) : null;
                    }
                    case BOOLEAN -> new ElementTag(Boolean.TRUE.equals(responseView.getBoolean(name)));
                    case NUMBER -> {
                        Float value = responseView.getFloat(name);
                        yield value != null ? new ElementTag(value) : null;
                    }
                };
            }
        }
        Float floatValue = responseView.getFloat(name);
        if (floatValue != null) {
            return new ElementTag(floatValue);
        }
        Boolean boolValue = responseView.getBoolean(name);
        if (boolValue != null) {
            return new ElementTag(boolValue);
        }
        String text = responseView.getText(name);
        if (text != null) {
            return new ElementTag(text);
        }
        return null;
    }

    /** 解析形如 'denizen:script_name/buttons.submit' 的 key。 */
    public static ButtonPathInfo parseButtonKey(Key key) {
        String value = key.value();
        if (!key.namespace().equals("denizen") || !value.contains("/")) {
            int lastDot = value.lastIndexOf('.');
            return new ButtonPathInfo(null, value, lastDot >= 0 ? value.substring(lastDot + 1) : value);
        }
        int slashIndex = value.indexOf('/');
        String scriptName = value.substring(0, slashIndex);
        String buttonPath = value.substring(slashIndex + 1);
        int lastDot = buttonPath.lastIndexOf('.');
        return new ButtonPathInfo(scriptName, buttonPath, lastDot >= 0 ? buttonPath.substring(lastDot + 1) : buttonPath);
    }

    /**
     * 按钮路径可能指向 procedural 段动态生成的内容，那只存在于本次展示的数据中；
     * 也可能就是容器里写死的段落，此时无需依赖本次展示的数据即可取到。
     */
    public static YamlConfiguration getButtonSection(DialogScriptContainer container, DialogScriptHelper.DialogData dialogData, String buttonPath) {
        int firstDot = buttonPath.indexOf('.');
        if (firstDot >= 0) {
            if (dialogData != null) {
                YamlConfiguration parentSection = dialogData.getConfigurationMap().get(buttonPath.substring(0, firstDot));
                if (parentSection != null) {
                    YamlConfiguration section = parentSection.getConfigurationSection(buttonPath.substring(firstDot + 1));
                    if (section != null) {
                        return section;
                    }
                }
            }
            return container.getConfigurationSection(buttonPath);
        }
        YamlConfiguration section = dialogData == null ? null : dialogData.getConfigurationMap().get(buttonPath);
        return section != null ? section : container.getConfigurationSection(buttonPath);
    }

    public static MapTag getInputs(DialogScriptHelper.DialogData dialogData, DialogResponseView responseView) {
        MapTag inputs = new MapTag();
        if (dialogData == null || dialogData.getInputs() == null || responseView == null) {
            return inputs;
        }
        for (String key : dialogData.getInputs().keySet()) {
            ObjectTag value = getResponseValue(dialogData, responseView, key);
            if (value != null) {
                inputs.putObject(key, value);
            }
        }
        return inputs;
    }
}
