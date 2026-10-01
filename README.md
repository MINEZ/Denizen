<div align="center">

# MINEZ Denizen

A fork of [Denizen](https://github.com/DenizenScript/Denizen) maintained for the MINEZ server.

<a href="README.md">English</a> ｜
<a href="docs/README_zh-CN.md">简体中文</a>

<br>

<div>
<a href="https://github.com/DenizenScript/Denizen"><img src="docs/badges/upstream.svg" alt="upstream"></a>
<a href="#supported-versions"><img src="https://img.shields.io/badge/Minecraft-7%20versions-4caf50" alt="Minecraft"></a>
<a href="LICENSE.txt"><img src="https://img.shields.io/badge/license-MIT-9e9e9e" alt="license"></a>
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

Following upstream, only the Spigot versions shown at the top of this page are supported — not the ranges between them.

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
| `<LocationTag.has_ce_drawer[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_drawer_item[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_drawer_item_quantity[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_drawer_max_quantity[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_drawer_data_keys>` | Tag | Added |
| `<LocationTag.has_ce_storage_inventory[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_storage_inventory[(<data_key>)]>` | Tag | Added |
| `<LocationTag.ce_storage_inventory_data_keys>` | Tag | Added |
| `<EntityTag.has_ce_storage_inventory[(<data_key>)]>` | Tag | Added |
| `<EntityTag.ce_storage_inventory[(<data_key>)]>` | Tag | Added |
| `<EntityTag.ce_storage_inventory_data_keys>` | Tag | Added |
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
| `player respawns` | Event | Changed |
| `PlayerTag.save_data` | Mechanism | Added |
| `fakespawn` | Command | Changed |
| `bossbar` | Command | Changed |
| `attach` | Command | Changed |
| `EntityTag.hide_description` | Property | Added |
| `EntityTag.description` | Property | Added |
| `EntityTag.immovable` | Property | Added |
| `EntityTag.main_hand` | Property | Added |
| `EntityTag.pose` | Property | Added |
| `EntityTag.profile` | Property | Added |
| `EntityTag.skin_layers` | Tag/Mechanism | Changed |
| `areadisplay` | Command | Added |
| `<server.area_displays>` | Tag | Added |
| `<server.area_display[<id>]>` | Tag | Added |
| `<PlayerTag.area_display_ids>` | Tag | Added |
| Offline player inventory editing | Behaviour | Fixed |
| `EntityTag` living-entity mechanisms | Mechanism | Fixed |
| `projectile launched` | Event | Fixed |
| `potion effects modified` | Event | Fixed |
| Fake entity tracking after a rejoin | Behaviour | Fixed |
| Boss bar visibility after a rejoin | Behaviour | Fixed |
| Disguise movement as seen by others | Behaviour | Fixed |
| Disguise attributes as seen by others | Behaviour | Fixed |

**Cooking recipe tags.** Upstream only offers recipe lookup by result. Looking one up by input meant iterating `server.recipe_ids` and matching against the text of `server.recipe_items`, which exposes just the first material of a multi-material input — the vanilla glass recipe accepts both sand and red sand, so red sand was always missed. These tags build a material-to-recipe index on first use and let vanilla's own `RecipeChoice#test` decide matches, so multi-material and exact-match inputs both work.

**Dialogs.** A `dialog` script container backed by Paper's dialog API, covering the `confirm`, `notice`, `list` and `multi` layouts, along with `base`, `bodies`, `inputs`, `buttons` and a `procedural` section for building content dynamically. The `base` section also takes `after action`, one of `close` (the default), `none` or `wait_for_response`, which decides what the client does with the dialog screen after a button is clicked; pick `none` for a button that opens another dialog, so the client never drops back to the game screen in between and the mouse cursor is not moved back to the center of the screen. Note that `none` takes the exit button and the Escape key with it, so a `list` or `multi` dialog using it needs an `exit button` whose script closes the dialog. Requires Paper 1.21.6 or newer; on older or non-Paper servers these are not registered and `type: dialog` containers will fail to load, while everything else is unaffected.

This subsystem is derived from [denizen-utilities](https://github.com/isnsest/denizen-utilities), which is licensed under the Apache License 2.0, and keeps its interface so existing `type: dialog` scripts migrate unchanged. Three deliberate differences: `exit button` is now read from the `base` section (the original only read the container root, so exit buttons never took effect), with the old placement still accepted; a button with no `script` section no longer binds a click action, so clicking it simply closes the dialog; and button IDs have their spaces folded into underscores on the way into the namespaced click key, which is what lets an `exit button` carry a `script` section of its own (the original rejected the space and dropped the action without binding it).

**Boss bar defaults.** Upstream's `bossbar` shows the bar to the linked player alone when no `players:` list is given, and fails outright when the script has no linked player at all. It now defaults to every player on the server, and keeps up with the roster: a player who joins while the bar is around is shown it too. Pass `players:<player>` for the old behaviour. Note that using `remove` with a `players:` list on a server-wide bar narrows it down to the players who can see it at that moment, so it stops following new joins.

**Inline images.** `<&head[...]>` and `<&sprite[...]>` emit the object text components added in 1.21.9, rendering a player face or an atlas sprite inline in chat. Denizen's text pipeline is built on the BungeeCord Chat API, which is frozen and drops this component type, so the fork carries its own component and serializer. **Requires a 1.21.9+ client** — older clients render nothing, without erroring.

**Respawn points.** Upstream's `player respawns` event only tells you whether the respawn point was a bed, so respawn anchors are indistinguishable from the world spawn. The event now carries the full information the server provides — `<context.spawn_type>` (bed, anchor or world), `<context.reason>` (death, end_portal or plugin), and `<context.is_missing_respawn_block>` for when a bed or anchor was destroyed — with matching `spawn_type:` and `reason:` switches. The old `at bed` and `elsewhere` forms still work but warn on load.

**Redundant potion effects.** Upstream's `cast` treats a `false` return from Bukkit as a failure and reports an error, but that return only means the effect table did not change. That is the normal outcome whenever the target already has an effect of the same type at an equal or higher amplifier — the existing one either keeps running, or the new one is stored as a hidden effect and takes over once it ends. The command now tells the two apart and only reports an error when the effect genuinely could not be applied.

**Writing player data on demand.** `PlayerTag.save_data` writes a player's data to their save file right away. Edits to an offline player's inventory are otherwise only written out when the data leaves the cache, when the player logs in, or when the server shuts down, which leaves a window where a crash would lose them.

**Fake entity defaults.** Upstream's `fakespawn` shows the entity to the linked player alone when no `players:` list is given. It now defaults to every player in the world the entity is spawned in, and keeps up with the roster: a player who joins the server, or who enters that world, is shown the entity as well, for as long as it is around. Pass `players:<player>` for the old behaviour. Viewers who disconnect or leave that world are dropped from tracking until they return, since there is nothing to show them in the meantime. The `duration:` default changed as well: leaving it out used to mean ten seconds, and now means the entity stays until it is cancelled or the server stops.

**Attaching fake entities.** `attach` drives an attachment by rewriting the movement packets sent for the target entity, which leaves two gaps where fake entities are concerned: the server never sends a player their own movement, so an attachment to yourself is invisible to you, and a fake entity's movement is only ever sent by Denizen in the first place. Fake entities now always sync serverside. For them that carries none of the side effects it has on real entities — they are not in the world, so the sync is just a coordinate update — and `attach <player.fake_entities> to:<player> offset:0,2,0` works without `sync_server`. The sync pushes the movement out as soon as it happens rather than waiting for the fake entity's own tick, which takes one tick off the lag the attached-to player sees. The rest of that lag is the round trip to the server and cannot be removed — other players see no such delay, since they receive the entity and the player over the same path.

**CraftEngine containers.** CraftEngine ships its own Denizen integration, but it offers no way to read what its containers hold, and the request for one was [declined](https://github.com/Xiao-MoMi/craft-engine/issues/810) on the grounds that these containers are custom registered behaviors rather than a general feature. Eleven tags now cover the three behaviors that define one: `drawer_block`, `simple_storage_block` and `simple_storage_furniture`. The storage tags return the live container, so anything written to it applies at once. Each takes an optional data key, matching the behavior's `data_key` option exactly as written; leaving it out takes the first container declared on the block or furniture, and the `..._data_keys` tags list them all in that same order. A furniture that leaves `data_key` unset reports the key its contents actually live under, since CraftEngine only falls back to a default when reading and writing the save. Note that `ce_drawer_max_quantity` moves with the stored item: a drawer set to 32 stacks reports 32 while empty and 32 times the item's max stack size once filled, except in compatible mode, which always reports the stack count. None of this is linked against at compile time — CraftEngine is reached entirely by reflection through its own class loader, members are looked up by name and then by type so a rename alone changes nothing, and if CraftEngine reshapes these internals the affected tags simply stop being registered.

**Mannequin properties.** Upstream Denizen has no mannequin support at all, so nothing vanilla offers on these entities could be reached from a script — least of all through `disguise ... as:mannequin[...]`, where the disguise entity is built inside Denizen and no command can touch it. Five properties now cover the settings worth having: `description` replaces the line below the name, where a player's below-name score would go, with no input returning it to the default; `hide_description` drops that line entirely, taking away the “NPC” shown there by default; `immovable` stops the entity being pushed; `main_hand` swaps which hand holds the main item; `pose` sets the pose it is held in; and `profile` sets the skin, as a map holding any of a player name, a UUID, a base64 texture blob, or texture keys for the body, cape and elytra plus the arm width (written as vanilla's WIDE and SLIM, or Bukkit's CLASSIC and SLIM — both are accepted). Give a name or a UUID and the client does the lookup, so the skin arrives a moment later; give a texture and it applies at once. `profile` is Paper-only, since Spigot's profile type cannot express the texture overrides at all. All require Minecraft 1.21.9 or newer. `<EntityTag.skin_layers>` and its mechanism now accept mannequins as well, so the outer skin layers can be turned on and off the same way they are for players — both sit on the same synced byte, since a mannequin is an avatar like a player is. Entities that have no such layers now report that plainly instead of throwing a cast error. Spigot and Paper expose several of these through incompatible interfaces, so they go through Denizen's existing split between the two.

**Area displays.** Upstream offers no way to show players where an area lies, short of a script loop playing particles point by point, which gets expensive as the area grows and has to be cleaned up by hand. `areadisplay` outlines a cuboid, polygon or ellipsoid with particles and keeps redrawing it until it expires or is removed, much like the selection outlines of [WorldEditSUI](https://github.com/kennytv/WorldEditSUI), with an optional grid on the surface that `update` can turn on and off at any time; as in WorldEditSUI, the grid gets sparser as the surface gets bigger, so large areas do not flood players with particles. It is driven the same way as the fork's `bossbar`: `auto`, `create`, `update` and `remove` against an ID, and leaving out `players:` shows the display to every player on the server, including those who join later. The points are worked out once when a display is created or updated, and each redraw only sends a viewer the points within `range:` of them; past the 32 blocks at which vanilla clients stop rendering ordinary particles, they are sent forced. A viewer is sent at most `max_particles:` of them per redraw, nearest first, and each redraw is spread evenly over its interval, up to a second, instead of going out in a single burst, with the picking and sending done off the main thread. Displays live in memory only and are gone after a restart. WorldEditSUI is licensed under the GPL, so this is an independent implementation that takes none of its code.

### Fixes

- **Edits to an offline player's inventory were silently discarded.** An offline player's inventory is rebuilt from their saved data as a plain Bukkit object, and edits to it only reach the saved data through an explicit sync step. That step never ran when the cached entry expired, so anything changed through `inventory open` was lost about an hour later. The sync now runs before a cached entry is dropped, and again whenever such an inventory is closed. Opened views are also closed if the player comes online, since the data behind them is no longer theirs.
- **Mechanisms that only apply to living entities** — `no_damage_duration`, `oxygen`, `gliding` and ten others read the entity as a living one without checking first, so using them on a boat, minecart or painting threw a raw null pointer exception. They now report which mechanism was misapplied and to what. The `cast` command had the same flaw and is guarded too.
- **`projectile launched` event** — `<context.shooter>` threw a null pointer exception when the projectile had no shooter, such as one fired by a dispenser. It now returns null.
- **`potion effects modified` event** — `<context.effect_type>` and the `effect` switch used Bukkit's legacy effect names such as `SLOW` and `FAST_DIGGING`. They now use the modern keys, `slowness` and `haste`, matching the rest of Denizen.
- **Fake entities stopped moving once a viewer rejoined.** Every viewer of a fake entity gets a tracker of their own, bound to the connection it was built on. That connection does not survive a disconnect, so after the player came back the updates were still being written to the dead one and the entity sat wherever it had been left. Trackers are now rebuilt when a viewer rejoins the server or changes worlds.
- **Boss bars were lost once a viewer rejoined.** Bukkit records a boss bar's viewers as player instances, and the instance a player is served by does not survive a disconnect, so a viewer who came back was never sent the bar again. The defunct instance was also held for as long as the bar lived, keeping it from being collected and leaving it listed by `<server.bossbar_viewers[<bossbar_id>]>`. Viewers are now recorded by UUID alongside that: the defunct instance is released when they disconnect, and the bar is shown to them again when they return. `bossbar update` and `bossbar remove` no longer hand Bukkit a null player when a named player is offline either, which used to throw a raw exception.
- **Disguised entities stood still for everyone but themselves.** From 1.19 onward the disguise handler intercepted the movement and teleport packets of a disguised entity and, for anything other than an ender dragon, dropped them in favour of re-sending the disguise. The re-sent spawn carries the position the disguise entity was created at, which is never updated, so the disguise stayed wherever it started. Movement packets now pass through untouched — the disguise shares the real entity's id, so the client applies them to it — and the disguise entity is moved into place before a spawn is re-sent, so viewers who come into range see it where it belongs.
- **A disguise's attributes never reached anyone.** The disguise entity sends its own attributes when a viewer is paired with it, but the real entity's attribute packet follows right behind under the same entity id and overwrites them. Anything set through `as:<type>[attribute_base_values=[...]]` — a scale, most usefully — therefore had no effect at all. The real entity's attribute packets are now dropped for everyone but the disguised player, whose own still pass through untouched, since client-side movement prediction rests on them.
- **Disguises froze in place once the disguised player rejoined.** A player is handed a new entity id when they reconnect, while the disguise entity keeps the id it was built with. Viewers then got movement packets addressed to the new id, and their client only held the old one, so the disguise stopped moving all over again. The disguise entity is now discarded when the disguised player rejoins, and rebuilt with their current id the next time a spawn packet is intercepted.

## Building

Requires JDK 17+ and all listed Spigot versions installed via [BuildTools](https://www.spigotmc.org/wiki/buildtools/).

```bash
mvn clean package -DBUILD_NUMBER=<upstream build>.<fork revision> -DBUILD_CLASS=DEV
```

The result lands in `target/`, for example `Denizen-1.3.3-b7303.1-DEV.jar`.

## Versioning

The project version follows upstream unchanged. Fork revisions are recorded in the build number instead: in `Denizen-1.3.3-b7303.1-DEV`, `7303` is the upstream build this is based on and the trailing `.1` is the fork revision. This keeps our version numbers from colliding with upstream releases and avoids conflicts in `pom.xml` when merging.

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
