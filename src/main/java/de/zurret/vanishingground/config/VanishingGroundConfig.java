package de.zurret.vanishingground.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.zurret.vanishingground.VanishingGroundMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistent configuration. Instances are deserialized by Gson, so every
 * field must tolerate being null after load from an older file.
 * {@link #normalize()} repairs that after every load.
 */
public final class VanishingGroundConfig {

	public enum BlockSelectionMode {
		BLACKLIST,
		WHITELIST
	}

	public enum RemovalMode {
		VANISH,
		RESTORE
	}

	private boolean enabled;
	private int delayTicks;
	private int warningTicks;
	private boolean destroyFluids;
	private boolean destroyBlockEntities;
	private BlockSelectionMode blockSelectionMode;
	private RemovalMode removalMode;
	private int restoreTicks;
	private Set<String> allowedDimensions;
	private boolean affectCreative;
	private boolean sneakProtects;
	private boolean vacateWhenFlyingOrMounted;
	private Set<String> immunePlayers;

	private transient Set<ResourceKey<Level>> allowedDimensionKeysCache;
	private transient Set<UUID> immunePlayerIdsCache;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(VanishingGroundMod.MOD_ID + ".json");

	public VanishingGroundConfig(boolean enabled, int delayTicks, int warningTicks, boolean destroyFluids,
			boolean destroyBlockEntities, BlockSelectionMode blockSelectionMode, RemovalMode removalMode,
			int restoreTicks, Set<String> allowedDimensions, boolean affectCreative, boolean sneakProtects,
			boolean vacateWhenFlyingOrMounted, Set<String> immunePlayers) {
		this.enabled = enabled;
		this.delayTicks = Math.max(0, delayTicks);
		this.warningTicks = Math.max(0, warningTicks);
		this.destroyFluids = destroyFluids;
		this.destroyBlockEntities = destroyBlockEntities;
		this.blockSelectionMode = blockSelectionMode;
		this.removalMode = removalMode;
		this.restoreTicks = Math.max(0, restoreTicks);
		this.allowedDimensions = new LinkedHashSet<>(allowedDimensions);
		this.affectCreative = affectCreative;
		this.sneakProtects = sneakProtects;
		this.vacateWhenFlyingOrMounted = vacateWhenFlyingOrMounted;
		this.immunePlayers = new LinkedHashSet<>(immunePlayers);
	}

	public boolean enabled() { return enabled; }
	public int delayTicks() { return delayTicks; }
	public int warningTicks() { return warningTicks; }
	public boolean destroyFluids() { return destroyFluids; }
	public boolean destroyBlockEntities() { return destroyBlockEntities; }
	public BlockSelectionMode blockSelectionMode() { return blockSelectionMode; }
	public RemovalMode removalMode() { return removalMode; }
	public int restoreTicks() { return restoreTicks; }
	public Set<String> allowedDimensions() { return Set.copyOf(allowedDimensions); }
	public boolean affectCreative() { return affectCreative; }
	public boolean sneakProtects() { return sneakProtects; }
	public boolean vacateWhenFlyingOrMounted() { return vacateWhenFlyingOrMounted; }
	public Set<String> immunePlayers() { return Set.copyOf(immunePlayers); }

	public boolean isDimensionAllowed(ResourceKey<Level> dimension) {
		if (allowedDimensions.isEmpty()) {
			return true;
		}
		return resolvedAllowedDimensions().contains(dimension);
	}

	public boolean isPlayerImmune(UUID playerId) {
		return resolvedImmunePlayers().contains(playerId);
	}

	private Set<ResourceKey<Level>> resolvedAllowedDimensions() {
		if (allowedDimensionKeysCache == null) {
			Set<ResourceKey<Level>> resolved = new HashSet<>();
			for (String id : allowedDimensions) {
				ResourceKey<Level> key = parseDimensionKey(id);
				if (key != null) {
					resolved.add(key);
				} else {
					VanishingGroundMod.LOGGER.warn("Ignoring invalid dimension id '{}' in allowedDimensions.", id);
				}
			}
			allowedDimensionKeysCache = resolved;
		}
		return allowedDimensionKeysCache;
	}

	private Set<UUID> resolvedImmunePlayers() {
		if (immunePlayerIdsCache == null) {
			Set<UUID> resolved = new HashSet<>();
			for (String raw : immunePlayers) {
				try {
					resolved.add(UUID.fromString(raw));
				} catch (IllegalArgumentException e) {
					VanishingGroundMod.LOGGER.warn("Ignoring invalid immune player UUID '{}'.", raw);
				}
			}
			immunePlayerIdsCache = resolved;
		}
		return immunePlayerIdsCache;
	}

	private static ResourceKey<Level> parseDimensionKey(String id) {
		int separator = id.indexOf(':');
		String namespace = separator >= 0 ? id.substring(0, separator) : "minecraft";
		String path = separator >= 0 ? id.substring(separator + 1) : id;
		try {
			return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(namespace, path));
		} catch (RuntimeException e) {
			return null;
		}
	}

	public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }
	public void setDelayTicks(int delayTicks) { this.delayTicks = Math.max(0, delayTicks); save(); }
	public void setWarningTicks(int warningTicks) { this.warningTicks = Math.max(0, warningTicks); save(); }
	public void setDestroyFluids(boolean destroyFluids) { this.destroyFluids = destroyFluids; save(); }
	public void setDestroyBlockEntities(boolean destroyBlockEntities) { this.destroyBlockEntities = destroyBlockEntities; save(); }
	public void setBlockSelectionMode(BlockSelectionMode blockSelectionMode) { this.blockSelectionMode = blockSelectionMode; save(); }
	public void setRemovalMode(RemovalMode removalMode) { this.removalMode = removalMode; save(); }
	public void setRestoreTicks(int restoreTicks) { this.restoreTicks = Math.max(0, restoreTicks); save(); }
	public void setAffectCreative(boolean affectCreative) { this.affectCreative = affectCreative; save(); }
	public void setSneakProtects(boolean sneakProtects) { this.sneakProtects = sneakProtects; save(); }
	public void setVacateWhenFlyingOrMounted(boolean vacateWhenFlyingOrMounted) {
		this.vacateWhenFlyingOrMounted = vacateWhenFlyingOrMounted;
		save();
	}

	public void addAllowedDimension(String dimensionId) {
		this.allowedDimensions.add(dimensionId);
		allowedDimensionKeysCache = null;
		save();
	}

	public void removeAllowedDimension(String dimensionId) {
		this.allowedDimensions.remove(dimensionId);
		allowedDimensionKeysCache = null;
		save();
	}

	public void clearAllowedDimensions() {
		this.allowedDimensions.clear();
		allowedDimensionKeysCache = null;
		save();
	}

	public void addImmunePlayer(UUID playerId) {
		this.immunePlayers.add(playerId.toString());
		immunePlayerIdsCache = null;
		save();
	}

	public void removeImmunePlayer(UUID playerId) {
		this.immunePlayers.remove(playerId.toString());
		immunePlayerIdsCache = null;
		save();
	}

	public void clearImmunePlayers() {
		this.immunePlayers.clear();
		immunePlayerIdsCache = null;
		save();
	}

	public static VanishingGroundConfig loadOrCreate() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				VanishingGroundConfig loaded = GSON.fromJson(reader, VanishingGroundConfig.class);
				if (loaded != null) {
					loaded.normalize();
					loaded.save();
					return loaded;
				}
			} catch (IOException | JsonSyntaxException e) {
				VanishingGroundMod.LOGGER.warn("Could not read {}, falling back to defaults.",
						CONFIG_PATH.getFileName(), e);
			}
		}
		VanishingGroundConfig defaults = defaults();
		defaults.save();
		return defaults;
	}

	public static VanishingGroundConfig defaults() {
		return new VanishingGroundConfig(false, 0, 0, false, false,
				BlockSelectionMode.BLACKLIST, RemovalMode.VANISH, 40, Set.of(),
				false, false, false, Set.of());
	}

	public boolean applyPreset(String name) {
		switch (name.toLowerCase(Locale.ROOT)) {
			case "parkour" -> {
				enabled = true;
				delayTicks = 8;
				warningTicks = 4;
				destroyFluids = false;
				destroyBlockEntities = false;
				blockSelectionMode = BlockSelectionMode.WHITELIST;
				removalMode = RemovalMode.RESTORE;
				restoreTicks = 80;
				affectCreative = false;
				sneakProtects = false;
				vacateWhenFlyingOrMounted = false;
			}
			case "challenge" -> {
				enabled = true;
				delayTicks = 0;
				warningTicks = 0;
				destroyFluids = false;
				destroyBlockEntities = false;
				blockSelectionMode = BlockSelectionMode.BLACKLIST;
				removalMode = RemovalMode.VANISH;
				restoreTicks = 40;
				affectCreative = true;
				sneakProtects = false;
				vacateWhenFlyingOrMounted = false;
			}
			case "safe" -> {
				enabled = false;
				delayTicks = 20;
				warningTicks = 10;
				destroyFluids = false;
				destroyBlockEntities = false;
				blockSelectionMode = BlockSelectionMode.WHITELIST;
				removalMode = RemovalMode.RESTORE;
				restoreTicks = 100;
				affectCreative = false;
				sneakProtects = true;
				vacateWhenFlyingOrMounted = false;
			}
			default -> {
				return false;
			}
		}
		save();
		return true;
	}

	private void normalize() {
		if (blockSelectionMode == null) {
			blockSelectionMode = BlockSelectionMode.BLACKLIST;
		}
		if (removalMode == null) {
			removalMode = RemovalMode.VANISH;
		}
		if (allowedDimensions == null) {
			allowedDimensions = new LinkedHashSet<>();
		} else {
			allowedDimensions = allowedDimensions.stream()
					.filter(id -> id != null && !id.isBlank())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		if (immunePlayers == null) {
			immunePlayers = new LinkedHashSet<>();
		} else {
			immunePlayers = immunePlayers.stream()
					.filter(id -> id != null && !id.isBlank())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		delayTicks = Math.max(0, delayTicks);
		warningTicks = Math.max(0, warningTicks);
		restoreTicks = Math.max(0, restoreTicks);
		allowedDimensionKeysCache = null;
		immunePlayerIdsCache = null;
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
