/*
 * Portions of this file are derived from denizen-utilities
 * (https://github.com/isnsest/denizen-utilities), Copyright (c) the denizen-utilities authors,
 * licensed under the Apache License, Version 2.0. A copy of that license is included at
 * licenses/denizen-utilities-LICENSE.txt in this repository.
 *
 * This file has been modified from the original: it was adapted to this project's package
 * layout and registration flow, and its behaviour differs as described in the README.
 */
package com.denizenscript.denizen.paper.containers;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import io.papermc.paper.connection.PlayerCommonConnection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 对话框脚本容器的辅助工具。
 * <p>
 * 对话框打开时会把“这次展示用的配置”记录到 {@link #dialogDataMap} 中，
 * 因为玩家点击按钮时客户端只回传一个 key，服务端需要凭它反查按钮脚本与各输入项的类型。
 * 用 {@link WeakHashMap} 是为了让连接断开后这份数据能自然回收。
 */
@SuppressWarnings("UnstableApiUsage")
public class DialogScriptHelper {

    public static final Map<PlayerCommonConnection, DialogData> dialogDataMap = new WeakHashMap<>();

    public enum InputType {
        TEXT, SINGLE, BOOLEAN, NUMBER
    }

    /** 一次对话框展示的运行时数据。 */
    public static class DialogData {

        public PlayerCommonConnection connection;

        /** 输入项的键到类型的映射，用于点击时按正确类型读取回传值。 */
        public Map<String, InputType> inputs;

        /** inputs / bodies / buttons 三段配置，可能来自静态定义，也可能来自 procedural 生成。 */
        public Map<String, YamlConfiguration> configurationMap;

        public Map<String, InputType> getInputs() {
            return inputs;
        }

        public Map<String, YamlConfiguration> getConfigurationMap() {
            return configurationMap;
        }
    }

    /** 把 procedural 段 determine 出来的 MapTag 转成容器可读的配置结构。 */
    public static YamlConfiguration mapToConfig(MapTag map) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<StringHolder, ObjectTag> entry : map.map.entrySet()) {
            ObjectTag val = entry.getValue();
            if (val instanceof MapTag mapTag) {
                config.contents.put(entry.getKey(), mapToConfig(mapTag).contents);
            }
            else {
                config.contents.put(entry.getKey(), val.getJavaObject());
            }
        }
        return config;
    }

    /**
     * 递归转义 procedural 段中的标签。
     * <p>
     * procedural 段在打开对话框时执行，而 '<context.connection>' 这类点击期标签此时还没有值，
     * 若不转义就会在加载阶段被提前解析掉。按钮的 script 段整体转义，其余位置只转义指定前缀。
     */
    public static Object deeplyEscapeTags(Object obj, String... prefixes) {
        if (obj instanceof String str) {
            return autoEscapeTags(str, prefixes);
        }
        if (obj instanceof StringHolder holder) {
            return new StringHolder(autoEscapeTags(holder.str, prefixes));
        }
        if (obj instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(deeplyEscapeTags(item, prefixes));
            }
            return result;
        }
        if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> newMap = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                String keyStr = k instanceof StringHolder holder ? holder.str : String.valueOf(k);
                if (keyStr.equalsIgnoreCase("script")) {
                    newMap.put(k, escapeAllTags(v));
                }
                else {
                    newMap.put(deeplyEscapeTags(k, prefixes), deeplyEscapeTags(v, prefixes));
                }
            });
            return newMap;
        }
        return obj;
    }

    private static Object escapeAllTags(Object obj) {
        if (obj instanceof String str) {
            return autoEscapeTags(str, "<");
        }
        if (obj instanceof StringHolder holder) {
            return new StringHolder(autoEscapeTags(holder.str, "<"));
        }
        if (obj instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(escapeAllTags(item));
            }
            return result;
        }
        if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> newMap = new LinkedHashMap<>();
            map.forEach((k, v) -> newMap.put(escapeAllTags(k), escapeAllTags(v)));
            return newMap;
        }
        return obj;
    }

    /** 把以指定前缀开头的标签整体替换为 '&lt;' / '&gt;' 转义形式，使其延后到点击时才解析。 */
    public static String autoEscapeTags(String input, String... prefixes) {
        if (input == null || prefixes == null || prefixes.length == 0 || !input.contains("<")) {
            return input;
        }
        StringBuilder result = new StringBuilder(input.length() + 32);
        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (input.charAt(i) != '<') {
                result.append(input.charAt(i));
                continue;
            }
            boolean matched = false;
            for (String prefix : prefixes) {
                if (input.startsWith(prefix, i)) {
                    int depth = 1;
                    int closeIndex = -1;
                    for (int j = i + prefix.length(); j < length; j++) {
                        if (input.charAt(j) == '<') {
                            depth++;
                        }
                        else if (input.charAt(j) == '>') {
                            depth--;
                            if (depth == 0) {
                                closeIndex = j;
                                break;
                            }
                        }
                    }
                    if (closeIndex != -1) {
                        result.append("<&lt>");
                        result.append(input, i + 1, closeIndex);
                        result.append("<&gt>");
                        i = closeIndex;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                result.append(input.charAt(i));
            }
        }
        return result.toString();
    }
}
