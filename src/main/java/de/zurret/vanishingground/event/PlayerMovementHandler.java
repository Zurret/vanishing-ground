package de.zurret.vanishingground.event;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.gamerule.ModGameRules;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.removal.PendingRemovalScheduler;
import de.zurret.vanishingground.tracking.GlobalBlockPos;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Determines, once per server tick and per online player, which block
 * currently supports them, and reacts when a block becomes fully vacated.
 * <p>
 * Deliberately skipped (no tracking update, no removal):
 * <ul>
 *   <li>Spectators - no collision, nothing meaningful to track.</li>
 *   <li>Players riding a vehicle (boat, minecart, horse, ...) - the
 *       vehicle, not the player, determines ground contact.</li>
 *   <li>Flying (Creative) players and anyone not currently
 *       {@code onGround} - this also covers swimming and Elytra gliding,
 *       where "on ground" is simply false.</li>
 * </ul>
 * For everyone else, {@link ServerPlayerEntity#getOnPos()} is used rather
 * than a plain floored block position - this is the same vanilla API used
 * for honey/soul-sand/ice friction and already resolves the supporting
 * block correctly for slabs, stairs, snow layers, carpets, fences and
 * similar partial-height blocks.
 */
public final class PlayerMovementHandler {

	private final SupportPositionTracker tracker;
	private final BlockProtectionRegistry protectionRegistry;
	private final PendingRemovalScheduler scheduler;
	private final VanishingGroundConfig config;

	public PlayerMovementHandler(SupportPositionTracker tracker, BlockProtectionRegistry protectionRegistry,
			PendingRemovalScheduler scheduler, VanishingGroundConfig config) {
		this.tracker = tracker;
		this.protectionRegistry = protectionRegistry;
		this.scheduler = scheduler;
		this.config = config;
	}

	public void tickPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(server, player);
		}
	}

	private void tickPlayer(MinecraftServer server, ServerPlayer player) {
		ServerLevel world = (ServerLevel) player.level();

		if (!ModGameRules.isEnabled(config)) {
			return;
		}
		if (player.isSpectator() || player.getVehicle() != null) {
			return;
		}
		if (player.getAbilities().flying || !player.onGround()) {
			return;
		}

		BlockPos onPos = player.getOnPos();
		GlobalBlockPos newPos = new GlobalBlockPos(world.dimension(), onPos);

		tracker.updatePlayerPosition(player.getUUID(), newPos)
				.ifPresent(vacated -> handleVacated(server, vacated));
	}

	private void handleVacated(MinecraftServer server, GlobalBlockPos vacated) {
		ServerLevel world = server.getLevel(vacated.dimension());
		if (world == null) {
			return;
		}

		int delay = ModGameRules.delayTicks(config);
		if (delay <= 0) {
			removeIfEligible(world, vacated.pos());
		} else {
			scheduler.schedule(vacated, server.getTickCount() + delay);
		}
	}

	private void removeIfEligible(ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (protectionRegistry.isProtected(state, world, pos, config)) {
			return;
		}
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}
}
