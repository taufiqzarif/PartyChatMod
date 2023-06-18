package net.alyph.partychatmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alyph.partychatmod.data.Party;
import net.alyph.partychatmod.util.IEntityDataSaver;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;

public class CreatePartyCommand {
    // This is a map of party IDs to parties. It can be used to find a party by its ID.
    public static final Map<UUID, Party> parties = new HashMap<>();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("createparty")
                .then(argument("partyname", StringArgumentType.word())
                        .executes(CreatePartyCommand::run)));

    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Party logic here
        IEntityDataSaver player = (IEntityDataSaver) context.getSource().getPlayer();
        String partyName = StringArgumentType.getString(context, "partyname");
        UUID playerUUID = context.getSource().getPlayer().getUuid();

        // Check if player is already in a party
        for(Party party : parties.values()) {
            if(party.isMember(playerUUID) || party.isLeader(playerUUID)) {
                context.getSource().sendFeedback(() -> Text.literal("You are already in a party!"), true);
                return 0;
            }
        }

        // Create a new party and add it to the map
        UUID partyID = UUID.randomUUID();
        Party newParty = new Party(partyID, partyName, playerUUID);
        parties.put(partyID, newParty);

        context.getSource().sendFeedback(() -> Text.literal("Party " + partyName + " created!"), true);
        return 1;
    }
}
