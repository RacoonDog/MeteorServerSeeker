package de.damcraft.serverseeker.utils;

import de.damcraft.serverseeker.ServerSeekerSystem;
import de.damcraft.serverseeker.hud.HistoricPlayersHud;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.damcraft.serverseeker.ServerSeeker.LOG;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HistoricPlayersUpdater {
    @EventHandler
    private static void onGameJoinEvent(GameJoinedEvent ignoredEvent) {
        // Run in a new thread
        new Thread(HistoricPlayersUpdater::update).start();
    }

    public static void update() {
        // If the Hud contains the HistoricPlayersHud, update the players
        List<HistoricPlayersHud> huds = new ArrayList<>();
        for (HudElement hudElement : Hud.get()) {
            if (hudElement instanceof HistoricPlayersHud && hudElement.isActive()) {
                huds.add((HistoricPlayersHud) hudElement);
            }
        }
        if (huds.isEmpty()) return;

        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null) return;

        String address = networkHandler.getConnection().getAddress().toString();
        // Split it at "/" and take the second part
        String[] addressParts = address.split("/");
        if (addressParts.length < 2) return;
        addressParts = addressParts[1].split(":");

        String ip = addressParts[0];
        int port = Integer.parseInt(addressParts[1]);

        ServerInfoRequest request = new ServerInfoRequest(ServerSeekerSystem.get().apiKey, ip, port);

        ServerInfoResponse response = Http.post("https://api.serverseeker.net/server_info")
            .exceptionHandler(e -> LOG.error("Could not post to 'server_info': ", e))
            .bodyJson(request)
            .sendJson(ServerInfoResponse.class);

        if (response == null) {
            return;
        }

        for (HistoricPlayersHud hud : huds) {
            hud.players = Objects.requireNonNullElse(response.players(), List.of());
            hud.isCracked = response.cracked() != null && response.cracked();
        }
    }
}
