package de.zurret.vanishingground.removal;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.tracking.GlobalBlockPos;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles the optional {@code vanishingGroundDelay} grace period. A
 * position scheduled for removal is re-checked for occupancy at execution
 * time - if a player has moved back onto it in the meantime, the removal
 * is silently dropped instead of pulling the block out from under them.
 */
public final class PendingRemovalScheduler {

	private record PendingRemoval(GlobalBlockPos pos, long executeAtTick) {
	}

	private final List<PendingRemoval> pending = new ArrayList<>();

	public void schedule(GlobalBlockPos pos, long executeAtTick) {
		pending.add(new PendingRemoval(pos, executeAtTick));
	}

	public void tick(MinecraftServer server, SupportPositionTracker tracker,
			BlockProtectionRegistry protectionRegistry, VanishingGroundConfig config) {
		if (pending.isEmpty()) {
			return;
		}

		long currentTick = server.getTickCount();
		Iterator<PendingRemoval> iterator = pending.iterator();
		while (iterator.hasNext()) {
			PendingRemoval removal = iterator.next();
			if (currentTick < removal.executeAtTick()) {
				continue;
			}
			iterator.remove();

			if (tracker.isOccupied(removal.pos())) {
				// A player re-entered this position before the delay elapsed.
				continue;
			}

			ServerLevel world = server.getLevel(removal.pos().dimension());
			if (world == null) {
				continue;
			}

			BlockState state = world.getBlockState(removal.pos().pos());
			if (protectionRegistry.isProtected(state, world, removal.pos().pos(), config)) {
				continue;
			}

			world.setBlock(removal.pos().pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}
}
