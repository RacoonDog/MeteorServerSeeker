package de.damcraft.serverseeker.ssapi.requests;

import com.google.gson.annotations.SerializedName;

public record UserInfoRequest(@SerializedName("api_key") String apiKey) {}
