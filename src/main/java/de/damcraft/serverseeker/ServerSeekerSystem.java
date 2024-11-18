package de.damcraft.serverseeker;

import com.google.gson.JsonObject;
import de.damcraft.serverseeker.ssapi.responses.UserInfoResponse;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.NbtCompound;

import java.util.concurrent.CompletableFuture;

import static de.damcraft.serverseeker.ServerSeeker.LOG;
import static de.damcraft.serverseeker.ServerSeeker.gson;

public class ServerSeekerSystem extends System<ServerSeekerSystem> {
    public ServerSeekerSystem() {
        super("serverseeker");
    }

    public volatile String apiKey = "";
    public volatile UserInfoResponse userInfo;
    public volatile boolean networkIssue = false;
    private volatile long lastUpdate = -1;
    private volatile CompletableFuture<UserInfoResponse> future;

    public static ServerSeekerSystem get() {
        return Systems.get(ServerSeekerSystem.class);
    }

    public void invalidate() {
        lastUpdate = -1;
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public CompletableFuture<UserInfoResponse> refresh() {
        // Refresh rate-limited, returns previous result
        if (lastUpdate != -1 && java.lang.System.currentTimeMillis() - lastUpdate < 10 * 1000) {
            return CompletableFuture.completedFuture(this.userInfo);
        }

        // Refresh underway, return cached future
        if (future != null) {
            return future;
        }

        // Execute refresh and cache future
        return future = CompletableFuture.supplyAsync(() -> {
            JsonObject params = new JsonObject();

            params.addProperty("api_key", apiKey);

            String jsonResp = SmallHttp.post("https://api.serverseeker.net/user_info", params.toString());
            if (jsonResp == null) {
                LOG.error("Network error");
                networkIssue = true;
                throw new RuntimeException("Network error");
            }

            networkIssue = false;
            lastUpdate = java.lang.System.currentTimeMillis();
            future = null;

            UserInfoResponse userInfo = gson.fromJson(jsonResp, UserInfoResponse.class);
            if (userInfo.isError()) {
                LOG.error("Error: " + userInfo.error);
                throw new RuntimeException(userInfo.error);
            }

            return this.userInfo = userInfo;
        }, MeteorExecutor.executor);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("apiKey", apiKey);
        tag.putString("userId", userInfo.discord_id);
        tag.putString("username", userInfo.discord_username);
        tag.putString("avatarUrl", userInfo.discord_avatar_url);

        return tag;
    }

    @Override
    public ServerSeekerSystem fromTag(NbtCompound tag) {
        apiKey = tag.getString("apiKey");

        String userId = tag.getString("userId");
        String username = tag.getString("username");
        String avatarUrl = tag.getString("avatarUrl");

        UserInfoResponse info = this.userInfo = new UserInfoResponse();
        info.discord_id = userId;
        info.discord_username = username;
        info.discord_avatar_url = avatarUrl;

        return this;
    }
}
