package de.zurret.vanishingground.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.zurret.vanishingground.VanishingGroundMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent, low-frequency configuration. Deliberately kept separate from
 * the {@code vanishingGround} / {@code vanishingGroundDelay} game rules:
 * the game rules are per-world toggles a player or admin flips during play,
 * while these are structural policy decisions (what counts as protected)
 * that a server operator sets once.
 *
 * @param enabled              if {@code false} (default), the mod does not
 *                             remove blocks.
 * @param delayTicks           ticks to wait before removing a vacated block.
 * @param destroyFluids        if {@code false} (default), water and lava
 *                              are never removed by this mod, regardless
 *                              of whether a player was standing on them.
 * @param destroyBlockEntities if {@code false} (default), any block with
 *                              a block entity (chests, furnaces, signs,
 *                              ...) is protected, to avoid silently
 *                              destroying stored items or state.
 */
public record VanishingGroundConfig(boolean enabled, int delayTicks, boolean destroyFluids,
		boolean destroyBlockEntities) {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(VanishingGroundMod.MOD_ID + ".json");

	public static VanishingGroundConfig loadOrCreate() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				VanishingGroundConfig loaded = GSON.fromJson(reader, VanishingGroundConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | JsonSyntaxException e) {
				VanishingGroundMod.LOGGER.warn("Could not read {}, falling back to defaults.",
						CONFIG_PATH.getFileName(), e);
			}
		}

		VanishingGroundConfig defaults = new VanishingGroundConfig(false, 0, false, false);
		defaults.save();
		return defaults;
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			VanishingGroundMod.LOGGER.warn("Could not write {}.", CONFIG_PATH.getFileName(), e);
		}
	}
}
