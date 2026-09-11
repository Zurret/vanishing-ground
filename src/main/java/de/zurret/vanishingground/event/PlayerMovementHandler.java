package de.zurret.vanishingground.event;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.removal.BlockRemovalService;
import de.zurret.vanishingground.removal.PendingRemovalScheduler;
import de.zurret.vanishingground.removal.RestoreScheduler;
import de.zurret.vanishingground.tracking.GlobalBlockPos;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Once per server tick and per online player, determine the supporting
 * block and react when a block becomes fully vacated.
 *
 * Spectators, immune players and (by default) Creative players are not
 * tracked. A short time in the air keeps the last support block occupied
 * so jumping in place does not remove the floor. Landing on a different
 * block still vacates the previous one.
 */
public final class PlayerMovementHandler {

	private final SupportPositionTracker tracker;
	private final BlockProtectionRegistry protectionRegistry;
	private final PendingRemovalScheduler scheduler;
	private final RestoreScheduler restoreScheduler;
	private final VanishingGroundConfig config;

	public PlayerMovementHandler(SupportPositionTracker tracker, BlockProtectionRegistry protectionRegistry,
			PendingRemovalScheduler scheduler, RestoreScheduler restoreScheduler, VanishingGroundConfig config) {
		this.tracker = tracker;
		this.protectionRegistry = protectionRegistry;
		this.scheduler = scheduler;
		this.restoreScheduler = restoreScheduler;
		this.config = config;
	}

	public void tickPlayers(MinecraftServer server) {
		if (!config.enabled()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(server, player);
		}
	}

	private void tickPlayer(MinecraftServer server, ServerPlayer player) {
		if (shouldIgnorePlayer(player)) {
			tracker.clearPlayer(player.getUUID());
			return;
		}

		boolean flyingOrMounted = player.getVehicle() != null || player.getAbilities().flying;
		if (flyingOrMounted) {
			if (config.vacateWhenFlyingOrMounted()) {
				tracker.clearPlayer(player.getUUID())
						.ifPresent(vacated -> handleVacated(server, vacated, player));
			} else {
				tracker.clearPlayer(player.getUUID());
			}
			return;
		}

		if (!player.onGround()) {
			return;
		}

		ServerLevel world = (ServerLevel) player.level();
		if (!config.isDimensionAllowed(world.dimension())) {
			tracker.clearPlayer(player.getUUID());
			return;
		}

		BlockPos onPos = player.getOnPos();
		GlobalBlockPos newPos = new GlobalBlockPos(world.dimension(), onPos);

		tracker.updatePlayerPosition(player.getUUID(), newPos)
				.ifPresent(vacated -> handleVacated(server, vacated, player));
	}

	private boolean shouldIgnorePlayer(ServerPlayer player) {
		if (player.isSpectator()) {
			return true;
		}
		if (config.isPlayerImmune(player.getUUID())) {
			return true;
		}
		if (!config.affectCreative() && player.getAbilities().instabuild) {
			return true;
		}
		return false;
	}

	private void handleVacated(MinecraftServer server, GlobalBlockPos vacated, ServerPlayer leavingPlayer) {
		if (config.sneakProtects() && leavingPlayer.isShiftKeyDown()) {
			return;
		}

		ServerLevel world = server.getLevel(vacated.dimension());
		if (world == null) {
			return;
		}

		int delay = config.delayTicks();
		if (delay <= 0) {
			BlockRemovalService.removeIfEligible(server, world, vacated, protectionRegistry, restoreScheduler, config);
		} else {
			scheduler.schedule(vacated, server.getTickCount() + delay);
		}
	}
}
