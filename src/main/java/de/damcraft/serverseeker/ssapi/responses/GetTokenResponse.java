package de.damcraft.serverseeker.ssapi.responses;

import com.google.gson.annotations.SerializedName;

public record GetTokenResponse(String error, @SerializedName("api_key") String apiKey) {
    public boolean isError() {
        return error() != null;
    }
}
