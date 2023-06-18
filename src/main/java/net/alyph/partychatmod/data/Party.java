package net.alyph.partychatmod.data;



import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    private final UUID partyID;
    private String partyName;
    private final UUID leaderID;
    private final List<UUID> memberIDs;

    public Party(UUID partyID, String partyName, UUID leaderID) {
        this.partyID = partyID;
        this.partyName = partyName;
        this.leaderID = leaderID;
        this.memberIDs = new ArrayList<>();
    }

    // Add a method to your Party class to convert it to and from a JSON string:
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Party fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Party.class);
    }



    public UUID getPartyID() {
        return partyID;
    }

    public UUID getLeaderID() {
        return leaderID;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public void addMember(UUID memberID) {
        memberIDs.add(memberID);
    }

    public void removeMember(UUID memberID) {
        memberIDs.remove(memberID);
    }

    public List<UUID> getMemberIDs() {
        return memberIDs;
    }

    public boolean isMember(UUID memberID) {
        return memberIDs.contains(memberID);
    }

    public boolean isLeader(UUID memberID) {
        return leaderID.equals(memberID);
    }

    public boolean isPartyEmpty() {
        return memberIDs.isEmpty();
    }

    public boolean isPartyFull() {
        return memberIDs.size() >= 4;
    }



}
