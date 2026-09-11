# Vanishing Ground

Fabric mod for Minecraft 26.2 that makes solid ground disappear once no player is standing on it anymore.

Author: Zurret (https://zurret.de)

## Build

```text
./gradlew build
```

The finished JAR is located in `build/libs/`.

This version targets Minecraft 26.2, Fabric Loader 0.19.5, and Fabric API `0.159.0+26.2`. Minecraft 26.2 requires Java 25.

## Usage

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

## Configuration

`config/vanishingground.json`:

```json
{
  "enabled": false,
  "delayTicks": 0,
  "destroyFluids": false,
  "destroyBlockEntities": false
}
```

* `enabled`: Enables or disables the mod.
* `delayTicks`: Number of ticks to wait before removing an unoccupied block.
* `destroyFluids`: Allows fluids to be removed when enabled.
* `destroyBlockEntities`: Allows blocks with block entities to be removed when enabled.

Protected blocks can also be extended with a data pack tag:

`data/vanishingground/tags/block/protected.json`

## Architecture Decisions

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

### 1.0.0

* Initial release.
