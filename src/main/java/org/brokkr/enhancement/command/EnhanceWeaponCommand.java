package org.brokkr.enhancement.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.brokkr.enhancement.EnhancementMessages;
import org.brokkr.enhancement.EnhancementResult;
import org.brokkr.enhancement.WeaponEnhancementService;
import org.brokkr.enhancement.text.EnhancementTextKeys;

public final class EnhanceWeaponCommand {
    private EnhanceWeaponCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enhanceweapon")
                .executes(context -> enhance(context.getSource()))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 20))
                                .executes(context -> setLevel(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "level")
                                )))));
    }

    private static int enhance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EnhancementResult result = WeaponEnhancementService.attempt(player);
        player.sendSystemMessage(EnhancementMessages.forResult(result));
        return result.type() == EnhancementResult.Type.SUCCESS ? 1 : 0;
    }

    private static int setLevel(CommandSourceStack source, int level) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EnhancementResult result = WeaponEnhancementService.setLevel(player.getMainHandItem(), level);
        if (result.type() == EnhancementResult.Type.SUCCESS) {
            player.sendSystemMessage(Component.translatable(EnhancementTextKeys.COMMAND_DEBUG_SET, result.newLevel()));
            return 1;
        }

        player.sendSystemMessage(EnhancementMessages.forResult(result));
        return 0;
    }
}
