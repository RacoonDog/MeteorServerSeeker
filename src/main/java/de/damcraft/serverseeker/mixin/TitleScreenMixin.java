package de.damcraft.serverseeker.mixin;

import de.damcraft.serverseeker.gui.InstallMeteorScreen;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;
import java.time.LocalDate;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Unique private static boolean firstLoad = true;

    @Inject(at = @At("HEAD"), method = "init()V", cancellable = true)
    private void init(CallbackInfo info) {
        // Check if meteor-client is installed
        if (!FabricLoader.getInstance().isModLoaded("meteor-client")) {
            info.cancel();
            MinecraftClient.getInstance().setScreen(new InstallMeteorScreen());
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", ordinal = 0))
    private void displayNotice(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (firstLoad) {
            firstLoad = false;
            YesNoPrompt.create()
                .title("Important Notice")
                .message("On 10/12/2024, GitHub responded to a DMCA takedown notice submitted by the Fifth Column griefing group")
                .message("that claimed that the ServerSeekerV2 project infringed on their copyright for the ServerSeeker project.")
                .message("This claim is entirely baseless and constitutes an abuse of GitHub's DMCA system as well as perjury.")
                .message("This action is completely reprehensible.")
                .message("Do you want to read more?")
                .dontShowAgainCheckboxVisible(Duration.between(LocalDate.of(2024, 12, 10).atStartOfDay(), LocalDate.now().atStartOfDay()).toDays() > 7L)
                .onYes(() -> Util.getOperatingSystem().open("https://gist.github.com/RacoonDog/41d88a21e2d7bab4d1ad075b77ee1248"))
                .id("5c-false-dmca")
                .show();
        }
    }
}
