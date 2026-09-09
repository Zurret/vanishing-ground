package de.zurret.vanishingground.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Identifies a block position across dimensions.
 * <p>
 * {@code record}s already provide value-based {@link #equals(Object)} and
 * {@link #hashCode()}, which is required here since instances are used as
 * hash map keys in {@link SupportPositionTracker}.
 */
public record GlobalBlockPos(ResourceKey<Level> dimension, BlockPos pos) {
}
