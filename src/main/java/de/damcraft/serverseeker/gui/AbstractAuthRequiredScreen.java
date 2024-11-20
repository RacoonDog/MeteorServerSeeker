package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.ServerSeekerSystem;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.MinecraftClient;

/**
 * Abstract away common logic for screens that require an authenticated connection to the ServerSeeker API.
 *
 * @see ServerSeekerScreen
 * @see GetInfoScreen
 */
public abstract class AbstractAuthRequiredScreen extends WindowScreen {
    private WButton refreshButton;

    private boolean waitingForAuth = false;
    private boolean waitingForRefresh = false;

    public AbstractAuthRequiredScreen(GuiTheme theme, String title) {
        super(theme, title);
    }

    protected boolean initWarnings() {
        ServerSeekerSystem system = ServerSeekerSystem.get();
        String authToken = system.apiKey;

        if (authToken.isEmpty()) {
            WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
            widgetList.add(theme.label("Please authenticate with Discord. "));
            WButton loginButton = widgetList.add(theme.button("Login")).widget();
            loginButton.action = () -> {
                this.waitingForAuth = true;
                this.client.setScreen(new LoginWithDiscordScreen(this));
            };
            return true;
        }

        if (system.networkIssue) {
            WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
            widgetList.add(theme.label("Could not connect to the ServerSeeker api servers."));
            this.refreshButton = widgetList.add(theme.button("Refresh")).widget();
            this.refreshButton.action = () -> {
                this.waitingForRefresh = true;
                ServerSeekerSystem.get().refresh().thenRun(() -> {
                    MinecraftClient.getInstance().execute(() -> {
                        this.waitingForRefresh = false;
                        this.refreshButton = null;
                        this.reload();
                    });
                });
            };
            return true;
        }

        return false;
    }

    @Override
    public void tick() {
        if (this.waitingForRefresh) {
            this.refreshButton.set(switch (this.refreshButton.getText()) {
                default -> "ooo";
                case "ooo" -> "0oo";
                case "0oo" -> "o0o";
                case "o0o" -> "oo0";
            });
        }
        if (this.waitingForAuth) {
            String authToken = ServerSeekerSystem.get().apiKey;
            if (!authToken.isEmpty()) {
                this.reload();
                this.waitingForAuth = false;
            }
        }
        if (this.refreshButton == null && ServerSeekerSystem.get().networkIssue) {
            this.reload();
        }
    }
}
