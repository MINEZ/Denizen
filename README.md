<div align="center">

# MINEZ Denizen

A fork of [Denizen](https://github.com/DenizenScript/Denizen) maintained for the MINEZ server.

<a href="README.md">English</a> ｜
<a href="docs/README_zh-CN.md">简体中文</a>

<br>

<div>
<a href="https://github.com/DenizenScript/Denizen"><img src="https://img.shields.io/badge/upstream-Denizen%201.3.3-1976d2" alt="upstream"></a>
<a href="#supported-versions"><img src="https://img.shields.io/badge/Minecraft-7%20versions-4caf50" alt="Minecraft"></a>
<a href="LICENSE.txt"><img src="https://img.shields.io/badge/license-MIT-9e9e9e" alt="license"></a>
</div>

<br>

<a href="https://denizen-meta.minez.cc/">Documentation</a> ｜
<a href="https://github.com/MINEZ/Denizen/issues">Issue Tracker</a> ｜
<a href="https://github.com/MINEZ/Denizen/releases">Releases</a> ｜
<a href="#changes-from-upstream">What's Changed</a>

</div>

> [!IMPORTANT]
> **This is an unofficial fork and is not affiliated with, endorsed by, or supported by the DenizenScript team.**
>
> Never take anything about this fork to the upstream repository, the official Discord, or the forums. Use [this repository's issue tracker](https://github.com/MINEZ/Denizen/issues) instead.
>
> To tell the two apart, reproduce the problem on an unmodified upstream build first:
>
> - **It still happens without this fork** — an upstream problem. Report it upstream as you normally would, with no mention of this fork.
> - **It does not happen without this fork** — ours. It belongs here, and only here.

## About

This repository tracks the upstream `dev` branch and adds a small set of changes needed by the MINEZ server. Everything not listed below is identical to upstream.

### Supported Versions

Following upstream, only these specific Spigot versions are supported — not the ranges between them:

**1.17.1** · **1.18.2** · **1.19.4** · **1.20.6** · **1.21.11** · **26.1.2** · **26.2**

Our commits follow upstream by rebase, so the history stays linear:

```bash
git fetch upstream
git rebase upstream/dev
```

Because rebasing rewrites commit hashes, pushing after a sync requires `git push origin dev --force-with-lease`.

## Changes from Upstream

Usage documentation lives in the meta comments in the source and is published automatically at **[denizen-meta.minez.cc](https://denizen-meta.minez.cc/)**. The table below is only an index.

| Name | Type | Change |
| --- | --- | --- |
| `<ItemTag.cooking_result[(<type>)]>` | Tag | Added |
| `<ItemTag.cooking_recipe_id[(<type>)]>` | Tag | Added |
| `<ItemTag.cooking_experience[(<type>)]>` | Tag | Added |
| `<ItemTag.cooking_time[(<type>)]>` | Tag | Added |
| `<&head[...]>` | Text tag | Added |
| `<&sprite[...]>` | Text tag | Added |
| `dialog` | Script container | Added |
| `showdialog` | Command | Added |
| `player custom click` | Event | Added |
| `PlayerTag.show_dialog` | Mechanism | Added |
| `PlayerTag.close_dialog` | Mechanism | Added |
| `cast` | Command | Changed |
| Living-entity mechanisms | Mechanism | Fixed |
| `player respawns` | Event | Changed |
| `projectile launched` | Event | Fixed |
| `potion effects modified` | Event | Fixed |

**Cooking recipe tags.** Upstream only offers recipe lookup by result. Looking one up by input meant iterating `server.recipe_ids` and matching against the text of `server.recipe_items`, which exposes just the first material of a multi-material input — the vanilla glass recipe accepts both sand and red sand, so red sand was always missed. These tags build a material-to-recipe index on first use and let vanilla's own `RecipeChoice#test` decide matches, so multi-material and exact-match inputs both work.

**Dialogs.** A `dialog` script container backed by Paper's dialog API, covering the `confirm`, `notice`, `list` and `multi` layouts, along with `base`, `bodies`, `inputs`, `buttons` and a `procedural` section for building content dynamically. Requires Paper 1.21.6 or newer; on older or non-Paper servers these are not registered and `type: dialog` containers will fail to load, while everything else is unaffected.

This subsystem is derived from [denizen-utilities](https://github.com/isnsest/denizen-utilities), which is licensed under the Apache License 2.0, and keeps its interface so existing `type: dialog` scripts migrate unchanged. Two deliberate differences: `exit button` is now read from the `base` section (the original only read the container root, so exit buttons never took effect), with the old placement still accepted; and a button with no `script` section no longer binds a click action, so clicking it simply closes the dialog.

**Inline images.** `<&head[...]>` and `<&sprite[...]>` emit the object text components added in 1.21.9, rendering a player face or an atlas sprite inline in chat. Denizen's text pipeline is built on the BungeeCord Chat API, which is frozen and drops this component type, so the fork carries its own component and serializer. **Requires a 1.21.9+ client** — older clients render nothing, without erroring.

**Respawn points.** Upstream's `player respawns` event only tells you whether the respawn point was a bed, so respawn anchors are indistinguishable from the world spawn. The event now carries the full information the server provides — `<context.spawn_type>` (bed, anchor or world), `<context.reason>` (death, end_portal or plugin), and `<context.is_missing_respawn_block>` for when a bed or anchor was destroyed — with matching `spawn_type:` and `reason:` switches. The old `at bed` and `elsewhere` forms still work but warn on load.

**Redundant potion effects.** Upstream's `cast` treats a `false` return from Bukkit as a failure and reports an error, but that return only means the effect table did not change — which is the normal outcome when the target already has an equal or stronger effect. The command now tells the two apart and only reports an error when the effect genuinely could not be applied.

### Fixes

- **Mechanisms that only apply to living entities** — `no_damage_duration`, `oxygen`, `gliding` and ten others read the entity as a living one without checking first, so using them on a boat, minecart or painting threw a raw null pointer exception. They now report which mechanism was misapplied and to what. The `cast` command had the same flaw and is guarded too.
- **`projectile launched` event** — `<context.shooter>` threw a null pointer exception when the projectile had no shooter, such as one fired by a dispenser. It now returns null.
- **`potion effects modified` event** — `<context.effect_type>` and the `effect` switch used Bukkit's legacy effect names such as `SLOW` and `FAST_DIGGING`. They now use the modern keys, `slowness` and `haste`, matching the rest of Denizen.

## Building

Requires JDK 17+ and all listed Spigot versions installed via [BuildTools](https://www.spigotmc.org/wiki/buildtools/).

```bash
mvn clean package -DBUILD_NUMBER=<upstream build>.<fork revision> -DBUILD_CLASS=DEV
```

The result lands in `target/`, for example `Denizen-1.3.3-b7299.4-DEV.jar`.

## Versioning

The project version follows upstream unchanged. Fork revisions are recorded in the build number instead: in `Denizen-1.3.3-b7299.4-DEV`, `7299` is the upstream build this is based on and the trailing `.4` is the fork revision. This keeps our version numbers from colliding with upstream releases and avoids conflicts in `pom.xml` when merging.

The download links on the upstream project point at the official CI and do **not** include these changes.

## Related Repositories

| Repository | Purpose |
| --- | --- |
| [MINEZ/DenizenCore](https://github.com/MINEZ/DenizenCore) | Core, mirrored without changes |
| [MINEZ/Depenizen](https://github.com/MINEZ/Depenizen) | Plugin bridges, mirrored without changes |
| [MINEZ/SharpDenizenTools](https://github.com/MINEZ/SharpDenizenTools) | Shared meta and script-checking library |
| [MINEZ/DenizenMetaWebsite](https://github.com/MINEZ/DenizenMetaWebsite) | Documentation site, deployed at [denizen-meta.minez.cc](https://denizen-meta.minez.cc/) |
| [MINEZ/DenizenVSCode](https://github.com/MINEZ/DenizenVSCode) | VS Code extension, built against this fork's meta |

## Upstream

Denizen is developed by [the DenizenScript team](https://denizenscript.com/). For learning the language itself, the [beginner's guide](https://guide.denizenscript.com/) and the [Discord](https://discord.gg/Q6pZGSR) remain the right places to go — for the language as upstream ships it, not for anything in this fork.

The changes here are built for one server's needs and are maintained separately. Please keep discussion of them out of upstream's channels.

## License

Denizen is open source under the MIT License, Copyright (c) The Denizen Script Team. Modifications in this fork are provided under the same terms. See [LICENSE.txt](LICENSE.txt) for the full text.

The short of it: you can do basically whatever you want, except hold any developer liable for what you do with the software.

### Third-Party Code

The dialog subsystem is derived from [denizen-utilities](https://github.com/isnsest/denizen-utilities), Copyright (c) isnsest, licensed under the **Apache License, Version 2.0**. Those files remain under that license rather than MIT, carry a notice saying they have been modified, and are listed here:

- `paper/src/main/java/com/denizenscript/denizen/paper/PaperDialogModule.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/commands/ShowDialogCommand.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/containers/DialogScriptContainer.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/containers/DialogScriptHelper.java`
- `paper/src/main/java/com/denizenscript/denizen/paper/events/PlayerCustomClickScriptEvent.java`

A copy of the Apache License 2.0 is included at [licenses/denizen-utilities-LICENSE.txt](licenses/denizen-utilities-LICENSE.txt).
