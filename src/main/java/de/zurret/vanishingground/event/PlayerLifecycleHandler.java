package de.zurret.vanishingground.event;

import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

/**
 * Clears tracking state on death, respawn, dimension change and
 * disconnect. In every one of these cases the previous position is
 * deliberately released <em>without</em> triggering block removal - the
 * player did not "walk away" in the sense the mechanic is meant to
 * capture, they were moved. This also avoids surprising world edits while
 * a player is offline.
 */
public final class PlayerLifecycleHandler {

	private PlayerLifecycleHandler() {
	}

	public static void register(SupportPositionTracker tracker) {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				tracker.clearPlayer(handler.getPlayer().getUUID()));

		// Covers both death respawns and dimension changes (e.g. End/Nether
		// portals fire this with alive == true).
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				tracker.clearPlayer(oldPlayer.getUUID()));
	}
}
