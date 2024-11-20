package de.damcraft.serverseeker.ssapi.responses;

public class UserInfoResponse {
    public String error;
    public String discord_id;
    public String discord_username;
    public String discord_avatar_url;

    public boolean isError() {
        return error != null;
    }
}
