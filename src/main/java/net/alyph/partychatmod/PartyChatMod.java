package net.alyph.partychatmod;

import net.alyph.partychatmod.util.ModRegistries;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartyChatMod implements ModInitializer {
	public static final String MOD_ID = "partychatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("partychatmod");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModRegistries.registerModStuffs();
	}
}