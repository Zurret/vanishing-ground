package de.zurret.vanishingground.removal;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.config.VanishingGroundConfig.RemovalMode;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.tracking.GlobalBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared execution path for immediate and delayed removals.
 */
public final class BlockRemovalService {

	private BlockRemovalService() {
	}

	public static boolean removeIfEligible(MinecraftServer server, ServerLevel world, GlobalBlockPos vacated,
			BlockProtectionRegistry protectionRegistry, RestoreScheduler restoreScheduler,
			VanishingGroundConfig config) {
		if (!world.hasChunkAt(vacated.pos())) {
			return false;
		}

		BlockPos pos = vacated.pos();
		BlockState state = world.getBlockState(pos);
		if (!protectionRegistry.canVanish(state, world, pos, config)) {
			return false;
		}

		world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

		if (config.removalMode() == RemovalMode.RESTORE) {
			restoreScheduler.schedule(vacated, state, server.getTickCount() + config.restoreTicks());
		}
		return true;
	}
}
