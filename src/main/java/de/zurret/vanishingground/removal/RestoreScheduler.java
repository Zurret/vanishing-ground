package de.zurret.vanishingground.removal;

import de.zurret.vanishingground.tracking.GlobalBlockPos;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Brings a vanished block back after restoreTicks if the position is
 * still air and unoccupied. A later schedule for the same position
 * replaces the earlier one.
 */
public final class RestoreScheduler {

	private record PendingRestore(GlobalBlockPos pos, BlockState previousState, long restoreAtTick) {
	}

	private final Map<GlobalBlockPos, PendingRestore> pending = new HashMap<>();

	public void schedule(GlobalBlockPos pos, BlockState previousState, long restoreAtTick) {
		pending.put(pos, new PendingRestore(pos, previousState, restoreAtTick));
	}

	public void clear() {
		pending.clear();
	}

	public void tick(MinecraftServer server, SupportPositionTracker tracker) {
		if (pending.isEmpty()) {
			return;
		}

		long currentTick = server.getTickCount();
		Iterator<Map.Entry<GlobalBlockPos, PendingRestore>> iterator = pending.entrySet().iterator();
		while (iterator.hasNext()) {
			PendingRestore restore = iterator.next().getValue();
			if (currentTick < restore.restoreAtTick()) {
				continue;
			}

			ServerLevel world = server.getLevel(restore.pos().dimension());
			if (world == null || !world.hasChunkAt(restore.pos().pos())) {
				continue;
			}

			if (!world.getBlockState(restore.pos().pos()).isAir()) {
				iterator.remove();
				continue;
			}

			if (tracker.isOccupied(restore.pos())) {
				continue;
			}

			world.setBlock(restore.pos().pos(), restore.previousState(), Block.UPDATE_ALL);
			iterator.remove();
		}
	}

	public int pendingCount() {
		return pending.size();
	}
}
