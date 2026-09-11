package de.zurret.vanishingground.protection;

import de.zurret.vanishingground.VanishingGroundMod;
import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.config.VanishingGroundConfig.BlockSelectionMode;
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
 * Decides whether a vacated block is allowed to vanish.
 * <p>
 * Hard-coded protections (air, bedrock, portals) are evaluated first and
 * are never configurable - removing them would either be a no-op (air) or
 * break the world in ways no config option should be able to cause
 * (bedrock, portals). Everything after that depends on
 * {@link VanishingGroundConfig#blockSelectionMode()}:
 * <ul>
 *   <li>{@link BlockSelectionMode#BLACKLIST} - a block vanishes unless it
 *       is tagged {@link #PROTECTED}, is a fluid and fluids are protected,
 *       or has a block entity and those are protected.</li>
 *   <li>{@link BlockSelectionMode#WHITELIST} - a block vanishes only if it
 *       is tagged {@link #VANISHING}. The fluid/block-entity switches still
 *       apply as an additional restriction on top of the whitelist, so a
 *       mapmaker cannot accidentally whitelist a chest into oblivion.</li>
 * </ul>
 */
public final class BlockProtectionRegistry {

	/**
	 * Server operators / datapacks can add block ids here
	 * (data/vanishingground/tags/block/protected.json) without touching
	 * mod code. Only consulted in {@link BlockSelectionMode#BLACKLIST}.
	 */
	public static final TagKey<Block> PROTECTED = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
			Identifier.fromNamespaceAndPath(VanishingGroundMod.MOD_ID, "protected"));

	/**
	 * Server operators / datapacks list block ids here
	 * (data/vanishingground/tags/block/vanishing.json) to opt them into
	 * vanishing. Only consulted in {@link BlockSelectionMode#WHITELIST}.
	 */
	public static final TagKey<Block> VANISHING = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
			Identifier.fromNamespaceAndPath(VanishingGroundMod.MOD_ID, "vanishing"));

	public boolean canVanish(BlockState state, ServerLevel world, BlockPos pos, VanishingGroundConfig config) {
		if (state.isAir()) {
			// Nothing to remove; also guards against re-processing a
			// position that already vanished (e.g. via the delay scheduler).
			return false;
		}

		Block block = state.getBlock();

		if (block == Blocks.BEDROCK) {
			return false;
		}

		if (block instanceof NetherPortalBlock
				|| block instanceof EndPortalBlock
				|| block instanceof EndGatewayBlock) {
			return false;
		}

		if (config.blockSelectionMode() == BlockSelectionMode.WHITELIST) {
			if (!state.is(VANISHING)) {
				return false;
			}
		} else if (state.is(PROTECTED)) {
			return false;
		}

		if (!state.getFluidState().isEmpty() && !config.destroyFluids()) {
			// Covers water, lava and bubble columns (which are always
			// paired with a fluid state). Deliberately not touching the
			// fluid simulation by default - see design notes in the README.
			return false;
		}

		if (!config.destroyBlockEntities() && world.getBlockEntity(pos) != null) {
			return false;
		}

		return true;
	}
}
