package com.autofish;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draws the AutoFish status overlay in the top-left corner of the HUD.
 *
 * Uses Fabric's {@link HudRenderCallback} rather than a mixin into
 * {@code InGameHud.render}, since that method's signature changes between
 * Minecraft versions (it takes a {@code RenderTickCounter} in 1.21.11).
 */
@Environment(EnvType.CLIENT)
public final class HudOverlay {

    private HudOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register(HudOverlay::onHudRender);
    }

    private static void onHudRender(DrawContext context, Object tickCounter) {
        AutoFishHandler handler = AutoFishHandler.getInstance();
        if (!handler.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) return;

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
