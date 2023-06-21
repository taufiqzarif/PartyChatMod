package net.alyph.partychatmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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

                // Send message to a party
                .then(CommandManager.literal("message")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("message", MessageArgumentType.message())
                                        .executes(PartyCommand::message)
                                )
                        )
                )

                // Create a new party
                .then(CommandManager.literal("create").requires(serverCommandSource -> true)
                        .then(CommandManager.argument("partyname", StringArgumentType.string())
                                .executes((context) -> create(context))
                        )
                )

                // Joins a party
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .executes(PartyCommand::join)
                        )
                )

                // Leaves party
                .then(CommandManager.literal("leave")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .executes(PartyCommand::leave)
                        )
                )

                // Invite player to join the party
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("Players", EntityArgumentType.players())
                                        .executes(PartyCommand::invite)
                                )
                        )
                )

                // Kick player out from the party
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("Players", EntityArgumentType.players())
                                        .executes(PartyCommand::kick)
                                )
                        )
                )

                // Ban player from the party
                .then(CommandManager.literal("ban")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("Players", EntityArgumentType.players())
                                        .executes(PartyCommand::ban))
                                .then(CommandManager.argument("PlayerString", StringArgumentType.string())
                                        .executes(PartyCommand::banOffline))
                        )
                )

                // Unban player from the party
                .then(CommandManager.literal("unban")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .then(CommandManager.argument("Players", EntityArgumentType.players())
                                        .executes(PartyCommand::unban)
                                )
                        )
                )

                // List all players in party
                .then(CommandManager.literal("players")
                        .then(CommandManager.argument("Party", StringArgumentType.string())
                                .suggests(PartyCommand::suggestParties)
                                .executes(PartyCommand::players)
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
            context.getSource().sendError(Text.literal("Party " + newParty + " already exist"));
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
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist"));
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

        MutableText partyMessage = Text.literal("[" + partyName + "] ").styled(style -> style
                .withColor(Formatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/p " + partyName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(
                        partyName + "\n" +
                        "Click to send message"
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
        List<ServerPlayerEntity> allOnlinePlayers = source.getServer().getPlayerManager().getPlayerList();


        for(Party p : PartyChatMod.partyChatFile.parties) {
            if(p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if(party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist"));
            return 0;
        }

        UUID senderUUID = null;

        senderUUID = source.getPlayer().getUuid();

        if(senderUUID != null) {
            if(party.getPlayerUUIDList().contains(senderUUID)) {
                source.sendError(Text.literal("You are already in party \"" + partyName + "\""));
                return 0;
            }
        }

        // Prevent banned player from joining
        if(party.getBannedPlayersList() != null) {
            if(party.getBannedPlayerUUIDList().contains(senderUUID)) {
                source.sendError(Text.literal("You are banned from party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;

        player = source.getPlayer();

        // Get old players in party
        Set<UUID> oldPlayersInParty = party.getPlayerUUIDList();

        party.addPlayer(player);

        // Alert all players in party that someone joined
        for(ServerPlayerEntity p : allOnlinePlayers) {
            if(oldPlayersInParty.contains(p.getUuid())) {
                p.sendMessageToClient(Text.literal(player.getEntityName() + " joined the party"), false);
            }
        }


        source.sendFeedback(() -> Text.literal("Joined party \"" + partyName + "\""), true);

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
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist"));
            return 0;
        }

        UUID currentPlayerUUID = null;
        currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        source.sendFeedback(() -> Text.literal("Left party \"" + partyName + "\""), true);
        PartyChatMod.partyChatFile.removePlayerFromParty(partyName, player);


        // Create message with custom color, using FORMATTING.Red showing the player left the party
        MutableText partyAlertMessage = Text.literal("[" + partyName + "] ").styled(style -> style
                .withColor(Formatting.YELLOW)
                .withItalic(true)
        );

        partyAlertMessage.append(Text.literal(
                player.getEntityName() + " left the party"
        ).styled(style -> style
                .withColor(Formatting.RED)));

        // Alert all players in party that someone left
        for(ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
            if(party.getPlayerUUIDList().contains(p.getUuid())) {
                p.sendMessageToClient(partyAlertMessage, false);
            }
        }

        return 1;
    }

    private static int invite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist"));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        Collection<ServerPlayerEntity> playerEntities = EntityArgumentType.getPlayers(context, "Players");


        MutableText partyText = Text.literal(partyName).styled(style -> style
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party join " + partyName))
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
            source.sendFeedback(() -> Text.literal("Invited player to party \"" + partyName + "\""), true);
            return 1;
        } else {
            source.sendError(Text.literal("Player already in party \"" + partyName + "\""));
        }
        return 0;
    }

    // Kick user function. ONLY LEADER OF PARTY CAN KICK USERS
    private static int kick(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist").styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        if(party.getLeaderUUID().equals(player.getUuid())) {

            Collection<ServerPlayerEntity> playerSelected = EntityArgumentType.getPlayers(context, "Players"); // Only for one player [in the argument] or @a, if @a, then all players will be added to playerSelected

            int totalPlayerKicked = 0;

            // Kick the player(s) from the party
            for (ServerPlayerEntity playerEntity : playerSelected) {

                if(party.getPlayerUUIDList().contains(playerEntity.getUuid())) {
                    if(!party.getLeaderUUID().equals(playerEntity.getUuid())) {
                        PartyChatMod.partyChatFile.removePlayerFromParty(partyName, playerEntity);
                        totalPlayerKicked++;
                        playerEntity.sendMessageToClient(Text.literal("You have been kicked from party \"" + partyName + "\""), false);
                        playerEntity.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);

                        // Create message with custom color, using FORMATTING.Red showing the player left the party
                        MutableText partyAlertMessage = Text.literal("[" + partyName + "] ").styled(style -> style
                                .withColor(Formatting.YELLOW)
                                .withItalic(true)
                        );

                        if(playerSelected.size() <= 10) {
                            partyAlertMessage.append(Text.literal(
                                    playerEntity.getEntityName() + " left the party"
                            ).styled(style -> style
                                    .withColor(Formatting.RED)));
                        }
                        else {
                            partyAlertMessage.append(Text.literal(
                                    "(" + totalPlayerKicked + ")" + " left the party"
                            ).styled(style -> style
                                    .withColor(Formatting.RED)));
                        }

                        // Alert other players in the party
                        for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
                            if (party.getPlayerUUIDList().contains(p.getUuid())) {
                                p.sendMessageToClient(partyAlertMessage, false);
                            }
                        }
                    }
                }


            }


            source.sendFeedback(() -> Text.literal("Kicked player from party \"" + partyName + "\""), true);
            return 1;
        } else {
            source.sendError(Text.literal("You are not the leader of party \"" + partyName + "\""));
        }
        return 0;
    }

    // Ban user function. ONLY LEADER OF PARTY CAN BAN USERS
    private static int ban(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PartyChatMod.LOGGER.info("IN ban");
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for (Party p : PartyChatMod.partyChatFile.parties) {
            if (p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if (party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist").styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if (currentPlayerUUID != null) {
            if (!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        if (party.getLeaderUUID().equals(player.getUuid())) {

            int totalPlayerBanned = 0;

            // Gets only online players
            Collection<ServerPlayerEntity> playerSelected = EntityArgumentType.getPlayers(context, "Players"); // Only for one player [in the argument] or @a, if @a, then all players will be added to playerSelected


            if(playerSelected.size() == 1) {
                if(party.getLeaderUUID().equals(playerSelected.iterator().next().getUuid())) {
                    source.sendError(Text.literal("You cannot ban yourself"));
                    return 0;
                }
            }

            //Ban the player(s) from the party
            for (ServerPlayerEntity playerEntity : playerSelected) {

                if (party.getPlayerUUIDList().contains(playerEntity.getUuid())) {
                    if (!party.getLeaderUUID().equals(playerEntity.getUuid())) {
                        PartyChatMod.partyChatFile.removePlayerFromParty(partyName, playerEntity);
                        party.addBannedPlayer(playerEntity);
                        totalPlayerBanned++;
                        playerEntity.sendMessageToClient(Text.literal("You have been banned from party \"" + partyName + "\""), false);
                        playerEntity.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);

                        // Create message with custom color, using FORMATTING.Red showing the player left the party
                        MutableText partyAlertMessage = Text.literal("[" + partyName + "] ").styled(style -> style
                                .withColor(Formatting.YELLOW)
                                .withItalic(true)
                        );

                        if (playerSelected.size() <= 10) {
                            partyAlertMessage.append(Text.literal(
                                    playerEntity.getEntityName() + " left the party"
                            ).styled(style -> style
                                    .withColor(Formatting.RED)));
                        } else {
                            partyAlertMessage.append(Text.literal(
                                    "(" + totalPlayerBanned + ")" + " left the party"
                            ).styled(style -> style
                                    .withColor(Formatting.RED)));
                        }

                        // Alert other players in the party
                        for (ServerPlayerEntity p : source.getServer().getPlayerManager().getPlayerList()) {
                            if (party.getPlayerUUIDList().contains(p.getUuid())) {
                                p.sendMessageToClient(partyAlertMessage, false);
                            }
                        }
                    }
                }
            }

            source.sendFeedback(() -> Text.literal("Banned player from party \"" + partyName + "\""), true);
            return 1;
        } else {
            source.sendError(Text.literal("You are not the leader of party \"" + partyName + "\""));
        }
        return 0;
    }

    // Ban offline players function. ONLY LEADER OF PARTY CAN BAN USERS
    private static int banOffline(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PartyChatMod.LOGGER.info("IN banOffline");
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for (Party p : PartyChatMod.partyChatFile.parties) {
            if (p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if (party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist").styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if (currentPlayerUUID != null) {
            if (!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        if (party.getLeaderUUID().equals(player.getUuid())) {

            int totalPlayerBanned = 0;

            // Gets only online/offline players
            String playerSelected = StringArgumentType.getString(context, "PlayerString");

            // Get the player's NameAndUUID
            NameAndUUID banPlayer = PartyChatMod.partyChatFile.findPlayerEntityViaNameFile(partyName, playerSelected);
            if(banPlayer == null) {
                source.sendError(Text.literal("Player \"" + playerSelected + "\" is not in party \"" + partyName + "\""));
                return 0;
            }

            if(banPlayer.getPlayerUUID().equals(party.getLeaderUUID())) {
                source.sendError(Text.literal("You cannot ban yourself"));
                return 0;
            }

            //Ban the player(s) from the party
            if (party.getPlayerUUIDList().contains(banPlayer.getPlayerUUID())) {
                PartyChatMod.partyChatFile.removePlayerFromParty(partyName, banPlayer.getPlayerUUID());
                party.addBannedPlayer(banPlayer);
                totalPlayerBanned++;
                player.sendMessageToClient(Text.literal("You have been banned from party \"" + partyName + "\""), false);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);

                // Create message with custom color, using FORMATTING.Red showing the player left the party
                MutableText partyAlertMessage = Text.literal("[" + partyName + "] ").styled(style -> style
                        .withColor(Formatting.YELLOW)
                        .withItalic(true)
                );


                    partyAlertMessage.append(Text.literal(
                            banPlayer.getPlayerName()+ " left the party"
                    ).styled(style -> style
                            .withColor(Formatting.RED)));



        }

            source.sendFeedback(() -> Text.literal("Banned player from party \"" + partyName + "\""), true);
            return 1;
        } else {
            source.sendError(Text.literal("You are not the leader of party \"" + partyName + "\""));
        }
        return 0;
    }

    // Unban user function. ONLY LEADER OF PARTY CAN UNBAN USERS
    private static int unban(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String partyName = StringArgumentType.getString(context, "Party");
        Party party = null;
        ServerCommandSource source = context.getSource();

        for (Party p : PartyChatMod.partyChatFile.parties) {
            if (p.getPartyName().equals(partyName)) {
                party = p;
                break;
            }
        }

        if (party == null) {
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist").styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if (currentPlayerUUID != null) {
            if (!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }

        ServerPlayerEntity player = null;
        player = source.getPlayer();

        if (party.getLeaderUUID().equals(player.getUuid())) {

            Collection<ServerPlayerEntity> playerSelected = EntityArgumentType.getPlayers(context, "Players"); // Only for one player [in the argument] or @a, if @a, then all players will be added to playerSelected

            int totalPlayerUnbanned = 0;

            List<String> playerNameUnbannedList = new ArrayList<>();

            // Unban the player(s) from the party
            for (ServerPlayerEntity playerEntity : playerSelected) {

                if (party.getBannedPlayerUUIDList().contains(playerEntity.getUuid())) {
                    PartyChatMod.partyChatFile.removePlayerFromBannedList(partyName, playerEntity);
                    playerNameUnbannedList.add(playerEntity.getEntityName());
                    totalPlayerUnbanned++;
                    playerEntity.sendMessageToClient(Text.literal("You have been unbanned from party \"" + partyName + "\""), false);
                    playerEntity.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }

            // Send feedback to leader with the player name that is unbanned
            if(totalPlayerUnbanned <= 10) {
                for(String playerName : playerNameUnbannedList) {
                    source.sendFeedback(() -> Text.literal("Unbanned from party \"" + partyName + "\": " + playerName), true);
                }
            } else {
                int finalTotalPlayerUnbanned = totalPlayerUnbanned;
                source.sendFeedback(() -> Text.literal("Unbanned (" + finalTotalPlayerUnbanned + ") from party \"" + partyName + "\""), true);
            }
            return 1;
        } else {
            source.sendError(Text.literal("You are not the leader of party \"" + partyName + "\""));
        }
        return 0;
    }

    private static int players(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
            source.sendError(Text.literal("Party \"" + partyName + "\" does not exist"));
            return 0;
        }

        UUID currentPlayerUUID = source.getPlayer().getUuid();

        if(currentPlayerUUID != null) {
            if(!party.getPlayerUUIDList().contains(currentPlayerUUID)) {
                source.sendError(Text.literal("You are not in party \"" + partyName + "\""));
                return 0;
            }
        }
        String playerListAsString = "";
        int i = 0;

        for(String playerName : party.getPlayerNamesList()) {
            if(i > 0) {
                playerListAsString += " , ";
            }
            playerListAsString += playerName;
            i += 1;
        }

        // Show total players in party and player names
        int totalPlayers = party.getPlayerList().size();
        MutableText message = Text.literal("Player" + (totalPlayers > 1 ? "s" : "") + " (" + totalPlayers + ") in party \"" + partyName + "\":\n").styled(style -> style.withColor(Formatting.GRAY));

        message.append(Text.literal(playerListAsString).styled(style -> style.withColor(Formatting.GREEN)));

        source.sendFeedback(() -> message, false);

        return i;
    }


}
