package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.DiscordAvatar;
import de.damcraft.serverseeker.ServerSeekerSystem;
import de.damcraft.serverseeker.utils.MultiplayerScreenUtil;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

public class ServerSeekerScreen extends WindowScreen {
    private final MultiplayerScreen multiplayerScreen;

    public ServerSeekerScreen(MultiplayerScreen multiplayerScreen) {
        super(GuiThemes.get(), "ServerSeeker");
        this.multiplayerScreen = multiplayerScreen;
    }
    private WButton refreshButton;
    private volatile boolean waitingForAuth = false;
    private volatile boolean waitingForRefreh = false;

    @Override
    public void initWidgets() {
        ServerSeekerSystem system = ServerSeekerSystem.get();
        String authToken = system.apiKey;

        if (authToken.isEmpty()) {
            WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
            widgetList.add(theme.label("Please authenticate with Discord. "));
            waitingForAuth = true;
            WButton loginButton = widgetList.add(theme.button("Login")).widget();
            loginButton.action = () -> {
                if (this.client == null) return;
                this.client.setScreen(new LoginWithDiscordScreen(this));
            };
            return;
        }
        if (system.networkIssue) {
            WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
            widgetList.add(theme.label("Could not connect to the ServerSeeker api servers."));
            refreshButton = widgetList.add(theme.button("Refresh")).widget();
            refreshButton.action = () -> {
                waitingForRefreh = true;
                ServerSeekerSystem.get().refresh().thenRun(() -> {
                    waitingForRefreh = false;
                    this.reload();
                });
            };
            return;
        }

        WHorizontalList accountList = add(theme.horizontalList()).expandX().widget();
        // Add an image of the user's avatar
        accountList.add(theme.texture(32, 32, 0, DiscordAvatar.get()));
        accountList.add(theme.label(ServerSeekerSystem.get().userInfo.discord_username)).expandX();
        WButton logoutButton = accountList.add(theme.button("Logout")).widget();
        logoutButton.action = () -> {
            ServerSeekerSystem.get().apiKey = "";
            ServerSeekerSystem.get().invalidate();
            ServerSeekerSystem.get().save();
            reload();
        };
        WTable userInfoList = add(theme.table()).widget();
        userInfoList.add(theme.label("Loading..."));

        ServerSeekerSystem.get().refresh().thenRun(() -> {
            userInfoList.clear();

            userInfoList.add(theme.label("Requests made:"));
            userInfoList.row();

            int whereisRequestsMade = ServerSeekerSystem.get().userInfo.requests_made_whereis;
            int whereisRequestsTotal = ServerSeekerSystem.get().userInfo.requests_per_day_whereis;
            userInfoList.add(theme.label("Whereis: "));
            userInfoList.add(theme.label(whereisRequestsMade + "/" + whereisRequestsTotal)).widget().color(whereisRequestsTotal == whereisRequestsMade ? Color.RED : Color.WHITE);
            userInfoList.row();

            int serversRequestsMade = ServerSeekerSystem.get().userInfo.requests_made_servers;
            int serversRequestsTotal = ServerSeekerSystem.get().userInfo.requests_per_day_servers;
            userInfoList.add(theme.label("Servers: "));
            userInfoList.add(theme.label(serversRequestsMade + "/" + serversRequestsTotal)).widget().color(serversRequestsTotal == serversRequestsMade ? Color.RED : Color.WHITE);
            userInfoList.row();

            int serverInfoRequestsMade = ServerSeekerSystem.get().userInfo.requests_made_server_info;
            int serverInfoRequestsTotal = ServerSeekerSystem.get().userInfo.requests_per_day_server_info;
            userInfoList.add(theme.label("Server Info: "));
            userInfoList.add(theme.label(serverInfoRequestsMade + "/" + serverInfoRequestsTotal)).widget().color(serverInfoRequestsTotal == serverInfoRequestsMade ? Color.RED : Color.WHITE);
        });


        WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
        WButton newServersButton = widgetList.add(this.theme.button("Find new servers")).expandX().widget();
        WButton findPlayersButton = widgetList.add(this.theme.button("Search players")).expandX().widget();
        WButton cleanUpServersButton = widgetList.add(this.theme.button("Clean up")).expandX().widget();
        newServersButton.action = () -> {
            if (this.client == null) return;
            this.client.setScreen(new FindNewServersScreen(this.multiplayerScreen));
        };
        findPlayersButton.action = () -> {
            if (this.client == null) return;
            this.client.setScreen(new FindPlayerScreen(this.multiplayerScreen));
        };
        cleanUpServersButton.action = () -> {
            if (this.client == null) return;
            clear();
            add(theme.label("Are you sure you want to clean up your server list?"));
            add(theme.label("This will remove all servers that start with \"ServerSeeker\""));
            WHorizontalList buttonList = add(theme.horizontalList()).expandX().widget();
            WButton backButton = buttonList.add(theme.button("Back")).expandX().widget();
            backButton.action = this::reload;
            WButton confirmButton = buttonList.add(theme.button("Confirm")).expandX().widget();
            confirmButton.action = this::cleanUpServers;
        };
    }

    @Override
    public void reload() {
        this.refreshButton = null;
        super.reload();
    }

    @Override
    public void tick() {
        if (waitingForRefreh) {
            refreshButton.set(switch (refreshButton.getText()) {
                default -> "ooo";
                case "ooo" -> "0oo";
                case "0oo" -> "o0o";
                case "o0o" -> "oo0";
            });
        }
        if (waitingForAuth) {
            String authToken = ServerSeekerSystem.get().apiKey;
            if (!authToken.isEmpty()) {
                this.reload();
                this.waitingForAuth = false;
            }
        }
    }

    public void cleanUpServers() {
        if (this.client == null) return;

        for (int i = 0; i < this.multiplayerScreen.getServerList().size(); i++) {
            if (this.multiplayerScreen.getServerList().get(i).name.startsWith("ServerSeeker")) {
                this.multiplayerScreen.getServerList().remove(this.multiplayerScreen.getServerList().get(i));
                i--;
            }
        }

        MultiplayerScreenUtil.saveList(multiplayerScreen);
        MultiplayerScreenUtil.reloadServerList(multiplayerScreen);

        client.setScreen(this.multiplayerScreen);
    }
}
