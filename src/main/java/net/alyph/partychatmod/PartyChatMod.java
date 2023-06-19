package net.alyph.partychatmod;

import net.alyph.partychatmod.data.PartyChatFile;
import net.alyph.partychatmod.util.FOModVersion;
import net.alyph.partychatmod.util.ModRegistries;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartyChatMod implements ModInitializer {
	public static final String MOD_ID = "partychatmod";
	public static final FOModVersion VERSION = FOModVersion.fromString("1.0.0");
	public static final FOModVersion FILE_VERSION = FOModVersion.fromString("1.0.0");
    public static final Logger LOGGER = LoggerFactory.getLogger("partychatmod");

	public static PartyChatFile partyChatFile = PartyChatFile.init();

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