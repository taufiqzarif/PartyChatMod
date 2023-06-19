package net.alyph.partychatmod.data;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.alyph.partychatmod.PartyChatMod;
import net.minecraft.entity.player.PlayerEntity;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

public class PartyChatFile {
    final static Path PARTY_CHAT_FILE_PATH = Paths.get("", "config", "party.json");
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String FILE_VERSION = PartyChatMod.FILE_VERSION.toString();
    public Config config = new Config();

    public static class Config {
        public boolean soundOnInvite = true;
    }

    public Set<Party> parties = Sets.newHashSet();
    public Set<String> getPartiesForPlayer(NameAndUUID nameAndUUID) {
        Set<String> set = Sets.newHashSet();

        for(Party party : this.parties) {
            if(party.getPlayerUUIDList().contains(nameAndUUID.getPlayerUUID())) {
                set.add(party.getPartyName());
            }
        }

        this.save();
        return set;
    }

    public Set<String> getPartiesForPlayer(PlayerEntity player) {
        return this.getPartiesForPlayer(NameAndUUID.of(player));
    }

    public void removePlayerFromParty(String partyName, UUID playerUUID) {
        for(Party party : this.parties) {
            if(party.getPartyName().equals(partyName)) {
                party.removePlayer(playerUUID);
            }
        }
    }

    public void removePlayerFromParty(String partyName, PlayerEntity player) {
        this.removePlayerFromParty(partyName, player.getUuid());
    }

    public void addParty(Party party) {
        this.parties.add(party);
        this.save();
        PartyChatMod.LOGGER.info("Added party " + party.getPartyName());
    }

    public void setParty(Party party) {
        boolean b1 = false;

        for(Party p : this.parties) {
            if(p.getPartyName().equals(party.getPartyName())) {
                b1 = true;
                break;
            }
        }

        if(!b1) {
            this.addParty(party);
        }

        this.save();
    }

    public void save() {
        this.removeEmptyParties();

        try {
            Paths.get("", "config").toFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(PARTY_CHAT_FILE_PATH.toFile())
            );
            writer.write(gson.toJson(this));
            writer.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        PartyChatMod.partyChatFile = this;
        PartyChatMod.LOGGER.info("Saved all parties to file");
    }

    private void removeEmptyParties() {
        int totalPartiesRemoved = 0;

        for (Party party : this.parties) {
            if (party.getPlayerList().isEmpty()) {
                this.parties.remove(party);
                totalPartiesRemoved++;
            }
        }
        PartyChatMod.LOGGER.info("Removed " + totalPartiesRemoved + " empty parties");
    }

    public static PartyChatFile init() {
        PartyChatMod.LOGGER.info("Loading parties from file");
        PartyChatFile partyChatFile = null;

        try {
            if(Files.exists(PARTY_CHAT_FILE_PATH)) {
                PartyChatMod.LOGGER.info("Found party file");
                partyChatFile = gson.fromJson(
                        new FileReader(PARTY_CHAT_FILE_PATH.toFile()),
                        PartyChatFile.class
                );

                if(!partyChatFile.FILE_VERSION.equals(PartyChatMod.FILE_VERSION.toString())) {
                    PartyChatMod.LOGGER.info("Party file is outdated, updating");
                    partyChatFile.FILE_VERSION = PartyChatMod.FILE_VERSION.toString();

                    BufferedWriter writer = new BufferedWriter(
                            new FileWriter(PARTY_CHAT_FILE_PATH.toFile())
                    );
                    writer.write(gson.toJson(partyChatFile));
                    writer.close();
                }
            } else {
                PartyChatMod.LOGGER.info("No party file found, creating new one");
                partyChatFile = new PartyChatFile();
                Paths.get("", "config").toFile().mkdirs();

                BufferedWriter writer = new BufferedWriter(
                        new FileWriter(PARTY_CHAT_FILE_PATH.toFile())
                );
                writer.write(gson.toJson(partyChatFile));
                writer.close();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return partyChatFile;
    }
}
