package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.ServerSeekerSystem;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.MinecraftClient;

/**
 * Abstract away common logic for screens that require an authenticated connection to the ServerSeeker API.
 *
 * @see ServerSeekerScreen
 * @see GetInfoScreen
 */
public abstract class AbstractAuthRequiredScreen extends WindowScreen {
    private boolean waitingForAuth = false;
    private volatile boolean waitingForRefesh = false;
    private WButton okButton;

    public AbstractAuthRequiredScreen(GuiTheme theme, String title) {
        super(theme, title);
    }

    protected boolean initWarnings() {
        ServerSeekerSystem system = ServerSeekerSystem.get();
        String authToken = system.apiKey;

        if (authToken.isEmpty() || system.userInfo.discord_username == null) {
            WHorizontalList widgetList = add(theme.horizontalList()).expandX().widget();
            widgetList.add(theme.label("Please authenticate with Discord. "));
            WButton loginButton = widgetList.add(theme.button("Login")).widget();
            loginButton.action = () -> {
                this.waitingForAuth = true;
                this.client.setScreen(new LoginWithDiscordScreen(this));
            };

            add(theme.horizontalSeparator()).expandX();
            WVerticalList verticalList = add(theme.verticalList()).expandX().widget();
            verticalList.add(theme.label("Alternatively, you can manually insert an authentication token."));
            WHorizontalList manualList = verticalList.add(theme.horizontalList()).expandX().widget();
            manualList.add(theme.label("Auth Token:"));
            WTextBox tokenBox = manualList.add(theme.textBox("")).expandX().widget();
            okButton = manualList.add(theme.button("Accept Token")).expandX().widget();
            okButton.action = () -> {
                this.waitingForRefesh = true;
                ServerSeekerSystem.get().invalidate();
                ServerSeekerSystem.get().apiKey = tokenBox.get();
                ServerSeekerSystem.get().refresh().thenAccept(res -> {
                    MinecraftClient.getInstance().execute(() -> {
                        this.waitingForRefesh = false;
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
        if (this.waitingForAuth) {
            String authToken = ServerSeekerSystem.get().apiKey;
            if (!authToken.isEmpty()) {
                this.reload();
                this.waitingForAuth = false;
            }
        }
        if (this.waitingForRefesh) {
            okButton.set(switch (okButton.getText()) {
                default -> "Ooo";
                case "Ooo" -> "oOo";
                case "oOo" -> "ooO";
            });
        }
    }
}
