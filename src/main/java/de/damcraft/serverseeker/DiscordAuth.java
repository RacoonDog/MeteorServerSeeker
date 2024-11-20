package de.damcraft.serverseeker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.damcraft.serverseeker.ssapi.requests.GetTokenRequest;
import de.damcraft.serverseeker.ssapi.responses.GetTokenResponse;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class DiscordAuth {
    private static final int port = 7637;

    // Store as a string because it's too big and I don't want to import unnecessary libraries if I join it to a String anyway
    private static final String clientId = "1087083964432404590";

    public static final String url =
        "https://discord.com/api/oauth2/authorize" +
            "?client_id=" + clientId +
            "&redirect_uri=http%3A%2F%2F127.0.0.1%3A" + port + "%2F" +
            "&response_type=code" +
            "&scope=identify";

    private static HttpServer server;

    private static BiConsumer<String, String> callback;

    public static void auth(BiConsumer<String, String> callback) {
        DiscordAuth.callback = callback;
        Util.getOperatingSystem().open(url);
        startServer();
    }

    private static void startServer() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/", new AuthHandler());
            server.start();
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    public static void stopServer() {
        if (server == null) return;

        server.stop(0);
        server = null;
    }

    private static void accept(String result, String exception) {
        MinecraftClient.getInstance().execute(() -> {
            callback.accept(result, exception);
            callback = null;
        });
    }

    private static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange req) throws IOException {
            if (req.getRequestMethod().equals("GET")) {
                // Login
                List<NameValuePair> query = URLEncodedUtils.parse(req.getRequestURI(), StandardCharsets.UTF_8);

                boolean ok = false;

                for (NameValuePair pair : query) {
                    if (pair.getName().equals("code")) {
                        handleCode(pair.getValue());

                        ok = true;
                        break;
                    }
                }

                if (!ok) {
                    writeText(req, "Cannot authenticate.");
                } else writeText(req, "You may now close this page.");
                stopServer();
            } else if (req.getRequestMethod().equals("OPTIONS")) {
                req.getResponseHeaders().add("Allow", "GET, OPTIONS");
                req.sendResponseHeaders(204, -1);
            } else {
                req.sendResponseHeaders(405, -1);
                LOG.warn("Invalid request method: {}", req.getRequestMethod());
            }
        }

        private void writeText(HttpExchange req, String text) throws IOException {
            OutputStream out = req.getResponseBody();

            req.sendResponseHeaders(200, text.length());

            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }

        public void handleCode(String code) {
            // Get the ServerSeeker auth token

            GetTokenRequest request = new GetTokenRequest(code);

            GetTokenResponse response = Http.post("https://api.serverseeker.net/get_token")
                .exceptionHandler(e -> LOG.info("Network error: " + e.getMessage()))
                .bodyJson(request)
                .sendJson(GetTokenResponse.class);

            // {"api_key": "..."} or {"error": "..."}

            if (response == null) {
                ServerSeekerSystem.get().networkIssue = true;
                accept(null, "Network error");
                return;
            }

            if (response.isError()) {
                LOG.error("Error: " + response.error());
                accept(null, response.error());
                return;
            } else if (response.apiKey() == null) {
                LOG.error("Error: No api_key in response.");
                accept(null, "No api_key in response.");
                return;
            }

            ServerSeekerSystem system = ServerSeekerSystem.get();
            system.apiKey = response.apiKey();

            system.refresh().thenAccept(userInfo -> {
                accept(ServerSeekerSystem.get().apiKey, null);
            }).exceptionally(ex -> {
                accept(null, ex.getMessage());
                return null;
            });
        }
    }
}
