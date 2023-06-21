package net.alyph.partychatmod.data;



import com.google.common.collect.Sets;
import net.minecraft.entity.player.PlayerEntity;


import java.util.Set;
import java.util.UUID;

public class Party {
    final String partyName;
    private final UUID leaderUUID;
    private Set<NameAndUUID> bannedPlayersList = Sets.newHashSet();
    private Set<NameAndUUID> playerList = Sets.newHashSet();
    public Party(String partyName, UUID leaderUUID) {
        this.partyName = partyName;
        this.leaderUUID = leaderUUID;
    }
    public Party(String partyName, UUID leaderUUID, Set<NameAndUUID> playerList) {
        this.partyName = partyName;
        this.leaderUUID = leaderUUID;
        for(NameAndUUID player : playerList) {
            this.playerList.add(player);
        }
    }

    public Party(String partyName, Set<NameAndUUID> players, UUID leaderUUID) {
        this.partyName = partyName;
        this.leaderUUID = leaderUUID;
        for (NameAndUUID player : players) {
            this.playerList.add(player);
        }
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }
    public String getPartyName() {
        return partyName;
    }

    public Set<NameAndUUID> getPlayerList() {
        return this.playerList;
    }

    public Set<NameAndUUID> getBannedPlayersList() {
        return this.bannedPlayersList;
    }
    public Set<UUID> getBannedPlayerUUIDList() {
        Set<UUID> bannedPlayerUUIDList = Sets.newHashSet();
        for(NameAndUUID player : bannedPlayersList) {
            bannedPlayerUUIDList.add(player.getPlayerUUID());
        }
        return bannedPlayerUUIDList;
    }
    public Set<UUID> getPlayerUUIDList() {
        Set<UUID> playerUUIDList = Sets.newHashSet();
        for(NameAndUUID player : playerList) {
            playerUUIDList.add(player.getPlayerUUID());
        }
        return playerUUIDList;
    }
    public Set<String> getPlayerNamesList() {
        Set<String> playerNamesList = Sets.newHashSet();
        for(NameAndUUID player : playerList) {
            playerNamesList.add(player.getPlayerName());
        }
        return playerNamesList;
    }

    public void addPlayer(PlayerEntity player) {
        this.playerList.add(NameAndUUID.of(player));
    }
    public void addPlayer(NameAndUUID player) {
        this.playerList.add(player);
    }

    public void addBannedPlayer(PlayerEntity player) {
        this.bannedPlayersList.add(NameAndUUID.of(player));
    }

    public void addBannedPlayer(NameAndUUID player) {
        this.bannedPlayersList.add(player);
    }

    public NameAndUUID findPlayerEntityViaName(String playerName) {
        for(NameAndUUID player : playerList) {
            if(player.getPlayerName().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    public void removeBannedPlayer(UUID playerUUID) {
        for(NameAndUUID playerInList : bannedPlayersList) {
            if(playerInList.getPlayerUUID().equals(playerUUID)) {
                this.bannedPlayersList.remove(playerInList);
                return;
            }
        }
    }

    public void removePlayer(UUID playerUUID) {
        for(NameAndUUID playerInList : playerList) {
            if(playerInList.getPlayerUUID().equals(playerUUID)) {
                this.playerList.remove(playerInList);
                return;
            }
        }
    }

    public void removePlayerFromName(String playerName) {
        for(NameAndUUID playerInList : playerList) {
            if(playerInList.getPlayerName().equals(playerName)) {
                this.playerList.remove(playerInList);
                return;
            }
        }
    }

    public void setPlayerList(Set<NameAndUUID> playerList) {
        this.playerList = playerList;
    }

    public boolean isMember(UUID memberID) {
        for(NameAndUUID player : playerList) {
            if(player.getPlayerUUID().equals(memberID)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLeader(UUID leaderUUID) {
        return this.leaderUUID.equals(leaderUUID);
    }

    public String getPlayerListToString() {
        String playerListString = "";
        for(NameAndUUID player : playerList) {
            playerListString += player.getPlayerName() + ", ";
        }
        return playerListString;
    }


}
