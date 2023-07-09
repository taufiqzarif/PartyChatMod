package net.alyph.partychatmod;

import net.alyph.partychatmod.data.PartyChatFile;
import net.alyph.partychatmod.util.FOModVersion;
import net.alyph.partychatmod.util.ModRegistries;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.fabricmc.fabric.mixin.client.message.ClientPlayNetworkHandlerMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PartyChatMod implements ModInitializer {
	public static final String MOD_ID = "partychatmod";
	public static final FOModVersion VERSION = FOModVersion.fromString("1.0.0");
	public static final FOModVersion FILE_VERSION = FOModVersion.fromString("1.0.0");
    public static final Logger LOGGER = LoggerFactory.getLogger("partychatmod");

	public static PartyChatFile partyChatFile = PartyChatFile.init();

	public static HashMap<UUID, String> playerFocusPartyChat = new HashMap<>();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("File version: " + FILE_VERSION.toStrng());
		ModRegistries.registerModStuffs();
	}

	public static PartyChatFile.Config getConfig() {
		return partyChatFile.config;
	}
}