package org.brokkr.enhancement.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.brokkr.enhancement.menu.EnhancementMenu;
import org.brokkr.enhancement.text.EnhancementTextKeys;
import org.jetbrains.annotations.Nullable;

public final class BrokkrCommand {
    private BrokkrCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("brokkr")
                .executes(context -> open(context.getSource())));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ((IPlayerExtension) player).openMenu(new Provider(), ignored -> {
        });
        return 1;
    }

    private static final class Provider implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable(EnhancementTextKeys.SCREEN_TITLE);
        }

        @Override
        public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new EnhancementMenu(containerId, inventory);
        }
    }
}
