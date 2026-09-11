package de.zurret.vanishingground.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Identifies a block position across dimensions.
 * The stored {@link BlockPos} is always made immutable so the value is
 * safe as a hash map key even if the caller passed a mutable instance
 * such as {@code BlockPos.MutableBlockPos} from {@code getOnPos()}.
 */
public record GlobalBlockPos(ResourceKey<Level> dimension, BlockPos pos) {

	public GlobalBlockPos {
		pos = pos.immutable();
	}
}
