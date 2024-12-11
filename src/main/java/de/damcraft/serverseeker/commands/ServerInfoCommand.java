package de.damcraft.serverseeker.commands;

import com.google.common.net.HostAndPort;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class ServerInfoCommand extends Command {
    private static final SimpleCommandExceptionType SINGLEPLAYER_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Cannot run command in singleplayer."));

    public ServerInfoCommand() {
        super("serverInfo", "");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.getCurrentServerEntry() == null) {
                throw SINGLEPLAYER_EXCEPTION.create();
            }

            HostAndPort hap = HostAndPort.fromString(mc.getCurrentServerEntry().address);
            ServerInfoRequest request = new ServerInfoRequest(ServerSeeker.API_KEY, hap.getHost(), hap.getPort());

            MeteorExecutor.execute(() -> {
                ServerInfoResponse response = Http.post("https://api.serverseeker.net/server_info")
                    .exceptionHandler(e -> LOG.error("Could not post to 'server_info': ", e))
                    .bodyJson(request)
                    .sendJson(ServerInfoResponse.class);

                mc.execute(() -> {
                    if (response == null) {
                        error("Network error");
                        return;
                    }

                    if (response.isError()) {
                        error(response.error());
                        return;
                    }

                    Boolean cracked = response.cracked();
                    String description = response.description();
                    if (description.length() > 100) description = description.substring(0, 100) + "...";
                    description = description.replace("\n", "\\n");
                    description = description.replace("§r", "");
                    int onlinePlayers = response.onlinePlayers();
                    int maxPlayers = response.maxPlayers();
                    int protocol = response.protocol();
                    int lastSeen = response.lastSeen();
                    String lastSeenDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .format(Instant.ofEpochSecond(lastSeen).atZone(ZoneId.systemDefault()).toLocalDateTime());
                    String version = response.version();
                    List<ServerInfoResponse.Player> players = response.players();

                    info("-- Server Info --");
                    info("Cracked: (highlight)" + (cracked == null ? "Unknown" : cracked.toString()));
                    info("Description: (highlight)" + description);
                    info("Online Players (last scan): (highlight)" + onlinePlayers);
                    info("Max Players: (highlight)" + maxPlayers);
                    info("Last Scanned: (highlight)" + lastSeenDate);
                    info("Version: (highlight)" + version + " (default)(" + protocol + ")");
                    if (players.isEmpty()) {
                        warning("No player history.");
                    } else {
                        info("-- Player History --");
                        for (ServerInfoResponse.Player player : players) {
                            long playerLastSeen = player.lastSeen();
                            String lastSeenFormatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                .format(Instant.ofEpochSecond(playerLastSeen).atZone(ZoneId.systemDefault()).toLocalDateTime());
                            info("- (highlight)" + player.name() + " " + lastSeenFormatted);
                        }
                    }
                });
            });

            return SINGLE_SUCCESS;
        });
    }
}
