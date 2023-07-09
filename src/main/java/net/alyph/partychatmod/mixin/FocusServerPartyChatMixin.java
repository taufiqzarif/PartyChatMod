package net.alyph.partychatmod.mixin;

import com.mojang.brigadier.ParseResults;
import net.alyph.partychatmod.PartyChatMod;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public class FocusServerPartyChatMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    protected void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        //PartyChatMod.LOGGER.info("server onChatMessage");
        MinecraftServer server = player.getServer();
        UUID playerUUID = player.getUuid();
        if(PartyChatMod.playerFocusPartyChat.containsKey(playerUUID)) {
            //PartyChatMod.LOGGER.info("in server if onChatMessage");
            String message = packet.chatMessage();
            PartyChatMod.LOGGER.info("server message: " + message);
            String focusedPartyName = PartyChatMod.playerFocusPartyChat.get(playerUUID);
            handlePartyMessage(focusedPartyName, message);
            ci.cancel(); // cancel the original method to prevent the original chat message from being sent
        }
    }

    private void handlePartyMessage(String partyName, String partyMessage) {
        //PartyChatMod.LOGGER.info("server handlePartyMessage");
        // Create a command string
        String command = "party message " + partyName + " " + partyMessage;

        ParseResults<ServerCommandSource> parseResults = player.getServer().getCommandManager().getDispatcher().parse(command, player.getCommandSource());

        player.getServer().getCommandManager().execute(parseResults, command);

    }

}
