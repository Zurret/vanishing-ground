package de.zurret.vanishingground.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.zurret.vanishingground.config.VanishingGroundConfig;
import de.zurret.vanishingground.config.VanishingGroundConfig.BlockSelectionMode;
import de.zurret.vanishingground.config.VanishingGroundConfig.RemovalMode;
import de.zurret.vanishingground.removal.PendingRemovalScheduler;
import de.zurret.vanishingground.removal.RestoreScheduler;
import de.zurret.vanishingground.tracking.SupportPositionTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Locale;

/**
 * In-game configuration and inspection interface.
 * Mutating subcommands persist immediately.
 */
public final class VanishingGroundCommand {

	private VanishingGroundCommand() {
	}

	public static void register(VanishingGroundConfig config, SupportPositionTracker tracker,
			PendingRemovalScheduler removalScheduler, RestoreScheduler restoreScheduler) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			register(dispatcher, "vanishingground", config, tracker, removalScheduler, restoreScheduler);
			register(dispatcher, "vg", config, tracker, removalScheduler, restoreScheduler);
		});
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String rootName,
			VanishingGroundConfig config, SupportPositionTracker tracker,
			PendingRemovalScheduler removalScheduler, RestoreScheduler restoreScheduler) {
		var root = Commands.literal(rootName)
				.requires(source -> source.permissions() instanceof LevelBasedPermissionSet permissions
						&& permissions.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS));

		root.executes(context -> help(context));
		root.then(Commands.literal("help").executes(context -> help(context)));
		root.then(Commands.literal("enable").executes(context -> setEnabled(context, config, true)));
		root.then(Commands.literal("disable").executes(context -> setEnabled(context, config, false)));
		root.then(Commands.literal("status").executes(context -> status(context, config)));
		root.then(Commands.literal("debug")
				.executes(context -> debug(context, config, tracker, removalScheduler, restoreScheduler)));
		root.then(Commands.literal("delay")
				.then(Commands.argument("ticks", IntegerArgumentType.integer(0))
						.executes(context -> setDelay(context, config))));
		root.then(Commands.literal("warning")
				.then(Commands.argument("ticks", IntegerArgumentType.integer(0))
						.executes(context -> setWarning(context, config))));

		var fluids = Commands.literal("fluids");
		fluids.then(Commands.literal("allow").executes(context -> setFluids(context, config, true)));
		fluids.then(Commands.literal("protect").executes(context -> setFluids(context, config, false)));
		root.then(fluids);

		var blockEntities = Commands.literal("block_entities");
		blockEntities.then(Commands.literal("allow").executes(context -> setBlockEntities(context, config, true)));
		blockEntities.then(Commands.literal("protect").executes(context -> setBlockEntities(context, config, false)));
		root.then(blockEntities);

		var mode = Commands.literal("mode");
		mode.then(Commands.literal("blacklist")
				.executes(context -> setBlockSelectionMode(context, config, BlockSelectionMode.BLACKLIST)));
		mode.then(Commands.literal("whitelist")
				.executes(context -> setBlockSelectionMode(context, config, BlockSelectionMode.WHITELIST)));
		root.then(mode);

		var removalMode = Commands.literal("removalmode");
		removalMode.then(Commands.literal("vanish")
				.executes(context -> setRemovalMode(context, config, RemovalMode.VANISH)));
		removalMode.then(Commands.literal("restore")
				.executes(context -> setRemovalMode(context, config, RemovalMode.RESTORE)));
		root.then(removalMode);

		root.then(Commands.literal("restore")
				.then(Commands.argument("ticks", IntegerArgumentType.integer(1))
						.executes(context -> setRestoreTicks(context, config))));

		var dimensions = Commands.literal("dimensions");
		dimensions.then(Commands.literal("add")
				.then(Commands.argument("dimension", StringArgumentType.string())
						.executes(context -> addDimension(context, config))));
		dimensions.then(Commands.literal("remove")
				.then(Commands.argument("dimension", StringArgumentType.string())
						.executes(context -> removeDimension(context, config))));
		dimensions.then(Commands.literal("clear").executes(context -> clearDimensions(context, config)));
		dimensions.then(Commands.literal("list").executes(context -> listDimensions(context, config)));
		root.then(dimensions);

		var creative = Commands.literal("creative");
		creative.then(Commands.literal("affect").executes(context -> setCreative(context, config, true)));
		creative.then(Commands.literal("ignore").executes(context -> setCreative(context, config, false)));
		root.then(creative);

		var sneak = Commands.literal("sneak");
		sneak.then(Commands.literal("protect").executes(context -> setSneak(context, config, true)));
		sneak.then(Commands.literal("normal").executes(context -> setSneak(context, config, false)));
		root.then(sneak);

		var fly = Commands.literal("flyleave");
		fly.then(Commands.literal("vanish").executes(context -> setFlyLeave(context, config, true)));
		fly.then(Commands.literal("keep").executes(context -> setFlyLeave(context, config, false)));
		root.then(fly);

		var immune = Commands.literal("immune");
		immune.then(Commands.literal("add")
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> addImmune(context, config))));
		immune.then(Commands.literal("remove")
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> removeImmune(context, config))));
		immune.then(Commands.literal("clear").executes(context -> clearImmune(context, config)));
		immune.then(Commands.literal("list").executes(context -> listImmune(context, config)));
		root.then(immune);

		var preset = Commands.literal("preset");
		preset.then(Commands.literal("parkour").executes(context -> applyPreset(context, config, "parkour")));
		preset.then(Commands.literal("challenge").executes(context -> applyPreset(context, config, "challenge")));
		preset.then(Commands.literal("safe").executes(context -> applyPreset(context, config, "safe")));
		root.then(preset);

		dispatcher.register(root);
	}

	private static int help(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(() -> Component.literal("""
				Vanishing Ground commands (/vanishingground or /vg)
				enable | disable | status | debug | help
				delay <ticks> | warning <ticks> | restore <ticks>
				fluids allow|protect
				block_entities allow|protect
				mode blacklist|whitelist
				removalmode vanish|restore
				dimensions add|remove|clear|list
				creative affect|ignore
				sneak protect|normal
				flyleave vanish|keep
				immune add|remove|clear|list
				preset parkour|challenge|safe"""), false);
		return 1;
	}

	private static int setEnabled(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean enabled) {
		config.setEnabled(enabled);
		context.getSource().sendSuccess(() -> Component.literal(
				"Vanishing Ground " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
				"""
				Vanishing Ground: %s
				Block selection: %s, removal mode: %s
				Delay: %d ticks, warning: %d ticks, restore: %d ticks
				Fluids: %s, block entities: %s
				Creative players: %s, sneak: %s, fly/mount leave: %s
				Allowed dimensions: %s
				Immune players: %d""",
				config.enabled() ? "enabled" : "disabled",
				config.blockSelectionMode(), config.removalMode(),
				config.delayTicks(), config.warningTicks(), config.restoreTicks(),
				config.destroyFluids() ? "allowed" : "protected",
				config.destroyBlockEntities() ? "allowed" : "protected",
				config.affectCreative() ? "affected" : "ignored",
				config.sneakProtects() ? "protects vacated block" : "normal",
				config.vacateWhenFlyingOrMounted() ? "vanishes last block" : "keeps last block",
				config.allowedDimensions().isEmpty() ? "all" : config.allowedDimensions(),
				config.immunePlayers().size())), false);
		return 1;
	}

	private static int debug(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			SupportPositionTracker tracker, PendingRemovalScheduler removalScheduler,
			RestoreScheduler restoreScheduler) {
		context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
				"""
				Vanishing Ground Debug
				Enabled: %s
				Tracked players: %d
				Occupied positions: %d
				Pending removals: %d
				Pending restores: %d""",
				config.enabled(),
				tracker.trackedPlayerCount(),
				tracker.occupiedPositionCount(),
				removalScheduler.pendingCount(),
				restoreScheduler.pendingCount())), false);
		return 1;
	}

	private static int setDelay(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		int ticks = IntegerArgumentType.getInteger(context, "ticks");
		config.setDelayTicks(ticks);
		context.getSource().sendSuccess(() -> Component.literal(
				"Vanishing Ground delay set to " + ticks + " ticks."), true);
		return 1;
	}

	private static int setWarning(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		int ticks = IntegerArgumentType.getInteger(context, "ticks");
		config.setWarningTicks(ticks);
		context.getSource().sendSuccess(() -> Component.literal(
				"Vanishing Ground warning set to " + ticks + " ticks before removal."), true);
		return 1;
	}

	private static int setFluids(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean allowed) {
		config.setDestroyFluids(allowed);
		context.getSource().sendSuccess(() -> Component.literal(
				"Fluid removal " + (allowed ? "allowed." : "protected.")), true);
		return 1;
	}

	private static int setBlockEntities(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean allowed) {
		config.setDestroyBlockEntities(allowed);
		context.getSource().sendSuccess(() -> Component.literal(
				"Block entity removal " + (allowed ? "allowed." : "protected.")), true);
		return 1;
	}

	private static int setBlockSelectionMode(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config, BlockSelectionMode mode) {
		config.setBlockSelectionMode(mode);
		context.getSource().sendSuccess(() -> Component.literal(
				"Block selection mode set to " + mode + "."), true);
		return 1;
	}

	private static int setRemovalMode(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			RemovalMode mode) {
		config.setRemovalMode(mode);
		context.getSource().sendSuccess(() -> Component.literal(
				"Removal mode set to " + mode + "."), true);
		return 1;
	}

	private static int setRestoreTicks(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		int ticks = IntegerArgumentType.getInteger(context, "ticks");
		config.setRestoreTicks(ticks);
		context.getSource().sendSuccess(() -> Component.literal(
				"Restore delay set to " + ticks + " ticks. Has no effect unless removalmode is restore."), true);
		return 1;
	}

	private static int addDimension(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		String dimension = StringArgumentType.getString(context, "dimension");
		config.addAllowedDimension(dimension);
		context.getSource().sendSuccess(() -> Component.literal(
				"Added '" + dimension + "' to allowed dimensions."), true);
		return 1;
	}

	private static int removeDimension(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		String dimension = StringArgumentType.getString(context, "dimension");
		config.removeAllowedDimension(dimension);
		context.getSource().sendSuccess(() -> Component.literal(
				"Removed '" + dimension + "' from allowed dimensions."), true);
		return 1;
	}

	private static int clearDimensions(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		config.clearAllowedDimensions();
		context.getSource().sendSuccess(() -> Component.literal(
				"Allowed dimensions cleared. Vanishing Ground now applies to all dimensions."), true);
		return 1;
	}

	private static int listDimensions(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		context.getSource().sendSuccess(() -> Component.literal(
				config.allowedDimensions().isEmpty()
						? "No dimension restriction. All dimensions are allowed."
						: "Allowed dimensions: " + config.allowedDimensions()), false);
		return 1;
	}

	private static int setCreative(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean affect) {
		config.setAffectCreative(affect);
		context.getSource().sendSuccess(() -> Component.literal(
				affect ? "Creative players are now affected." : "Creative players are now ignored."), true);
		return 1;
	}

	private static int setSneak(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean protect) {
		config.setSneakProtects(protect);
		context.getSource().sendSuccess(() -> Component.literal(
				protect ? "Sneaking now keeps the block you leave." : "Sneaking no longer protects vacated blocks."), true);
		return 1;
	}

	private static int setFlyLeave(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			boolean vanish) {
		config.setVacateWhenFlyingOrMounted(vanish);
		context.getSource().sendSuccess(() -> Component.literal(
				vanish ? "Taking off or mounting now vacates the last block."
						: "Taking off or mounting no longer removes the last block."), true);
		return 1;
	}

	private static ServerPlayer findOnlinePlayer(CommandContext<CommandSourceStack> context, String name) {
		return context.getSource().getServer().getPlayerList().getPlayerByName(name);
	}

	private static int addImmune(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		String name = StringArgumentType.getString(context, "player");
		ServerPlayer player = findOnlinePlayer(context, name);
		if (player == null) {
			context.getSource().sendFailure(Component.literal("No online player named '" + name + "'."));
			return 0;
		}
		config.addImmunePlayer(player.getUUID());
		context.getSource().sendSuccess(() -> Component.literal(
				"Added " + player.getName().getString() + " to the immune list."), true);
		return 1;
	}

	private static int removeImmune(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		String name = StringArgumentType.getString(context, "player");
		ServerPlayer player = findOnlinePlayer(context, name);
		if (player == null) {
			context.getSource().sendFailure(Component.literal("No online player named '" + name + "'."));
			return 0;
		}
		config.removeImmunePlayer(player.getUUID());
		context.getSource().sendSuccess(() -> Component.literal(
				"Removed " + player.getName().getString() + " from the immune list."), true);
		return 1;
	}

	private static int clearImmune(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		config.clearImmunePlayers();
		context.getSource().sendSuccess(() -> Component.literal("Immune player list cleared."), true);
		return 1;
	}

	private static int listImmune(CommandContext<CommandSourceStack> context, VanishingGroundConfig config) {
		context.getSource().sendSuccess(() -> Component.literal(
				config.immunePlayers().isEmpty()
						? "No immune players."
						: "Immune player UUIDs: " + config.immunePlayers()), false);
		return 1;
	}

	private static int applyPreset(CommandContext<CommandSourceStack> context, VanishingGroundConfig config,
			String name) {
		if (!config.applyPreset(name)) {
			context.getSource().sendFailure(Component.literal("Unknown preset '" + name + "'."));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(
				"Applied preset '" + name + "'. Use /vg status to review."), true);
		return 1;
	}
}
