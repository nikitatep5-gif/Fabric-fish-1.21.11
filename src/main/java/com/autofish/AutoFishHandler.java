package com.autofish;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Random;

public class AutoFishHandler {

    private static final AutoFishHandler INSTANCE = new AutoFishHandler();

    // How far (in blocks) the bobber must dip below its stable surface to count as a bite.
    // Increase if you get false positives; decrease if bites are missed.
    private static final double BITE_THRESHOLD = 0.025;

    // Ticks the bobber must spend in water before we start watching for bites.
    // Prevents the initial splash motion from triggering a false bite.
    private static final int STABILISE_TICKS = 30;

    // Ticks to wait after reeling in before recasting.
    private static final int BASE_RECAST_DELAY = 12;

    private static final Random RANDOM = new Random();

    // --- state ---
    private boolean enabled = false;
    private int catchCount = 0;

    private int waitTicks = 0;
    private boolean pendingCast = false;

    private int bobberId = -1;
    private int ticksInWater = 0;
    private double stableY = 0;
    private long lastReelMs = 0;

    private AutoFishHandler() {}

    public static AutoFishHandler getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCatchCount() {
        return catchCount;
    }

    // -------------------------------------------------------------------------
    // Toggle
    // -------------------------------------------------------------------------

    public void toggle(MinecraftClient client) {
        enabled = !enabled;
        resetState();
        if (client.player != null) {
            if (enabled) {
                client.player.sendMessage(
                        Text.literal("§a[AutoFish] §fEnabled  — catches: §e" + catchCount), false);
            } else {
                client.player.sendMessage(
                        Text.literal("§c[AutoFish] §fDisabled — total catches: §e" + catchCount), false);
                catchCount = 0;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Per-tick logic
    // -------------------------------------------------------------------------

    public void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return;
        if (client.isPaused()) return;

        // --- wait timer ---
        if (waitTicks > 0) {
            waitTicks--;
            if (waitTicks == 0 && pendingCast) {
                pendingCast = false;
                castOrReel(client);
            }
            return;
        }

        ClientPlayerEntity player = client.player;
        FishingBobberEntity bobber = player.fishHook;

        // No bobber out → cast the rod
        if (bobber == null) {
            if (hasFishingRod(player)) {
                castOrReel(client);
            }
            return;
        }

        // Bobber changed (new cast) → reset tracking
        if (bobber.getId() != bobberId) {
            bobberId = bobber.getId();
            ticksInWater = 0;
            stableY = bobber.getY();
        }

        // Bobber still in air / on ground
        if (!bobber.isTouchingWater()) {
            ticksInWater = 0;
            return;
        }

        ticksInWater++;
        double currentY = bobber.getY();

        if (ticksInWater < STABILISE_TICKS) {
            // Fast-converge the baseline during splash settling
            stableY = stableY * 0.80 + currentY * 0.20;
            return;
        }

        double dip = stableY - currentY;

        if (dip > BITE_THRESHOLD && System.currentTimeMillis() - lastReelMs > 1500) {
            // ── Fish bite detected ──
            catchCount++;
            lastReelMs = System.currentTimeMillis();

            castOrReel(client); // reels in (bobber is out)
            resetState();

            int delay = BASE_RECAST_DELAY + RANDOM.nextInt(6); // 12–17 ticks, looks human
            waitTicks = delay;
            pendingCast = true;
        } else {
            // Slowly drift the baseline to account for natural water movement
            stableY = stableY * 0.94 + currentY * 0.06;
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void castOrReel(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        Hand hand = getFishingHand(player);
        if (hand == null) return;

        client.interactionManager.interactItem(player, hand);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void resetState() {
        waitTicks = 0;
        pendingCast = false;
        ticksInWater = 0;
        stableY = 0;
        bobberId = -1;
    }

    private boolean hasFishingRod(ClientPlayerEntity player) {
        return getFishingHand(player) != null;
    }

    private Hand getFishingHand(ClientPlayerEntity player) {
        if (player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof FishingRodItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getStackInHand(Hand.OFF_HAND).getItem() instanceof FishingRodItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }
}
