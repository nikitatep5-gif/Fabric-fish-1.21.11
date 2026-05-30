package com.autofish.mixin;

import com.autofish.AutoFishHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;F)V",
            at = @At("TAIL")
    )
    private void renderAutoFishOverlay(DrawContext context, float tickDelta, CallbackInfo ci) {
        AutoFishHandler handler = AutoFishHandler.getInstance();
        if (!handler.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;

        String line1 = "§a● AutoFish §fON";
        String line2 = "§7Catches: §e" + handler.getCatchCount();
        String line3 = "§7Press §f[.]§7 to stop";

        int x = 5;
        int y = 5;
        int lineHeight = 10;

        context.drawTextWithShadow(client.textRenderer, Text.literal(line1), x, y, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal(line2), x, y + lineHeight, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal(line3), x, y + lineHeight * 2, 0xFFFFFF);
    }
}
