package de.zurret.vanishingground.tracking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks, per online player, the block position that currently supports
 * them, and how many players currently share that same position.
 * <p>
 * Design constraints (deliberate):
 * <ul>
 *   <li>No history is kept. Only the <em>current</em> support position per
 *       player is stored, so memory usage is bounded by the number of
 *       online players, not by run length or world size.</li>
 *   <li>A position is only reported as "vacated" once its occupant set
 *       becomes empty, which is what makes this safe for multiple players
 *       standing on the same block.</li>
 *   <li>Not thread-safe by design. All mutating calls are expected to
 *       happen from the server thread (i.e. inside tick events), matching
 *       how the rest of the Minecraft server operates.</li>
 * </ul>
 */
public final class SupportPositionTracker {

	private final Map<UUID, GlobalBlockPos> currentSupport = new HashMap<>();
	private final Map<GlobalBlockPos, Set<UUID>> occupants = new HashMap<>();

	/**
	 * Updates the tracked support position for a player.
	 *
	 * @return the previous support position, but only if it became
	 *         unoccupied as a result of this update (i.e. eligible for
	 *         removal). Empty otherwise, including on a player's first
	 *         recorded position.
	 */
	public Optional<GlobalBlockPos> updatePlayerPosition(UUID playerId, GlobalBlockPos newPos) {
		GlobalBlockPos oldPos = currentSupport.get(playerId);
		if (newPos.equals(oldPos)) {
			return Optional.empty();
		}

		// Register the new position before releasing the old one. This
		// ordering matters when a block is re-entered by the same tick
		// logic and avoids any transient double-counting artifacts.
		occupants.computeIfAbsent(newPos, p -> new HashSet<>()).add(playerId);
		currentSupport.put(playerId, newPos);

		if (oldPos == null) {
			return Optional.empty();
		}

		return releaseOccupant(oldPos, playerId);
	}

	/**
	 * Removes all tracking state for a player without treating it as
	 * "walking away" for game-design purposes at the call site - callers
	 * decide whether the returned vacated position should actually trigger
	 * a removal (see lifecycle handling for death/respawn/disconnect,
	 * where it deliberately does not).
	 */
	public Optional<GlobalBlockPos> clearPlayer(UUID playerId) {
		GlobalBlockPos oldPos = currentSupport.remove(playerId);
		if (oldPos == null) {
			return Optional.empty();
		}
		return releaseOccupant(oldPos, playerId);
	}

	public boolean isOccupied(GlobalBlockPos pos) {
		Set<UUID> set = occupants.get(pos);
		return set != null && !set.isEmpty();
	}

	private Optional<GlobalBlockPos> releaseOccupant(GlobalBlockPos pos, UUID playerId) {
		Set<UUID> set = occupants.get(pos);
		if (set == null) {
			return Optional.empty();
		}
		set.remove(playerId);
		if (set.isEmpty()) {
			occupants.remove(pos);
			return Optional.of(pos);
		}
		return Optional.empty();
	}
}
