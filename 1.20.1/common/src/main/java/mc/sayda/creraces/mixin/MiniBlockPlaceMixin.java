package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.MicroBlockWhitelist;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.MiniPlacePacket;
import mc.sayda.creraces.network.MiniRemovePacket;
import mc.sayda.creraces.network.MiniUsePacket;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

/**
 * Client-side Mixin that intercepts vanilla block placement and breaking
 * when the player has smallBuild mode active.
 *
 * Placement: converts a normal right-click into a MiniPlacePacket,
 * computing the target slot from the fractional hit position.
 *
 * Breaking: when targeting a mini_block host, sends a MiniRemovePacket
 * for the specific slot instead of breaking the host block.
 */
@Mixin(MultiPlayerGameMode.class)
public class MiniBlockPlaceMixin {

    @Unique
    private long creraces$lastMiniInteractionTime = 0;

    // ─── Placement
    // ────────────────────────────────────────────────────────────────

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptMiniBlockPlace(
            LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (!CreRacesConfig.MINI_BUILD_ENABLED.get())
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            if (!vars.isSmallBuild()) {
                return;
            }

            ItemStack held = player.getItemInHand(hand);

            // Check if we're right-clicking an EXISTING micro-block slot
            BlockPos hitPos = hitResult.getBlockPos();
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (hitPos != null && minecraft.level != null &&
                    minecraft.level.getBlockState(hitPos).is(ModBlocks.MICRO_BLOCK.get())) {

                // Calculate which sub-slot was hit
                Vec3 normal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
                Vec3 hitCenter = hitResult.getLocation().subtract(normal.scale(0.005));

                int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.x);
                int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.y);
                int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.z);

                // Check if the slot contains an interactive block
                if (minecraft.level.getBlockEntity(hitPos) instanceof MicroBlockEntity micro) {
                    BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
                    boolean isInteractive = MicroBlockWhitelist.isInteractive(slotState.getBlock());

                    if (isInteractive) {
                        // Local prediction for immediate feedback (e.g. stopping Jukebox music)
                        micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
                        BoundaryHandler.sendMiniUse(new MiniUsePacket(hitPos, slotX, slotY, slotZ, hand));
                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                }

                // If holding a non-block item, still try to use it (for empty hand etc)
                boolean holdingBlock = !held.isEmpty() && held.getItem() instanceof BlockItem;
                if (!holdingBlock) {
                    BoundaryHandler.sendMiniUse(new MiniUsePacket(hitPos, slotX, slotY, slotZ, hand));
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
            }

            if (held.isEmpty())
                return;

            if (!(held.getItem() instanceof BlockItem blockItem))
                return;
            Block heldBlock = blockItem.getBlock();
            if (!MicroBlockWhitelist.isAllowed(heldBlock))
                return;

            // Compute the target center for the new mini-block
            // Move slightly outward from the hit face to target the new slot space
            Vec3 placeNormal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
            Vec3 newCenter = hitResult.getLocation().add(placeNormal.scale(0.125)); // 0.25 / 2 = 0.125

            BlockPos hostPos = BlockPos.containing(newCenter.x, newCenter.y, newCenter.z);

            int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.x);
            int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.y);
            int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.z);

            // VALIDATION: Whitelist and Slot-Empty check before consumption/packet
            if (!MicroBlockWhitelist.isAllowed(heldBlock)) {
                return;
            }

            // Check if slot is empty or host is replaceable on client-side
            BlockState hostState = minecraft.level.getBlockState(hostPos);
            if (hostState.is(ModBlocks.MICRO_BLOCK.get())) {
                if (minecraft.level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro) {
                    if (!micro.getSlot(slotX, slotY, slotZ).isAir()) {
                        return;
                    }
                }
            } else if (!hostState.canBeReplaced()) {
                // Cannot place a MicroBlock here
                return;
            }

            long now = net.minecraft.Util.getMillis();
            creraces$lastMiniInteractionTime = now;

            BoundaryHandler.sendMiniPlace(new MiniPlacePacket(
                    hostPos, slotX, slotY, slotZ,
                    hitResult.getDirection(), hitResult.getLocation(),
                    BuiltInRegistries.BLOCK.getKey(heldBlock)));

            // REMOVED: Client-side item consumption prediction to avoid ghost-loss
            // Server-side MiniPlacePacket already handles consumption.

            cir.setReturnValue(InteractionResult.SUCCESS);
        });
    }

    // ─── Breaking ────────────────────────────────────────────────────────────────

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptMiniBlockStartBreak(BlockPos pos, Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        if (creraces$handleMiniBreak(pos, face, true)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptMiniBlockContinueBreak(BlockPos pos, Direction face,
            CallbackInfoReturnable<Boolean> cir) {
        if (creraces$handleMiniBreak(pos, face, false)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean creraces$handleMiniBreak(BlockPos pos, Direction face, boolean isStart) {
        if (!CreRacesConfig.MINI_BUILD_ENABLED.get())
            return false;

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null)
            return false;
        if (!(minecraft.hitResult instanceof BlockHitResult bhr))
            return false;

        // Only intercept if the targeted block is a MicroBlock host
        if (!minecraft.level.getBlockState(pos).is(ModBlocks.MICRO_BLOCK.get()))
            return false;

        boolean isSmallBuild = DataUtils.getVariables(minecraft.player)
                .map(mc.sayda.creraces.capability.IPlayerVariables::isSmallBuild)
                .orElse(false);

        if (isSmallBuild) {
            if (isStart) {
                long now = net.minecraft.Util.getMillis();
                if (now - creraces$lastMiniInteractionTime < 50) {
                    return true; // Suppress rapid spam
                }
                creraces$lastMiniInteractionTime = now;

                // --- Targeted Mini-Block Breaking ---
                Vec3 normal = Vec3.atLowerCornerOf(bhr.getDirection().getNormal());
                // Use a very small backstep (0.005) to stay inside thin blocks like
                // vines/ladders
                Vec3 hitCenter = bhr.getLocation().subtract(normal.scale(0.005));

                int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.x);
                int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.y);
                int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.z);

                // --- Local Jukebox Music Stop Prediction ---
                if (minecraft.level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                    BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
                    if (slotState.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
                        minecraft.level.levelEvent(null, 1010, pos, 0);
                    }
                }

                BoundaryHandler.sendMiniRemove(new MiniRemovePacket(pos, slotX, slotY, slotZ));
            }
            return true; // Always suppress vanilla block-breaking in smallBuild
        } else {
            // Not in minibuild mode: Host block breaking requires sneaking
            return !minecraft.player.isShiftKeyDown();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

}
