package mc.sayda.creraces.client;

import mc.sayda.creraces.effect.CharmedEffect;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** The local player's side of Charmed, since a player's view and movement are decided by their own client. */
public final class CharmClient {
    @Nullable
    private static Entity cachedSource;

    private CharmClient() {
    }

    /** Points the view at the source. Returns false when the player isn't charmed, leaving the mouse in charge. */
    public static boolean aimView(LocalPlayer player) {
        Entity source = findSource(player);
        if (source == null) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getFrameTime();
        Vec3 offset = source.getEyePosition(partialTick).subtract(player.getEyePosition(partialTick));
        float wantedYaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));

        // Vanilla never wraps a player's yaw, so turn from the current value by the shortest way.
        float yaw = player.getYRot() + Mth.wrapDegrees(wantedYaw - player.getYRot());
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        return true;
    }

    /** The view stays on the source, so forward is the only direction that doesn't lead away from it. */
    public static void restrictMovement(LocalPlayer player, Input input) {
        Entity source = findSource(player);
        if (source == null) {
            return;
        }
        input.forwardImpulse = Math.max(0.0F, input.forwardImpulse);
        input.leftImpulse = 0.0F;
        if (player.getAbilities().flying) {
            boolean sourceIsAbove = source.getY() > player.getY();
            input.jumping &= sourceIsAbove;
            input.shiftKeyDown &= !sourceIsAbove;
        }
    }

    @Nullable
    private static Entity findSource(LocalPlayer player) {
        if (!CharmedEffect.isCharmed(player) || !(player instanceof IPersistentDataAccessor accessor)) {
            cachedSource = null;
            return null;
        }
        String uuid = accessor.creraces$getPersistentData().getString(CharmedEffect.CHARM_SOURCE_KEY);
        if (cachedSource == null || cachedSource.isRemoved() || !cachedSource.getStringUUID().equals(uuid)) {
            cachedSource = null;
            for (Entity entity : player.clientLevel.entitiesForRendering()) {
                if (entity != player && entity.getStringUUID().equals(uuid)) {
                    cachedSource = entity;
                    break;
                }
            }
        }
        return cachedSource;
    }
}
