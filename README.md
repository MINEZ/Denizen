The Denizen Scripting Language - Spigot Impl
--------------------------------------------

An implementation of the Denizen Scripting Language for Spigot servers, with strong Citizens interlinks to emphasize the power of using Denizen with NPCs!

**Version 1.3.3**: Compatible with Spigot 1.17.1, 1.18.2, 1.19.4, 1.20.6, 1.21.11, 26.1.2, and 26.2!

**Learn about Denizen from the Beginner's guide:** https://guide.denizenscript.com/guides/background/index.html

### 关于本 Fork

本仓库是[官方 Denizen](https://github.com/DenizenScript/Denizen) 的 Fork，用于承载 MineZ 服务器所需的改动。除本节列出的内容外，其余部分与上游保持一致。

版本号沿用上游，Fork 的修订信息记录在构建号中，形如 `Denizen-1.3.3-b7286.1-DEV`：其中 `7286` 是所基于的上游构建号，末尾的 `.1` 是本 Fork 的修订号。下方的下载链接指向官方 CI，不包含本 Fork 的改动，需自行构建：

```
mvn clean package -DBUILD_NUMBER=<上游构建号>.<Fork 修订号> -DBUILD_CLASS=DEV
```

本 Fork 的提交以变基方式跟随上游 `dev` 分支，历史保持线性：

```
git fetch upstream
git rebase upstream/dev
```

#### 新增功能

**熔炼配方查询标签。** 一组 `ItemTag` 标签，用于按输入物品反查熔炼类配方：

| 标签 | 返回 | 说明 |
| --- | --- | --- |
| `<ItemTag.cooking_result[(<type>)]>` | ItemTag | 烧炼产物 |
| `<ItemTag.cooking_recipe_id[(<type>)]>` | ElementTag | 配方 ID，形如 `minecraft:glass` |
| `<ItemTag.cooking_experience[(<type>)]>` | ElementTag(Decimal) | 烧炼所得经验 |
| `<ItemTag.cooking_time[(<type>)]>` | DurationTag | 烧炼耗时 |

`type` 可取 `furnace`（默认）、`blasting`、`smoking`、`campfire`，以及不限炉子类型的 `cooking`；查不到配方时一律返回 null。

上游只提供了按产物查配方的 API（`Bukkit.getRecipesFor`、`ItemTag.recipe_ids`），脚本想反过来查，只能遍历 `server.recipe_ids` 再用 `server.recipe_items` 的文本做近似匹配。而 `server.recipe_items` 对多材料输入只会暴露第一个材料——例如玻璃的输入同时包含沙子与红沙，红沙必然漏判——遍历本身也无法承受“每破坏一个方块查一次”的调用频率。本 Fork 改为在首次查询时构建一份材料到配方的索引，是否匹配则交由原版的 `RecipeChoice#test` 判断，多材料输入与精确匹配输入都能正确处理。

索引会在服务器加载与 Denizen 脚本重载时失效重建。注意 `/minecraft:reload`（数据包重载）不会触发重建，此时需手动执行一次 `/denizen reload scripts`。

#### 修复与调整

- **`projectile launched` 事件**：`<context.shooter>` 在弹射物没有射手时（例如由发射器发射）会抛出空指针异常，现改为返回 null。
- **`potion effects modified` 事件**：`<context.effect_type>` 与 `effect` 开关此前使用 Bukkit 的旧式效果名（如 `SLOW`、`FAST_DIGGING`），现改用现代的键名（如 `slowness`、`haste`），与 Denizen 其余部分的命名保持一致。

#### Download Links:

- **Release builds**: https://ci.citizensnpcs.co/job/Denizen/
- **Developmental builds**: https://ci.citizensnpcs.co/job/Denizen_Developmental/
- **SpigotMC - VERY SLOW releases**: https://www.spigotmc.org/resources/denizen.21039/

#### Need help using Denizen? Try one of these places:

- **Discord** - chat room (Modern, strongly recommended): https://discord.gg/Q6pZGSR
- **Denizen Home Page** - a link directory (Modern): https://denizenscript.com/
- **Forum and script sharing** (Modern): https://forum.denizenscript.com/
- **Meta Documentation** - command/tag/event/etc. search (Modern): https://meta.denizenscript.com/
- **Beginner's Guide** - text form (Modern): https://guide.denizenscript.com/

#### Also check out:

- **Citizens2 (NPC support)**: https://github.com/CitizensDev/Citizens2/
- **Depenizen (Other plugin support)**: https://github.com/DenizenScript/Depenizen
- **dDiscordBot (Adds a Discord bot to Denizen)**: https://github.com/DenizenScript/dDiscordBot
- **DenizenCore (Our core, needed for building)**: https://github.com/DenizenScript/Denizen-Core
- **DenizenVSCode (extension for writing Denizen scripts in VS Code)**: https://github.com/DenizenScript/DenizenVSCode

### Building

- Built against JDK 17, using maven `pom.xml` as project file.
- Requires building all listed versions of Spigot via Spigot BuildTools: https://www.spigotmc.org/wiki/buildtools/

### Maven

```xml
    <repository>
        <id>citizens-repo</id>
        <url>https://maven.citizensnpcs.co/repo</url>
    </repository>
    <dependencies>
        <dependency>
            <groupId>com.denizenscript</groupId>
            <artifactId>denizen</artifactId>
            <version>1.3.3-SNAPSHOT</version>
            <type>jar</type>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
```

### Licensing pre-note:

This is an open source project, provided entirely freely, for everyone to use and contribute to.

If you make any changes that could benefit the community as a whole, please contribute upstream.

### The short of the license is:

You can do basically whatever you want, except you may not hold any developer liable for what you do with the software.

### Previous License

Copyright (C) 2012-2013 Aufdemrand, All Rights Reserved.

Copyright (C) 2013-2019 The Denizen Script Team, All Rights Reserved.

### The long version of the license follows:

The MIT License (MIT)

Copyright (c) 2019-2026 The Denizen Script Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
