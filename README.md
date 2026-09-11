# Vanishing Ground

Fabric mod for Minecraft 26.2 that makes solid ground disappear once no player is standing on it anymore.

Author: Zurret (https://zurret.de)

Version 1.2.0 keeps the 1.1.0 mechanic and adds safer defaults for mixed singleplayer and multiplayer worlds, shorter commands, presets, and a few correctness fixes.

## Build

```text
./gradlew build
```

The finished JAR is located in `build/libs/`.

This version targets Minecraft 26.2, Fabric Loader 0.19.5, and Fabric API `0.159.0+26.2`. Minecraft 26.2 requires Java 25.

## Usage

<<<<<<< HEAD
The mod is disabled by default so installing it cannot wipe an existing world.

Enable it with:

```text
/vg enable
```

or set `enabled` to `true` in `config/vanishingground.json`.

The configuration file is created on first launch. All command changes are saved there immediately.

For a first setup without hand-tuning every option:

```text
/vg preset parkour
/vg preset challenge
/vg preset safe
```

## Commands

Permission level 2 or higher is required.

`/vg` is an alias of `/vanishingground`.

```text
/vg help
/vg enable
/vg disable
/vg status
/vg debug
```

### Timing

```text
/vg delay <ticks>
/vg warning <ticks>
/vg restore <ticks>
```

`delay 0` removes a vacated block immediately. `delay 20` waits about one second. `warning` shows particles and a click shortly before a delayed removal. `restore` only matters when removal mode is `restore`.

### What may vanish

```text
/vg fluids allow|protect
/vg block_entities allow|protect
/vg mode blacklist|whitelist
/vg removalmode vanish|restore
```

* `blacklist` (default): every solid block vanishes unless it is hard-protected or tagged `#vanishingground:protected`.
* `whitelist`: nothing vanishes unless it is tagged `#vanishingground:vanishing`. Use this on mixed survival or adventure worlds.
* `vanish`: the block is gone for good.
* `restore`: the block comes back after `restore <ticks>` if the spot is still air and empty.

### Where and who

```text
/vg dimensions add minecraft:overworld
/vg dimensions remove minecraft:overworld
/vg dimensions clear
/vg dimensions list
/vg creative ignore
/vg creative affect
/vg sneak protect
/vg sneak normal
/vg flyleave keep
/vg flyleave vanish
/vg immune add <onlinePlayer>
/vg immune remove <onlinePlayer>
/vg immune clear
/vg immune list
```

* Empty dimension list means every dimension.
* Creative players are ignored by default. That keeps singleplayer building safe while Survival or Adventure still uses the mechanic.
* `sneak protect` keeps the block you step off while sneaking, which is useful while placing blocks next to a live course.
* `flyleave keep` (default) does not treat takeoff or mounting as walking away. `flyleave vanish` does.
* Immune players are never tracked. The player must be online when added or removed.
=======
The mod is disabled by default.

Enable it by setting `enabled` to `true` in `config/vanishingground.json`, or use the in-game command:

```text
/vanishingground enable
```

The configuration file is created on first launch.

## Commands

Vanishing Ground provides in-game commands for changing and inspecting its configuration.

All commands require permission level 2 or higher.

### Enable or disable

```text
/vanishingground enable
/vanishingground disable
```

Enables or disables Vanishing Ground. The change is saved to the configuration file.

### Check the current configuration

```text
/vanishingground status
```

Displays the current configuration and mod status.

### Set the removal delay

```text
/vanishingground delay <ticks>
```

Sets the number of ticks to wait before removing an unoccupied block.

For example:

```text
/vanishingground delay 0
/vanishingground delay 20
```

`0` removes blocks immediately. `20` waits approximately one second.

### Configure fluid destruction

```text
/vanishingground fluids allow
/vanishingground fluids protect
```

Controls whether fluids can be removed.

Fluids are protected by default.

### Configure block entity destruction

```text
/vanishingground block_entities allow
/vanishingground block_entities protect
```

Controls whether blocks containing block entities can be removed.

Blocks with block entities are protected by default.

All configuration changes made through commands are persisted to `config/vanishingground.json`.
>>>>>>> 5330f23e1540a28758021289d6ee163ca4d75267

## Configuration

`config/vanishingground.json`:

```json
{
  "enabled": false,
  "delayTicks": 0,
  "warningTicks": 0,
  "destroyFluids": false,
  "destroyBlockEntities": false,
  "blockSelectionMode": "BLACKLIST",
  "removalMode": "VANISH",
  "restoreTicks": 40,
  "allowedDimensions": [],
  "affectCreative": false,
  "sneakProtects": false,
  "vacateWhenFlyingOrMounted": false,
  "immunePlayers": []
}
```

<<<<<<< HEAD
Files written by 1.0.x and 1.1.0 are upgraded on load. Missing fields use the defaults above.
=======
* `enabled`: Enables or disables the mod.
* `delayTicks`: Number of ticks to wait before removing an unoccupied block.
* `destroyFluids`: Allows fluids to be removed when enabled.
* `destroyBlockEntities`: Allows blocks with block entities to be removed when enabled.

Protected blocks can also be extended with a data pack tag:

`data/vanishingground/tags/block/protected.json`
>>>>>>> 5330f23e1540a28758021289d6ee163ca4d75267

Data pack tags:

<<<<<<< HEAD
* `data/vanishingground/tags/block/protected.json` used in blacklist mode. The shipped tag already covers command blocks, spawners, chests, barriers and similar blocks you almost never want deleted.
* `data/vanishingground/tags/block/vanishing.json` used in whitelist mode. The shipped tag is a small parkour-oriented starter list. Replace or extend it with a datapack.

## Behaviour notes

* Support detection uses `getOnPos()`, the same vanilla landing position used for friction. Slabs, stairs, snow, carpets and fences are handled without special cases.
* A block is removed only when its occupant set is empty. Two players on the same block are safe.
* Memory use scales with online players, not world size.
* Jumping in the air does not drop tracking, so a jump in place does not delete the floor. Landing on another block does vacate the previous one.
* Spectators are never tracked. Death, respawn, dimension change and disconnect clear tracking without removing a block.
* Delayed removals are cancelled if anyone steps back onto the block. Disabling the mod also drops pending removals.
* The same position is only queued once for removal or restore.
* Unloaded chunks are not edited.
* Stored `BlockPos` values are copied to an immutable instance before they are used as map keys.

## Testing

```text
./gradlew test
```

`SupportPositionTracker` is covered without a running server: one player leaving, several players sharing a block, disconnects, and the same coordinates in two dimensions.

## Continuous Integration

Pushes and pull requests to `main` run `./gradlew build` and upload the JAR.

## Changelog

### 1.2.0

* Added `/vg` as a short alias and `/vg help`.
* Added presets: `parkour`, `challenge`, `safe`.
* Creative players are ignored by default.
* Added sneak-protect, fly/mount leave behaviour, and an immune player list.
* Pending removals and restores are deduplicated per position.
* Pending removals are dropped when the mod is disabled.
* Chunk-loaded checks before world edits.
* Support positions now store an immutable `BlockPos`.
* Default protected and vanishing block tags ship with useful starter entries.
* Config files from 1.1.0 gain the new fields automatically.

### 1.1.0

* Block selection mode, restore mode, warning, dimension filter, debug command, unit tests, CI.

### 1.0.1

* In-game configuration commands.
=======
* **Support block detection:** Uses vanilla collision-shape logic (`getLandingPos()`), rather than a floored block position. This correctly handles slabs, stairs, snow layers, carpets, fences, trapdoors, and other partial blocks without special-case code for individual block types.
* **No persistent player state:** Only the current support position per online player is stored (`UUID -> Dimension + BlockPos`), together with an occupancy map for multiplayer safety. Memory usage grows with the number of tracked players, not with the length of the game.
* **Multiplayer:** A block is scheduled for removal only after its occupant set becomes empty. If another player is still standing on it, the block remains in place.
* **Fluids:** Protected by default (`destroyFluids = false`) so fluid simulation is not altered by side effects.
* **Block entities:** Protected by default (`destroyBlockEntities = false`) to prevent items from being lost in chests and other blocks.
* **Skipped players:** Spectators, players riding vehicles such as boats, minecarts, or horses, flying players, and players for which `isOnGround() == false` do not update ground tracking. This also covers swimming and Elytra flight.
* **Death, respawn, dimension changes, and disconnects:** The stored tracking entry is cleared deliberately without removing a block.
* **Delayed removal:** When `delayTicks` is greater than zero, the block's occupancy is checked again before removal. If a player returns during the delay, the scheduled removal is cancelled.

## Changelog

### 1.0.1

* Added in-game commands for configuring Vanishing Ground.
* Added commands to enable and disable the mod.
* Added a status command for inspecting the current configuration.
* Added commands for configuring the block removal delay.
* Added commands for allowing or protecting fluids.
* Added commands for allowing or protecting blocks with block entities.
* Command-based configuration changes are saved automatically.
>>>>>>> 5330f23e1540a28758021289d6ee163ca4d75267

### 1.0.0

* Initial release.
