# Vanishing Ground

Fabric mod: Solid ground disappears once no player is standing on it anymore.

Author: Zurret (https://zurret.de)

## Build

```
./gradlew build
```

The finished JAR is located in `build/libs/`.

This version targets Minecraft 26.2, Fabric Loader 0.19.5, and Fabric API
0.159.0+26.2. Minecraft 26.2 requires Java 25.

## Usage

The mod is disabled by default. Enable it in
`config/vanishingground.json` with `enabled: true`.

## Configuration

`config/vanishingground.json` (created on first launch):

```json
{
  "enabled": false,
  "delayTicks": 0,
  "destroyFluids": false,
  "destroyBlockEntities": false
}
```

Can also be extended with a data pack:
`data/vanishingground/tags/block/protected.json`

## Architecture Decisions

- **Support block detection:** Uses vanilla collision-shape logic
  (`getLandingPos()`), rather than a floored block position. This correctly
  handles slabs, stairs, snow layers, carpets, fences, and trapdoors as ground
  without special-case code for individual block types.
- **No persistent state:** Only the current support position per online player
  is stored (`UUID -> Dimension + BlockPos`), together with an occupancy map
  for multiplayer safety. Memory usage grows with the player count, not with
  the length of the game.
- **Multiplayer:** A block is scheduled for removal only after its occupant
  set becomes empty. If another player is still standing on it, the block
  remains in place.
- **Fluids:** Protected by default (`destroyFluids = false`) so the fluid
  simulation is not altered by side effects.
- **Block entities:** Protected by default (`destroyBlockEntities = false`)
  to prevent items from being lost in chests and other blocks.
- **Skipped (no tracking update):** Spectators, players riding vehicles
  (boat/minecart/horse), flying players, and anything with
  `isOnGround() == false`; this also covers swimming and Elytra flight.
- **Death, respawn, dimension changes, and disconnects:** The stored tracking
  entry is cleared deliberately without removing a block.
- **Delay:** Controlled by `delayTicks` in the configuration. The default
  value 0 removes blocks immediately. With a value greater than 0, occupancy
  is checked again before the actual removal.
