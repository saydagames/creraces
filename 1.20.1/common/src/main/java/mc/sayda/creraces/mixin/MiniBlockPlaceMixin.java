package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.MicroBlockWhitelist;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.MiniPlacePacket;
import mc.sayda.creraces.network.MiniRemovePacket;
import mc.sayda.creraces.network.MiniUsePacket;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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

@Mixin(MultiPlayerGameMode.class)
public class MiniBlockPlaceMixin {

    @Unique
    private long creraces$lastMiniInteractionTime = 0;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptMiniBlockPlace(
            LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (!CreRacesConfig.MINI_BUILD_ENABLED.get())
            return;

        if (!DataUtils.canInteractWithMiniBuild(player)) {
            // Cancel with SUCCESS if looking at a MicroBlock to prevent vanilla logic
            BlockPos targeted = hitResult.getBlockPos();
            if (targeted != null && player.level().getBlockState(targeted).is(ModBlocks.MICRO_BLOCK.get())) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }

        IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
        if (vars == null || !vars.isSmallBuild()) {
            return;
        }

        ResourceLocation creraces$dim = player.level().dimension().location();
        if (CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST.get().contains(creraces$dim.toString())) {
            return;
        }

        ItemStack held = player.getItemInHand(hand);
        BlockPos hitPos = hitResult.getBlockPos();
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();

        if (hitPos != null && minecraft.level != null &&
                minecraft.level.getBlockState(hitPos).is(ModBlocks.MICRO_BLOCK.get())) {

            Vec3 normal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
            Vec3 hitCenter = hitResult.getLocation().subtract(normal.scale(0.005));

            int slotX = MicroBlockEntity.clampSlot(hitCenter.x);
            int slotY = MicroBlockEntity.clampSlot(hitCenter.y);
            int slotZ = MicroBlockEntity.clampSlot(hitCenter.z);

            if (minecraft.level.getBlockEntity(hitPos) instanceof MicroBlockEntity micro) {
                BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
                boolean isInteractive = MicroBlockWhitelist.isInteractive(slotState.getBlock());

                if (isInteractive) {
                    micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
                    BoundaryHandler.sendMiniUse(new MiniUsePacket(hitPos, slotX, slotY, slotZ, hand));
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                boolean holdingBlock = !held.isEmpty() && held.getItem() instanceof BlockItem;
                boolean isSlotReplaceable = slotState.isAir() || slotState.canBeReplaced();
                if (holdingBlock && isSlotReplaceable) {
                    Block heldBlock = ((BlockItem) held.getItem()).getBlock();
                    if (!CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get()
                            || mc.sayda.creraces.engine.MicroBlockWhitelist.isAllowed(heldBlock)) {
                        BoundaryHandler.sendMiniPlace(new MiniPlacePacket(
                                hitPos, slotX, slotY, slotZ, hitResult.getDirection(),
                                hitResult.getLocation(),
                                BuiltInRegistries.BLOCK.getKey(heldBlock)));

                        if (player.level().isClientSide()) {
                            micro.setSlot(slotX, slotY, slotZ, heldBlock.defaultBlockState());
                        }

                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                }
            }

            boolean holdingBlock = !held.isEmpty() && held.getItem() instanceof BlockItem;
            if (!holdingBlock) {
                BoundaryHandler.sendMiniUse(new MiniUsePacket(hitPos, slotX, slotY, slotZ, hand));
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }

        if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Block heldBlock = blockItem.getBlock();
        if (CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get()
                && !MicroBlockWhitelist.isAllowed(heldBlock)) {
            return;
        }

        if (minecraft.level == null)
            return;

        Vec3 placeNormal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
        BlockPos finalHitPos = hitResult.getBlockPos();
        if (finalHitPos == null)
            return;
        boolean replaceable = minecraft.level.getBlockState(finalHitPos).canBeReplaced();
        Vec3 localHitLoc = hitResult.getLocation();
        if (localHitLoc == null)
            return;
        Vec3 newCenter = (replaceable ? localHitLoc.subtract(placeNormal.scale(0.005))
                : localHitLoc.add(placeNormal.scale(0.125)));

        if (newCenter == null)
            return;

        BlockPos hostPos = BlockPos.containing(newCenter.x, newCenter.y, newCenter.z);

        int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.x);
        int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.y);
        int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(newCenter.z);

        BlockState hostState = minecraft.level.getBlockState(hostPos);
        if (hostState.is(ModBlocks.MICRO_BLOCK.get())) {
            if (minecraft.level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro) {
                BlockState targetSlotState = micro.getSlot(slotX, slotY, slotZ);
                if (!targetSlotState.isAir() && !targetSlotState.canBeReplaced()) {
                    cir.setReturnValue(InteractionResult.FAIL);
                    return;
                }
            }
        } else if (!hostState.canBeReplaced()) {
            return;
        }

        long now = net.minecraft.Util.getMillis();
        creraces$lastMiniInteractionTime = now;

        ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(heldBlock);
        if (blockKey == null)
            return;

        BoundaryHandler.sendMiniPlace(new MiniPlacePacket(
                hostPos, slotX, slotY, slotZ,
                hitResult.getDirection(), hitResult.getLocation(),
                blockKey));

        if (player.level().isClientSide()) {
            if (!hostState.is(ModBlocks.MICRO_BLOCK.get())) {
                player.level().setBlock(hostPos, ModBlocks.MICRO_BLOCK.get().defaultBlockState(), 3);
            }
            if (player.level().getBlockEntity(hostPos) instanceof MicroBlockEntity micro) {
                micro.setSlot(slotX, slotY, slotZ, heldBlock.defaultBlockState());
            }
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return false;

        if (!DataUtils.canInteractWithMiniBuild(minecraft.player)) {
            return false;
        }

        IPlayerVariables vars = DataUtils.getVariables(minecraft.player).orElse(null);
        if (vars == null || !vars.isSmallBuild()) {
            return false;
        }

        if (!CreRacesConfig.MINI_BUILD_ENABLED.get())
            return false;

        if (!(minecraft.hitResult instanceof BlockHitResult bhr))
            return false;

        Block microBlock = ModBlocks.MICRO_BLOCK.get();
        if (microBlock == null || !minecraft.level.getBlockState(pos).is(microBlock))
            return false;

        if (isStart) {
            long now = net.minecraft.Util.getMillis();
            if (now - creraces$lastMiniInteractionTime < CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS.get()) {
                return true;
            }
            creraces$lastMiniInteractionTime = now;

            Vec3 normal = Vec3.atLowerCornerOf(bhr.getDirection().getNormal());
            Vec3 hitCenter = bhr.getLocation().subtract(normal.scale(0.005));

            int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.x);
            int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.y);
            int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.z);

            if (minecraft.level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
                if (slotState.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
                    minecraft.level.levelEvent(minecraft.player, 1010, pos, 0);
                }
                micro.setSlot(slotX, slotY, slotZ, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }

            BoundaryHandler.sendMiniRemove(new MiniRemovePacket(pos, slotX, slotY, slotZ));
        }
        return true;
    }
}
