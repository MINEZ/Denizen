<div align="center">

# MINEZ Denizen

面向 MINEZ 服务器维护的 [Denizen](https://github.com/DenizenScript/Denizen) Fork。

<a href="../README.md">English</a> ｜
<a href="README_zh-CN.md">简体中文</a>

<br>

<div>
<a href="https://github.com/DenizenScript/Denizen"><img src="https://img.shields.io/badge/upstream-Denizen%201.3.3-1976d2" alt="upstream"></a>
<a href="#受支持的版本"><img src="https://img.shields.io/badge/Minecraft-7%20versions-4caf50" alt="Minecraft"></a>
<a href="../LICENSE.txt"><img src="https://img.shields.io/badge/license-MIT-9e9e9e" alt="license"></a>
</div>

<br>

<a href="https://denizen-meta.minez.cc/">查阅文档</a> ｜
<a href="https://github.com/MINEZ/Denizen/issues">问题反馈</a> ｜
<a href="https://github.com/MINEZ/Denizen/releases">版本发布</a> ｜
<a href="#相对上游的改动">改动一览</a>

</div>

> [!IMPORTANT]
> **本项目是非官方 Fork，与 DenizenScript 团队没有关系，既未经其认可，也不由其提供支持。**
>
> 与本 Fork 有关的一切，请提交至[本仓库的 issue](https://github.com/MINEZ/Denizen/issues)，不要带到上游仓库、官方 Discord 或论坛。
>
> 区分方法是先在未经修改的上游构建上复现：
>
> - **脱离本 Fork 仍会出现** —— 属于上游的问题，照常向上游反馈，不必也不要提及本 Fork。
> - **脱离本 Fork 便不再出现** —— 属于我们的问题，只应提交到这里。

## 关于

本仓库跟随上游 `dev` 分支，仅承载 MINEZ 服务器所需的少量改动。除下文列出的内容外，其余部分与上游完全一致。

### 受支持的版本

与上游一致，仅支持下列特定的 Spigot 版本，其间的其他版本并不受支持：

**1.17.1** · **1.18.2** · **1.19.4** · **1.20.6** · **1.21.11** · **26.1.2** · **26.2**

我们的提交以变基方式跟随上游，历史保持线性：

```bash
git fetch upstream
git rebase upstream/dev
```

变基会重写提交哈希，因此同步后推送需要 `git push origin dev --force-with-lease`。

## 相对上游的改动

各项的具体用法随源码中的 meta 注释一同维护，并自动发布到 **[denizen-meta.minez.cc](https://denizen-meta.minez.cc/)**。下表仅为索引。

| 名称 | 类型 | 改动 |
| --- | --- | --- |
| `<ItemTag.cooking_result[(<type>)]>` | 标签 | 新增 |
| `<ItemTag.cooking_recipe_id[(<type>)]>` | 标签 | 新增 |
| `<ItemTag.cooking_experience[(<type>)]>` | 标签 | 新增 |
| `<ItemTag.cooking_time[(<type>)]>` | 标签 | 新增 |
| `<&head[...]>` | 文本标签 | 新增 |
| `<&sprite[...]>` | 文本标签 | 新增 |
| `dialog` | 脚本容器 | 新增 |
| `showdialog` | 命令 | 新增 |
| `player custom click` | 事件 | 新增 |
| `PlayerTag.show_dialog` | 机制 | 新增 |
| `PlayerTag.close_dialog` | 机制 | 新增 |
| `player respawns` | 事件 | 修改 |
| `projectile launched` | 事件 | 修复 |
| `potion effects modified` | 事件 | 修复 |

**熔炼配方标签。** 上游只提供了按产物查找配方的能力。想按输入反查，只能遍历 `server.recipe_ids` 再用 `server.recipe_items` 的文本做匹配，而后者对多材料输入只会暴露第一个材料——原版玻璃配方同时接受沙子与红沙，红沙因此必然漏判。这组标签在首次查询时构建材料到配方的索引，是否匹配交由原版的 `RecipeChoice#test` 判断，多材料输入与精确匹配输入都能正确处理。

**对话框。** 基于 Paper dialog API 的 `dialog` 脚本容器，支持 `confirm`、`notice`、`list`、`multi` 四种版式，以及 `base`、`bodies`、`inputs`、`buttons` 与用于动态生成内容的 `procedural` 段。依赖 Paper 1.21.6 及以上版本；在更低版本或非 Paper 服务端上这些内容不会被注册，`type: dialog` 容器将无法加载，其余内容不受影响。

本部分衍生自以 Apache 2.0 许可证发布的 [denizen-utilities](https://github.com/isnsest/denizen-utilities)，并沿用其接口，原有 `type: dialog` 脚本无需改动即可迁移。有两处刻意的差异：`exit button` 改为从 `base` 段读取（原实现只读容器根部，导致退出按钮始终不生效），同时兼容旧写法；没有 `script` 段的按钮不再绑定点击动作，点击后仅关闭对话框。

**内联图像。** `<&head[...]>` 与 `<&sprite[...]>` 输出 1.21.9 引入的 object 类型文本组件，可在聊天中内联渲染玩家头像或图集精灵。Denizen 的文本管线建立在已被冻结的 BungeeCord Chat API 之上，会丢弃这一类型的组件，因此本 Fork 自带了对应的组件与序列化器。**需要 1.21.9 及以上的客户端**，低版本客户端不会显示，但也不会报错。

**重生点。** 上游的 `player respawns` 事件只区分是否为床，重生锚与世界出生点无从分辨。现改为完整暴露服务端提供的信息：`<context.spawn_type>`（bed、anchor 或 world）、`<context.reason>`（death、end_portal 或 plugin），以及在床或锚被破坏时为真的 `<context.is_missing_respawn_block>`，并配有 `spawn_type:` 与 `reason:` 两个开关。旧的 `at bed` 与 `elsewhere` 写法仍可使用，但加载时会给出废弃提示。

### 修复

- **`projectile launched` 事件** —— `<context.shooter>` 在弹射物没有射手时（例如由发射器发射）会抛出空指针异常，现改为返回 null。
- **`potion effects modified` 事件** —— `<context.effect_type>` 与 `effect` 开关此前使用 Bukkit 的旧式效果名（如 `SLOW`、`FAST_DIGGING`），现改用现代的键名（`slowness`、`haste`），与 Denizen 其余部分保持一致。

## 构建

需要 JDK 17 及以上版本，并通过 [BuildTools](https://www.spigotmc.org/wiki/buildtools/) 构建全部所列的 Spigot 版本。

```bash
mvn clean package -DBUILD_NUMBER=<上游构建号>.<Fork 修订号> -DBUILD_CLASS=DEV
```

产物位于 `target/`，形如 `Denizen-1.3.3-b7299.4-DEV.jar`。

## 版本号

项目版本号沿用上游，不作改动。Fork 的修订信息记录在构建号中：在 `Denizen-1.3.3-b7299.4-DEV` 中，`7299` 是所基于的上游构建号，末尾的 `.4` 是本 Fork 的修订号。这样既不会与上游的版本号撞车，合并上游时也不会在 `pom.xml` 上产生冲突。

上游项目页面上的下载链接指向官方 CI，**不包含**本 Fork 的改动。

## 相关仓库

| 仓库 | 用途 |
| --- | --- |
| [MINEZ/DenizenCore](https://github.com/MINEZ/DenizenCore) | 核心库，无改动镜像 |
| [MINEZ/Depenizen](https://github.com/MINEZ/Depenizen) | 插件桥接，无改动镜像 |
| [MINEZ/SharpDenizenTools](https://github.com/MINEZ/SharpDenizenTools) | meta 解析与脚本检查的共用库 |
| [MINEZ/DenizenMetaWebsite](https://github.com/MINEZ/DenizenMetaWebsite) | 文档站，部署于 [denizen-meta.minez.cc](https://denizen-meta.minez.cc/) |
| [MINEZ/DenizenVSCode](https://github.com/MINEZ/DenizenVSCode) | VS Code 扩展，构建时对接本 Fork 的 meta |

## 上游

Denizen 由 [DenizenScript 团队](https://denizenscript.com/)开发。学习这门语言本身，仍应前往[新手指南](https://guide.denizenscript.com/)与官方 [Discord](https://discord.gg/Q6pZGSR)——但仅限于上游原本提供的内容，不包括本 Fork 的任何改动。

此处的改动为单一服务器的需求而作，独立维护。请勿将相关讨论带入上游的各个渠道。

## 许可证

Denizen 以 MIT 许可证开源，版权归 The Denizen Script Team 所有。本 Fork 的修改同样适用该许可证。完整条款见 [LICENSE.txt](../LICENSE.txt)。

简而言之：你几乎可以做任何事，但不得就你使用本软件所造成的后果追究任何开发者的责任。

### 第三方代码

对话框部分衍生自 [denizen-utilities](https://github.com/isnsest/denizen-utilities)，版权归 isnsest 所有，以 **Apache License 2.0** 许可。这些文件适用该许可证而非 MIT，文件头部标注了已被修改，清单如下：

- `paper/src/main/java/com/denizenscript/denizen/paper/PaperDialogModule.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/commands/ShowDialogCommand.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/containers/DialogScriptContainer.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/containers/DialogScriptHelper.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/events/PlayerCustomClickScriptEvent.java`

Apache License 2.0 的完整条款见 [licenses/denizen-utilities-LICENSE.txt](../licenses/denizen-utilities-LICENSE.txt)。
