package de.zurret.vanishingground.protection;

import de.zurret.vanishingground.VanishingGroundMod;
import de.zurret.vanishingground.config.VanishingGroundConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decides whether a vacated block is allowed to be removed.
 * <p>
 * Rules are evaluated in a fixed, non-configurable order for the hard
 * cases (air, bedrock, portals) followed by the configurable cases
 * (fluids, block entities), plus a datapack-extensible tag for anything
 * a server operator wants to protect beyond the defaults.
 */
public final class BlockProtectionRegistry {

	/**
	 * Server operators / datapacks can add block ids here
	 * (data/vanishingground/tags/block/protected.json) without touching
	 * mod code.
	 */
	public static final TagKey<Block> PROTECTED = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
			Identifier.fromNamespaceAndPath(VanishingGroundMod.MOD_ID, "protected"));

	public boolean isProtected(BlockState state, ServerLevel world, BlockPos pos, VanishingGroundConfig config) {
		if (state.isAir()) {
			// Nothing to remove; also guards against re-processing a
			// position that already vanished (e.g. via the delay scheduler).
			return true;
		}

		if (state.is(PROTECTED)) {
			return true;
		}

		Block block = state.getBlock();

		if (block == Blocks.BEDROCK) {
			return true;
		}

		if (block instanceof NetherPortalBlock
				|| block instanceof EndPortalBlock
				|| block instanceof EndGatewayBlock) {
			return true;
		}

		if (!state.getFluidState().isEmpty() && !config.destroyFluids()) {
			// Covers water, lava and bubble columns (which are always
			// paired with a fluid state). Deliberately not touching the
			// fluid simulation by default - see design notes in the README.
			return true;
		}

		if (!config.destroyBlockEntities() && world.getBlockEntity(pos) != null) {
			return true;
		}

		return false;
	}
}
