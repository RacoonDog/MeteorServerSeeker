package de.damcraft.serverseeker;

import de.damcraft.serverseeker.ssapi.requests.UserInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.UserInfoResponse;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class ServerSeekerSystem extends System<ServerSeekerSystem> {
    public ServerSeekerSystem() {
        super("serverseeker");
    }

    public volatile String apiKey = "";
    public volatile UserInfoResponse userInfo;
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
            UserInfoRequest request = new UserInfoRequest(apiKey);

            UserInfoResponse response = Http.post("https://api.serverseeker.net/user_info")
                .exceptionHandler(e -> LOG.error("Could not post to 'user_info': ", e))
                .bodyJson(request)
                .sendJson(UserInfoResponse.class);

            if (response == null) {
                throw new RuntimeException("Network error");
            }

            lastUpdate = java.lang.System.currentTimeMillis();
            future = null;

            if (response.isError()) {
                LOG.error("Error: " + response.error);
                throw new RuntimeException(response.error);
            }

            return this.userInfo = response;
        }, MeteorExecutor.executor);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("apiKey", apiKey);
        tag.putString("username", userInfo.discord_username);
        tag.putString("avatarUrl", Optional.ofNullable(userInfo.discord_avatar_url).orElse(""));

        return tag;
    }

    @Override
    public ServerSeekerSystem fromTag(NbtCompound tag) {
        apiKey = tag.getString("apiKey");

        UserInfoResponse info = this.userInfo = new UserInfoResponse();
        info.discord_username = tag.getString("username");
        info.discord_avatar_url = tag.getString("avatarUrl");

        return this;
    }
}
