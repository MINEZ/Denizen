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

<div>
<img src="https://img.shields.io/badge/1.17.1-81c784" alt="1.17.1">
<img src="https://img.shields.io/badge/1.18.2-81c784" alt="1.18.2">
<img src="https://img.shields.io/badge/1.19.4-81c784" alt="1.19.4">
<img src="https://img.shields.io/badge/1.20.6-81c784" alt="1.20.6">
<img src="https://img.shields.io/badge/1.21.11-81c784" alt="1.21.11">
<img src="https://img.shields.io/badge/26.1.2-81c784" alt="26.1.2">
<img src="https://img.shields.io/badge/26.2-81c784" alt="26.2">
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

与上游一致，仅支持本页顶部列出的这几个 Spigot 版本，其间的其他版本并不受支持。

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
| `cast` | 命令 | 修改 |
| `player respawns` | 事件 | 修改 |
| `PlayerTag.save_data` | 机制 | 新增 |
| `fakespawn` | 命令 | 修改 |
| `attach` | 命令 | 修改 |
| `EntityTag.hide_description` | 属性 | 新增 |
| `EntityTag.description` | 属性 | 新增 |
| `EntityTag.immovable` | 属性 | 新增 |
| `EntityTag.main_hand` | 属性 | 新增 |
| `EntityTag.pose` | 属性 | 新增 |
| 离线玩家的物品栏编辑 | 行为 | 修复 |
| `EntityTag` 中仅适用于生物实体的机制 | 机制 | 修复 |
| `projectile launched` | 事件 | 修复 |
| `potion effects modified` | 事件 | 修复 |
| 重新加入后的假实体跟踪 | 行为 | 修复 |
| 他人视角中的伪装体移动 | 行为 | 修复 |

**熔炼配方标签。** 上游只提供了按产物查找配方的能力。想按输入反查，只能遍历 `server.recipe_ids` 再用 `server.recipe_items` 的文本做匹配，而后者对多材料输入只会暴露第一个材料——原版玻璃配方同时接受沙子与红沙，红沙因此必然漏判。这组标签在首次查询时构建材料到配方的索引，是否匹配交由原版的 `RecipeChoice#test` 判断，多材料输入与精确匹配输入都能正确处理。

**对话框。** 基于 Paper dialog API 的 `dialog` 脚本容器，支持 `confirm`、`notice`、`list`、`multi` 四种版式，以及 `base`、`bodies`、`inputs`、`buttons` 与用于动态生成内容的 `procedural` 段。依赖 Paper 1.21.6 及以上版本；在更低版本或非 Paper 服务端上这些内容不会被注册，`type: dialog` 容器将无法加载，其余内容不受影响。

本部分衍生自以 Apache 2.0 许可证发布的 [denizen-utilities](https://github.com/isnsest/denizen-utilities)，并沿用其接口，原有 `type: dialog` 脚本无需改动即可迁移。有两处刻意的差异：`exit button` 改为从 `base` 段读取（原实现只读容器根部，导致退出按钮始终不生效），同时兼容旧写法；没有 `script` 段的按钮不再绑定点击动作，点击后仅关闭对话框。

**内联图像。** `<&head[...]>` 与 `<&sprite[...]>` 输出 1.21.9 引入的 object 类型文本组件，可在聊天中内联渲染玩家头像或图集精灵。Denizen 的文本管线建立在已被冻结的 BungeeCord Chat API 之上，会丢弃这一类型的组件，因此本 Fork 自带了对应的组件与序列化器。**需要 1.21.9 及以上的客户端**，低版本客户端不会显示，但也不会报错。

**重生点。** 上游的 `player respawns` 事件只区分是否为床，重生锚与世界出生点无从分辨。现改为完整暴露服务端提供的信息：`<context.spawn_type>`（bed、anchor 或 world）、`<context.reason>`（death、end_portal 或 plugin），以及在床或锚被破坏时为真的 `<context.is_missing_respawn_block>`，并配有 `spawn_type:` 与 `reason:` 两个开关。旧的 `at bed` 与 `elsewhere` 写法仍可使用，但加载时会给出废弃提示。

**重复施加的药水效果。** 上游的 `cast` 把 Bukkit 返回的 `false` 一律当作失败并报错，而该返回值仅表示效果表未发生改变。目标身上已有同类且等级不低的效果时，这本就是正常结果——已有效果或是继续生效，或是将新效果存为隐藏效果，待其结束后自行接上。现改为区分两者，仅在效果确实无法施加时才报错。

**按需写出玩家数据。** `PlayerTag.save_data` 可立即将玩家数据写入其存档文件。对离线玩家物品栏所作的改动，原本只在数据离开缓存、玩家登录或服务器关闭时才会写出，其间若发生崩溃便会丢失。

**假实体的默认行为。** 上游的 `fakespawn` 在未给出 `players:` 时只展示给关联玩家。现改为默认展示给假实体所在世界的每一位玩家，并跟随人员变动：只要假实体尚在，其后加入服务器或进入该世界的玩家同样会看到。若要沿用旧行为，显式写明 `players:<player>` 即可。观看者一旦离线或离开该世界便暂停跟踪，待其回来再行恢复，其间本就无从呈现。`duration:` 的默认也一并改了：此前不写表示十秒，现在表示一直保留到被取消或服务器停止。

**假实体的附着。** `attach` 靠改写目标实体的移动数据包来带动被附着者，这对假实体留下了两处缺口：服务端从不向玩家发送其自身的移动，附着到自己身上便自己看不见；而假实体的移动本就只由 Denizen 自行发出。现改为假实体一律走服务端同步。对它们而言这并没有作用于真实实体时的那些副作用——它们不在世界之中，所谓同步不过是改一下坐标——`attach <player.fake_entities> to:<player> offset:0,2,0` 因此无需再写 `sync_server`。同步之后会立即发出移动，而非等假实体自己的定时任务轮转，被附着的玩家本人所见的延迟因此少去一个 tick。剩下的延迟是往返服务端的网络耗时，无从消除；旁人则看不出这份延迟，因为假实体与玩家本体经的是同一条路。

**mannequin 的属性。** 上游 Denizen 完全未涉及此类实体，原版在其上提供的种种设定，脚本一概够不着——经 `disguise ... as:mannequin[...]` 时尤其如此，伪装体由 Denizen 内部创建，任何命令都触及不到。现补上五个属性：`description` 替换名称下方本应显示记分板分数的那一行，不给定内容则恢复默认；`hide_description` 将那一行整个隐去，默认显示的 “NPC” 由此可以去掉；`immovable` 使其不被推动；`main_hand` 更换持物的手；`pose` 设定所取的姿势。均需 Minecraft 1.21.9 及以上版本。其中数项在 Spigot 与 Paper 上的接口互不兼容，故经 Denizen 既有的两侧分派实现。

### 修复

- **离线玩家的物品栏编辑会被静默丢弃。** 离线玩家的物品栏是依其存档数据另行构造的 Bukkit 对象，其上的改动须经一次显式同步才能回到存档数据中，而缓存到期时并不会执行该同步，于是经由 `inventory open` 所作的改动会在约一小时后消失。现于缓存丢弃之前、以及每次关闭此类界面时执行同步；玩家上线时亦会关闭已打开的界面，因为其背后的数据届时已与本人无关。
- **仅适用于生物实体的机制** —— `no_damage_duration`、`oxygen`、`gliding` 等十余个机制未经判断便按生物实体取用，对船、矿车、画一类实体使用时会抛出空指针异常，现改为说明是哪个机制用在了什么实体上。`cast` 命令存在同样的缺陷，一并加以防护。
- **`projectile launched` 事件** —— `<context.shooter>` 在弹射物没有射手时（例如由发射器发射）会抛出空指针异常，现改为返回 null。
- **`potion effects modified` 事件** —— `<context.effect_type>` 与 `effect` 开关此前使用 Bukkit 的旧式效果名（如 `SLOW`、`FAST_DIGGING`），现改用现代的键名（`slowness`、`haste`），与 Denizen 其余部分保持一致。
- **假实体在观看者重新加入后不再移动。** 假实体为每个观看者单独建立跟踪器，而跟踪器绑定于建立时的那条连接。玩家断线后连接即告作废，重新加入时更新仍写往旧连接，实体便停在原处不动。现改为在观看者重新加入服务器或切换世界时重建跟踪器。
- **伪装体在他人视角中停在原地。** 自 1.19 起，伪装的处理会拦下该实体的移动与传送数据包，除末影龙之外一律丢弃，转而重发一次伪装。而重发的生成包用的是伪装实体创建时的位置，该位置从不更新，于是伪装体始终停在伪装发生的地方。现改为放行移动数据包——伪装体的实体编号与真身相同，客户端会直接将其作用于伪装体；重发生成包之前则先将伪装实体移到真身所在，使中途进入视野的玩家也能看到它在正确的位置。
- **伪装者重新加入后，伪装体又停在了原地。** 玩家重新连接后会换得一个新的实体编号，而伪装体的编号定格在其创建之时。于是他人收到的移动数据包指向新编号，其客户端上却只有旧编号的伪装体，伪装体便又一次停下。现改为在被伪装的玩家重新加入时弃掉旧的伪装体，待下一次拦下生成数据包时按其当下的编号重建。

## 构建

需要 JDK 17 及以上版本，并通过 [BuildTools](https://www.spigotmc.org/wiki/buildtools/) 构建全部所列的 Spigot 版本。

```bash
mvn clean package -DBUILD_NUMBER=<上游构建号>.<Fork 修订号> -DBUILD_CLASS=DEV
```

产物位于 `target/`，形如 `Denizen-1.3.3-b7302.1-DEV.jar`。

## 版本号

项目版本号沿用上游，不作改动。Fork 的修订信息记录在构建号中：在 `Denizen-1.3.3-b7302.1-DEV` 中，`7302` 是所基于的上游构建号，末尾的 `.1` 是本 Fork 的修订号。这样既不会与上游的版本号撞车，合并上游时也不会在 `pom.xml` 上产生冲突。

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
