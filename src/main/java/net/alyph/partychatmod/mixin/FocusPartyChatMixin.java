package net.alyph.partychatmod.mixin;

import net.alyph.partychatmod.PartyChatMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public class FocusPartyChatMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    protected void injectSendChatMessage(String message, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.player == null) return;
        UUID playerUUID = client.player.getUuid();
        if(PartyChatMod.playerFocusPartyChat.containsKey(playerUUID)) {
            String focusedPartyName = PartyChatMod.playerFocusPartyChat.get(playerUUID);
            String command = "party message " + focusedPartyName + " " + message;
            client.player.networkHandler.sendChatCommand(command);
            ci.cancel(); // cancel the original method to prevent the original chat message from being sent
        }
    }
}
