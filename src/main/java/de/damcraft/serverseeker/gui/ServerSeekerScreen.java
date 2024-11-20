package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.DiscordAvatar;
import de.damcraft.serverseeker.ServerSeekerSystem;
import de.damcraft.serverseeker.utils.MultiplayerScreenUtil;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

public class ServerSeekerScreen extends AbstractAuthRequiredScreen {
    private final MultiplayerScreen multiplayerScreen;

    public ServerSeekerScreen(MultiplayerScreen multiplayerScreen) {
        super(GuiThemes.get(), "ServerSeeker");
        this.multiplayerScreen = multiplayerScreen;
    }

    @Override
    public void initWidgets() {
        if (this.initWarnings()) {
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

        add(theme.horizontalSeparator()).expandX();

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
