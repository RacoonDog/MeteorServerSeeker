package de.damcraft.serverseeker;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class DiscordAvatar extends Texture {
    private static DiscordAvatar avatar;

    private volatile String url;
    private long lastUpdate = -1;

    private DiscordAvatar() {
        MeteorExecutor.execute(this::updateUserInfo);
    }

    public static DiscordAvatar get() {
        if (avatar == null) {
            avatar = new DiscordAvatar();
        } else {
            avatar.update();
        }
        return avatar;
    }

    private void update() {
        if (System.currentTimeMillis() - lastUpdate >= 120 * 1000) {
            MeteorExecutor.execute(this::updateUserInfo);
        }

        MeteorExecutor.execute(() -> load(ServerSeekerSystem.get().userInfo.discord_avatar_url));
    }

    private void updateUserInfo() {
        lastUpdate = System.currentTimeMillis();

        ServerSeekerSystem.get().refresh().thenRun(() -> {
            String avatarUrl = ServerSeekerSystem.get().userInfo.discord_avatar_url;
            if (!avatarUrl.isEmpty()) load(avatarUrl);
        });
    }

    private void load(String avatarUrl) {
        if (this.url != null && this.url.equals(avatarUrl)) return;
        this.url = avatarUrl;

        if (avatarUrl.isEmpty()) {
            avatarUrl = "https://cdn.discordapp.com/embed/avatars/1.png?size=32";
        } else {
            avatarUrl += "?size=32";
        }

        try (InputStream stream = Http.get(avatarUrl)
            .exceptionHandler(e -> LOG.error("Network error: " + e.getMessage()))
            .sendInputStream()) {
            if (stream == null) return;

            BufferedImage avatarImage = ImageIO.read(stream);

            byte[] data = new byte[avatarImage.getWidth() * avatarImage.getHeight() * 3];
            int[] pixel = new int[4];
            int i = 0;

            for (int y = 0; y < avatarImage.getHeight(); y++) {
                for (int x = 0; x < avatarImage.getWidth(); x++) {
                    avatarImage.getData().getPixel(x, y, pixel);

                    for (int j = 0; j < 3; j++) {
                        data[i] = (byte) pixel[j];
                        i++;
                    }
                }
            }

            RenderSystem.recordRenderCall(() -> {
                upload(BufferUtils.createByteBuffer(data.length).put(data));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void upload(ByteBuffer data) {
        upload(32, 32, data, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest, false);
    }
}
