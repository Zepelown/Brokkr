package org.brokkr.enhancement.event;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.brokkr.enhancement.command.BrokkrCommand;
import org.brokkr.enhancement.command.EnhanceWeaponCommand;

public final class EnhancementCommandEvents {
    private EnhancementCommandEvents() {
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        EnhanceWeaponCommand.register(event.getDispatcher());
        BrokkrCommand.register(event.getDispatcher());
    }
}
