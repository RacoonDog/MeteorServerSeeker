package de.damcraft.serverseeker.ssapi.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record ServerInfoResponse(String error, Boolean cracked, String description,
                                 @SerializedName("last_seen") Integer lastSeen,
                                 @SerializedName("max_players") Integer maxPlayers,
                                 @SerializedName("online_players") Integer onlinePlayers, Integer protocol,
                                 String version, List<Player> players) {
    public boolean isError() {
        return error != null;
    }

    public record Player(String name, String uuid, @SerializedName("last_seen") Integer lastSeen) {}
}
