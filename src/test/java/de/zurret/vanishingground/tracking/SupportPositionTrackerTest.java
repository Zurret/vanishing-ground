package de.zurret.vanishingground.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure occupancy/reference-counting logic in
 * {@link SupportPositionTracker} in isolation, without spinning up a
 * Minecraft server. This is the class the mod's core safety guarantee
 * ("a block is never removed while a player still stands on it") rests
 * on, so it is tested independently of tick timing, protection rules or
 * world access.
 * <p>
 * Dimension keys are built locally via {@link ResourceKey#create} with a
 * throwaway namespace, deliberately avoiding {@code Level.OVERWORLD} /
 * {@code Level.NETHER}. Touching those constants loads the {@link Level}
 * class and, with it, registry machinery that is only initialized once
 * the game has bootstrapped - which does not happen in a plain unit test
 * process. {@code ResourceKey.create} performs no such lookup; it only
 * wraps a registry key and an id as a plain, comparable value object, so
 * it is safe to use in isolation exactly like {@link BlockPos}.
 */
class SupportPositionTrackerTest {

	private static final ResourceKey<Level> DIMENSION_A =
			ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("test", "dimension_a"));
	private static final ResourceKey<Level> DIMENSION_B =
			ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("test", "dimension_b"));

	private static final GlobalBlockPos POS_A = new GlobalBlockPos(DIMENSION_A, new BlockPos(0, 64, 0));
	private static final GlobalBlockPos POS_B = new GlobalBlockPos(DIMENSION_A, new BlockPos(10, 64, 10));
	private static final GlobalBlockPos POS_A_OTHER_DIMENSION = new GlobalBlockPos(DIMENSION_B, new BlockPos(0, 64, 0));

	private final UUID playerOne = UUID.randomUUID();
	private final UUID playerTwo = UUID.randomUUID();

	private SupportPositionTracker tracker;

	@BeforeEach
	void setUp() {
		tracker = new SupportPositionTracker();
	}

	@Test
	void firstRecordedPositionIsNeverReportedAsVacated() {
		Optional<GlobalBlockPos> result = tracker.updatePlayerPosition(playerOne, POS_A);

		assertTrue(result.isEmpty());
		assertTrue(tracker.isOccupied(POS_A));
		assertEquals(1, tracker.trackedPlayerCount());
	}

	@Test
	void singlePlayerLeavingVacatesThePosition() {
		tracker.updatePlayerPosition(playerOne, POS_A);

		Optional<GlobalBlockPos> vacated = tracker.updatePlayerPosition(playerOne, POS_B);

		assertEquals(Optional.of(POS_A), vacated);
		assertFalse(tracker.isOccupied(POS_A));
		assertTrue(tracker.isOccupied(POS_B));
	}

	@Test
	void movingToTheSamePositionIsANoOp() {
		tracker.updatePlayerPosition(playerOne, POS_A);

		Optional<GlobalBlockPos> result = tracker.updatePlayerPosition(playerOne, POS_A);

		assertTrue(result.isEmpty());
		assertTrue(tracker.isOccupied(POS_A));
	}

	@Test
	void positionStaysOccupiedWhileAnyPlayerRemainsOnIt() {
		tracker.updatePlayerPosition(playerOne, POS_A);
		tracker.updatePlayerPosition(playerTwo, POS_A);

		Optional<GlobalBlockPos> resultAfterFirstLeaves = tracker.updatePlayerPosition(playerOne, POS_B);

		assertTrue(resultAfterFirstLeaves.isEmpty(), "Position must stay occupied while playerTwo still stands on it");
		assertTrue(tracker.isOccupied(POS_A));
	}

	@Test
	void positionVacatesOnlyOnceTheLastPlayerLeaves() {
		tracker.updatePlayerPosition(playerOne, POS_A);
		tracker.updatePlayerPosition(playerTwo, POS_A);
		tracker.updatePlayerPosition(playerOne, POS_B);

		Optional<GlobalBlockPos> resultAfterLastLeaves = tracker.updatePlayerPosition(playerTwo, POS_B);

		assertEquals(Optional.of(POS_A), resultAfterLastLeaves);
		assertFalse(tracker.isOccupied(POS_A));
	}

	@Test
	void disconnectingReleasesTheTrackedPosition() {
		tracker.updatePlayerPosition(playerOne, POS_A);

		Optional<GlobalBlockPos> vacated = tracker.clearPlayer(playerOne);

		assertEquals(Optional.of(POS_A), vacated);
		assertFalse(tracker.isOccupied(POS_A));
		assertEquals(0, tracker.trackedPlayerCount());
	}

	@Test
	void clearingAPlayerWithNoTrackedPositionIsHarmless() {
		Optional<GlobalBlockPos> vacated = tracker.clearPlayer(playerOne);

		assertTrue(vacated.isEmpty());
	}

	@Test
	void sameCoordinatesInDifferentDimensionsAreDistinctPositions() {
		tracker.updatePlayerPosition(playerOne, POS_A);

		Optional<GlobalBlockPos> vacated = tracker.updatePlayerPosition(playerOne, POS_A_OTHER_DIMENSION);

		assertEquals(Optional.of(POS_A), vacated);
		assertTrue(tracker.isOccupied(POS_A_OTHER_DIMENSION));
		assertFalse(tracker.isOccupied(POS_A));
	}

	@Test
	void occupiedPositionCountReflectsDistinctPositionsOnly() {
		tracker.updatePlayerPosition(playerOne, POS_A);
		tracker.updatePlayerPosition(playerTwo, POS_A);

		assertEquals(1, tracker.occupiedPositionCount());
		assertEquals(2, tracker.trackedPlayerCount());
	}
}
