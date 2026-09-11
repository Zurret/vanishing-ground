package de.zurret.vanishingground.removal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Cosmetic-only warning shown shortly before a delayed removal executes, so
 * a player has a chance to notice the ground under them is about to
 * disappear rather than being surprised by it.
 * <p>
 * Purely visual/audible - firing this never changes game state, and a
 * failure here must never prevent the underlying removal from happening.
 * <p>
 * NOTE: in the 26.2 mappings this project builds against,
 * {@code SoundEvents} constants are plain {@code SoundEvent} references
 * (not {@code Holder<SoundEvent>}), and {@code ServerLevel#playSound}
 * accepts a {@code BlockPos} overload directly.
 */
public final class WarningEffects {

	private WarningEffects() {
	}

	public static void fire(ServerLevel world, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.05;
		double z = pos.getZ() + 0.5;

		world.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 0.3, 0.05, 0.3, 0.01);
		world.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.6F);
	}
}
