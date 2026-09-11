package de.zurret.vanishingground.removal;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.tracking.GlobalBlockPos;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Handles the optional delayTicks grace period. Re-checks occupancy at
 * execution time. A later schedule for the same position replaces the
 * earlier one so stepping on and off the same block does not queue
 * several removals.
 */
public final class PendingRemovalScheduler {

	private record PendingRemoval(GlobalBlockPos pos, long executeAtTick) {
	}

	private final Map<GlobalBlockPos, PendingRemoval> pending = new HashMap<>();
	private final Set<GlobalBlockPos> warned = new HashSet<>();

	public void schedule(GlobalBlockPos pos, long executeAtTick) {
		pending.put(pos, new PendingRemoval(pos, executeAtTick));
		warned.remove(pos);
	}

	public void clear() {
		pending.clear();
		warned.clear();
	}

	public void tick(MinecraftServer server, SupportPositionTracker tracker,
			BlockProtectionRegistry protectionRegistry, RestoreScheduler restoreScheduler,
			VanishingGroundConfig config) {
		if (pending.isEmpty()) {
			return;
		}
		if (!config.enabled()) {
			clear();
			return;
		}

		long currentTick = server.getTickCount();
		int warningTicks = config.warningTicks();

		Iterator<Map.Entry<GlobalBlockPos, PendingRemoval>> iterator = pending.entrySet().iterator();
		while (iterator.hasNext()) {
			PendingRemoval removal = iterator.next().getValue();

			if (tracker.isOccupied(removal.pos())) {
				iterator.remove();
				warned.remove(removal.pos());
				continue;
			}

			if (warningTicks > 0 && !warned.contains(removal.pos())
					&& currentTick >= removal.executeAtTick() - warningTicks
					&& currentTick < removal.executeAtTick()) {
				fireWarning(server, removal.pos());
				warned.add(removal.pos());
			}

			if (currentTick < removal.executeAtTick()) {
				continue;
			}

			iterator.remove();
			warned.remove(removal.pos());
			execute(server, removal.pos(), protectionRegistry, restoreScheduler, config);
		}
	}

	private void fireWarning(MinecraftServer server, GlobalBlockPos pos) {
		ServerLevel world = server.getLevel(pos.dimension());
		if (world != null) {
			WarningEffects.fire(world, pos.pos());
		}
	}

	private void execute(MinecraftServer server, GlobalBlockPos pos, BlockProtectionRegistry protectionRegistry,
			RestoreScheduler restoreScheduler, VanishingGroundConfig config) {
		ServerLevel world = server.getLevel(pos.dimension());
		if (world == null) {
			return;
		}
		BlockRemovalService.removeIfEligible(server, world, pos, protectionRegistry, restoreScheduler, config);
	}

	public int pendingCount() {
		return pending.size();
	}
}
