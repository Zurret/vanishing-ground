package de.zurret.vanishingground.gamerule;

import de.zurret.vanishingground.config.VanishingGroundConfig;

/**
 * Runtime toggles, controllable per-world via {@code /gamerule} without
 * editing config files or restarting the server.
 */
public final class ModGameRules {

	/** Master switch. Default off so installing the mod alone changes nothing. */
	private ModGameRules() {
	}

	public static boolean isEnabled(VanishingGroundConfig config) {
		return config.enabled();
	}

	public static int delayTicks(VanishingGroundConfig config) {
		return Math.max(0, config.delayTicks());
	}
}
