package net.alyph.partychatmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alyph.partychatmod.util.IEntityDataSaver;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;



public class ReturnHomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.literal("return").executes(ReturnHomeCommand::run)));
    }
    // /home return

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        IEntityDataSaver player = (IEntityDataSaver) context.getSource().getPlayer();

        // not 0 means it contains SOMETHING
        int[] homepos = player.getPersistentData().getIntArray("homepos");

        if(homepos.length != 0) {
            int[] playerPos = player.getPersistentData().getIntArray("homepos");
            context.getSource().getPlayer().requestTeleport(playerPos[0], playerPos[1], playerPos[2]);

            context.getSource().sendFeedback(() -> Text.literal("Player returned Home!"), true);
            return 1;
        }
        else {
            context.getSource().sendFeedback(() -> Text.literal("No Home Position has been set!"), true);
            return -1;
        }
    }


}
