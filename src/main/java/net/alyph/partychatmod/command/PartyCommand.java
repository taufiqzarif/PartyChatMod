package net.alyph.partychatmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.alyph.partychatmod.PartyChatMod;
import net.alyph.partychatmod.data.NameAndUUID;
import net.alyph.partychatmod.data.Party;
import net.alyph.partychatmod.util.IEntityDataSaver;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;


public class PartyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register((LiteralArgumentBuilder<ServerCommandSource>) ((LiteralArgumentBuilder<ServerCommandSource>)
                CommandManager.literal("p").requires(serverCommandSource -> true))
                .then(CommandManager.argument("Party", StringArgumentType.string())
                        .suggests(PartyCommand::suggestParties)
                        .then(CommandManager.argument("message", MessageArgumentType.message())
                                .executes(PartyCommand::message)
                        )
                )
        );

        dispatcher.register((LiteralArgumentBuilder<ServerCommandSource>) ((LiteralArgumentBuilder<ServerCommandSource>)
                CommandManager.literal("party").requires(serverCommandSource -> true))
                .then(CommandManager.literal("message")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("message", MessageArgumentType.message())
                                        .executes(PartyCommand::message)
                                )
                )
        )
                .then(CommandManager.literal("create").requires(serverCommandSource -> true)
                        .then(CommandManager.argument("partyname", StringArgumentType.string())
                                .executes((context) -> create(context))
                        )
                )
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .executes(PartyCommand::join)
                        )
                )
        );
    }

    private static int create(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Party logic here
        IEntityDataSaver player = (IEntityDataSaver) context.getSource().getPlayer();
        Set<NameAndUUID> players = new HashSet<>();
        String newParty = StringArgumentType.getString(context, "partyname");
        UUID leaderUUID = context.getSource().getPlayer().getUuid();
        boolean isExist = false;

        // Check if party is already exist
        for(Party p : PartyChatMod.partyChatFile.parties) {
            if(p.getPartyName().equals(newParty)) {
                isExist = true;
                break;
            }
        }

        if(isExist) {
            context.getSource().sendError(Text.literal("Party " + newParty + " already exist!"));
            return 0;
        }

        if(player != null) {
            players.add(new NameAndUUID((Entity) player));
        }

        // Add party to file
        PartyChatMod.partyChatFile.addParty(
                new Party(newParty, players, leaderUUID)
        );

        context.getSource().sendFeedback(() -> Text.literal("Party " + newParty + " created!"), true);
        return 1;
    }

    // Show player's available parties
    private static CompletableFuture<Suggestions> suggestParties(CommandContext<ServerCommandSource> context, SuggestionsBuilder sBuilder) throws CommandSyntaxException {
        for(String s : PartyChatMod.partyChatFile.getPartiesForPlayer(context.getSource().getPlayer())) {
            sBuilder.suggest(s);
        }
        return sBuilder.buildFuture();
    }

    // Handle message
    private static int message(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for(Party p : PartyChatMod.partyChatFile.parties) {
            if(p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if(party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist!"));
            return 0;
        }

        UUID senderUUID = null;

        senderUUID = source.getPlayer().getUuid();

        if(senderUUID != null) {
            if(!party.getPlayerUUIDList().contains(senderUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\"!"));
                return 0;
            }
        }

        MutableText partyMessage = Text.literal("<" + partyName + "> ").styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/p " + partyName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(
                        partyName + "\n" +
                        "Click to suggest command"
                )))
        );

        Text message = partyMessage.append(Text.literal(
                " " + source.getName() + ": "
        )).append(MessageArgumentType.getMessage(context, "message"));

        List<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();
        int i = 0;

        for(ServerPlayerEntity player : players) {
            if(party.getPlayerUUIDList().contains(player.getUuid())) {
                player.sendMessageToClient(message, false);
                i++;
            }
        }

        return i;
    }

    private static int join(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for(Party p : PartyChatMod.partyChatFile.parties) {
            if(p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if(party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist!"));
            return 0;
        }

        UUID senderUUID = null;

        senderUUID = source.getPlayer().getUuid();

        if(senderUUID != null) {
            if(party.getPlayerUUIDList().contains(senderUUID)) {
                source.sendError(Text.literal("You are already in party \"" + partyName + "\"!"));
                return 0;
            }
        }

        ServerPlayerEntity player = null;

        player = source.getPlayer();

        party.addPlayer(player);

        source.sendFeedback(() -> Text.literal("Joined party \"" + partyName + "\"!"), true);

        return 1;
    }

    private static int leave(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for (Party p : PartyChatMod.partyChatFile.parties) {
            if (p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if(party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist!"));
            return 0;
        }

        UUID currentPlayerUUID = null;
        currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\"!"));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        source.sendFeedback(() -> Text.literal("Left party \"" + partyName + "\"!"), true);
        PartyChatMod.partyChatFile.removePlayerFromParty(partyName, player);
        return 1;
    }

    public static int invite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for (Party p : PartyChatMod.partyChatFile.parties) {
            if (p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if(party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist!"));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\"!"));
                return 0;
            }
        }

        Collection<ServerPlayerEntity> playerEntities = EntityArgumentType.getPlayers(context, "Players");

        final Party newParty = party;

        MutableText partyText = Text.literal(partyName).styled(style -> style
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/party join " + partyName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(
                        "Party: " + partyName + "\n" +
                        "Click to join party"
                )))
        );

        boolean isInviteSent = false;
        MutableText message = Text.literal(source.getPlayer().getEntityName() + " has invited to join Party ").append(partyText);

        message.append(Text.literal(
                "\nJoin this party with the command "
        ).styled(style -> style.withColor(Formatting.GRAY)));

        message.append(Text.literal(
                "/party join " + partyName
                ).styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));

        for (ServerPlayerEntity player : playerEntities) {
            if(!party.getPlayerUUIDList().contains(player.getUuid())) {
                player.sendMessageToClient(message, false);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                isInviteSent = true;
            }
        }

        if(isInviteSent) {
            source.sendFeedback(() -> Text.literal("Invited player to party \"" + partyName + "\"!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Player already in party \"" + partyName + "\"!"));
        }
        return 0;
    }
}
