package net.alyph.partychatmod.data;

import net.minecraft.entity.Entity;

import java.util.UUID;

public class NameAndUUID {
    private String playerName;
    private UUID playerUUID;
    private String focusPartyChat;

    public NameAndUUID(String playerName, UUID playerUUID) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.focusPartyChat = null;
    }

    public NameAndUUID(Entity entity) {
        this(entity.getEntityName(), entity.getUuid());
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getFocusPartyChat() {
        return focusPartyChat;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void setFocusPartyChat(String partyName) {
        this.focusPartyChat = partyName;
    }

    public static NameAndUUID of(Entity entity) {
        return new NameAndUUID(entity);
    }

}
