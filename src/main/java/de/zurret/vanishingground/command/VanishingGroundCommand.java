package de.zurret.vanishingground.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import de.zurret.vanishingground.config.VanishingGroundConfig;

public final class VanishingGroundCommand {

	private VanishingGroundCommand() {
	}

	public static void register(VanishingGroundConfig config) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				register(dispatcher, config));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
			VanishingGroundConfig config) {
		var root = Commands.literal("vanishingground")
				.requires(source -> source.permissions() instanceof LevelBasedPermissionSet permissions
						&& permissions.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS));
		root.then(Commands.literal("enable")
				.executes(context -> setEnabled(context, config, true)));
		root.then(Commands.literal("disable")
				.executes(context -> setEnabled(context, config, false)));
		root.then(Commands.literal("status")
				.executes(context -> status(context, config)));
		root.then(Commands.literal("delay")
				.then(Commands.argument("ticks", IntegerArgumentType.integer(0))
						.executes(context -> setDelay(context, config))));

		var fluids = Commands.literal("fluids");
		fluids.then(Commands.literal("allow")
				.executes(context -> setFluids(context, config, true)));
		fluids.then(Commands.literal("protect")
				.executes(context -> setFluids(context, config, false)));
		root.then(fluids);

		var blockEntities = Commands.literal("block_entities");
		blockEntities.then(Commands.literal("allow")
				.executes(context -> setBlockEntities(context, config, true)));
		blockEntities.then(Commands.literal("protect")
				.executes(context -> setBlockEntities(context, config, false)));
		root.then(blockEntities);

		dispatcher.register(root);
	}

	private static int setEnabled(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config, boolean enabled) {
		config.setEnabled(enabled);
		context.getSource().sendSuccess(() -> Component.literal(
				"Vanishing Ground " + (enabled ? "enabled." : "disabled.")), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config) {
		context.getSource().sendSuccess(() -> Component.literal(String.format(
				"Vanishing Ground: %s, delay: %d ticks, fluids: %s, block entities: %s",
				config.enabled() ? "enabled" : "disabled", config.delayTicks(),
				config.destroyFluids() ? "allowed" : "protected",
				config.destroyBlockEntities() ? "allowed" : "protected")), false);
		return 1;
	}

	private static int setDelay(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config) {
		int ticks = IntegerArgumentType.getInteger(context, "ticks");
		config.setDelayTicks(ticks);
		context.getSource().sendSuccess(() -> Component.literal(
				"Vanishing Ground delay set to " + ticks + " ticks."), true);
		return 1;
	}

	private static int setFluids(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config, boolean allowed) {
		config.setDestroyFluids(allowed);
		context.getSource().sendSuccess(() -> Component.literal(
				"Fluid removal " + (allowed ? "allowed." : "protected.")), true);
		return 1;
	}

	private static int setBlockEntities(CommandContext<CommandSourceStack> context,
			VanishingGroundConfig config, boolean allowed) {
		config.setDestroyBlockEntities(allowed);
		context.getSource().sendSuccess(() -> Component.literal(
				"Block entity removal " + (allowed ? "allowed." : "protected.")), true);
		return 1;
	}
}