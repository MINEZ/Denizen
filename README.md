<div align="center">

# MINEZ Denizen

A fork of [Denizen](https://github.com/DenizenScript/Denizen) maintained for the MINEZ server.

[English](README.md) | [简体中文](README_zh-CN.md)

[![Upstream](https://img.shields.io/badge/upstream-Denizen%201.3.3-1976d2)](https://github.com/DenizenScript/Denizen)
[![Minecraft](https://img.shields.io/badge/Minecraft-7%20versions-4caf50)](#supported-versions)
[![License](https://img.shields.io/badge/license-MIT-9e9e9e)](LICENSE.txt)
[![Docs](https://img.shields.io/badge/docs-denizen--meta.minez.cc-673ab7)](https://denizen-meta.minez.cc/)

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
| `projectile launched` | Event | Fixed |
| `potion effects modified` | Event | Fixed |

**Cooking recipe tags.** Upstream only offers recipe lookup by result. Looking one up by input meant iterating `server.recipe_ids` and matching against the text of `server.recipe_items`, which exposes just the first material of a multi-material input — the vanilla glass recipe accepts both sand and red sand, so red sand was always missed. These tags build a material-to-recipe index on first use and let vanilla's own `RecipeChoice#test` decide matches, so multi-material and exact-match inputs both work.

**Dialogs.** A `dialog` script container backed by Paper's dialog API, covering the `confirm`, `notice`, `list` and `multi` layouts, along with `base`, `bodies`, `inputs`, `buttons` and a `procedural` section for building content dynamically. Requires Paper 1.21.6 or newer; on older or non-Paper servers these are not registered and `type: dialog` containers will fail to load, while everything else is unaffected.

The interface mirrors the denizen-utilities plugin so existing `type: dialog` scripts migrate unchanged, with two deliberate differences: `exit button` is now read from the `base` section (the original only read the container root, so exit buttons never took effect), with the old placement still accepted; and a button with no `script` section no longer binds a click action, so clicking it simply closes the dialog.

**Inline images.** `<&head[...]>` and `<&sprite[...]>` emit the object text components added in 1.21.9, rendering a player face or an atlas sprite inline in chat. Denizen's text pipeline is built on the BungeeCord Chat API, which is frozen and drops this component type, so the fork carries its own component and serializer. **Requires a 1.21.9+ client** — older clients render nothing, without erroring.

### Fixes

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
