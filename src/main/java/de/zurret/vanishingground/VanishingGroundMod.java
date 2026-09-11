package de.zurret.vanishingground;

import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.command.VanishingGroundCommand;
import de.zurret.vanishingground.event.PlayerLifecycleHandler;
import de.zurret.vanishingground.event.PlayerMovementHandler;
import de.zurret.vanishingground.gamerule.ModGameRules;
import de.zurret.vanishingground.protection.BlockProtectionRegistry;
import de.zurret.vanishingground.removal.PendingRemovalScheduler;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vanishing Ground - solid ground a player walks away from disappears
 * once no player is standing on it anymore.
 * <p>
 * Author: Zurret (https://zurret.de)
 */
public final class VanishingGroundMod implements ModInitializer {

	public static final String MOD_ID = "vanishingground";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final SupportPositionTracker tracker = new SupportPositionTracker();
	private final BlockProtectionRegistry protectionRegistry = new BlockProtectionRegistry();
	private final PendingRemovalScheduler scheduler = new PendingRemovalScheduler();

	@Override
	public void onInitialize() {
		VanishingGroundConfig config = VanishingGroundConfig.loadOrCreate();
		VanishingGroundCommand.register(config);

		PlayerMovementHandler movementHandler =
				new PlayerMovementHandler(tracker, protectionRegistry, scheduler, config);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			movementHandler.tickPlayers(server);
			scheduler.tick(server, tracker, protectionRegistry, config);
		});

		PlayerLifecycleHandler.register(tracker);

		LOGGER.info("Vanishing Ground initialized (disabled by default - enable it in the config)");
	}
}
