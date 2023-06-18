package net.alyph.partychatmod.util;

import net.alyph.partychatmod.command.CreatePartyCommand;
import net.alyph.partychatmod.command.ReturnHomeCommand;
import net.alyph.partychatmod.command.SetHomeCommand;
import net.alyph.partychatmod.event.ModPlayerEventCopyFrom;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class ModRegistries {
    public static void registerModStuffs() {
        registerCommands();
        registerEvents();
    }
    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(SetHomeCommand::register);
        CommandRegistrationCallback.EVENT.register(ReturnHomeCommand::register);
        CommandRegistrationCallback.EVENT.register(CreatePartyCommand::register);
    }

    private static void registerEvents() {
        ServerPlayerEvents.COPY_FROM.register(new ModPlayerEventCopyFrom());
    }
}
