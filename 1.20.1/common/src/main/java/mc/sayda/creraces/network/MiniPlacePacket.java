package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.engine.MicroBlockWhitelist;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * C2S: Client requests to place a mini-block in a specific slot.
 */
public class MiniPlacePacket {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "mini_place");

    private final BlockPos hostPos;
    private final int slotX, slotY, slotZ;
    private final Direction clickedFace;
    private final Vec3 hitPos;
    private final ResourceLocation blockId;

    public MiniPlacePacket(BlockPos hostPos, int slotX, int slotY, int slotZ,
            Direction clickedFace, Vec3 hitPos,
            ResourceLocation blockId) {
        this.hostPos = hostPos;
        this.slotX = slotX;
        this.slotY = slotY;
        this.slotZ = slotZ;
        this.clickedFace = clickedFace;
        this.hitPos = hitPos;
        this.blockId = blockId;
    }

    public MiniPlacePacket(FriendlyByteBuf buf) {
        this.hostPos = buf.readBlockPos();
        this.slotX = buf.readByte();
        this.slotY = buf.readByte();
        this.slotZ = buf.readByte();
        this.clickedFace = buf.readEnum(Direction.class);
        this.hitPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.blockId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(hostPos);
        buf.writeByte(slotX);
        buf.writeByte(slotY);
        buf.writeByte(slotZ);
        buf.writeEnum(clickedFace);
        buf.writeDouble(hitPos.x);
        buf.writeDouble(hitPos.y);
        buf.writeDouble(hitPos.z);
        buf.writeResourceLocation(blockId);
    }

    private static Block getWallTorch(Block block) {
        if (block == Blocks.TORCH)
            return Blocks.WALL_TORCH;
        if (block == Blocks.SOUL_TORCH)
            return Blocks.SOUL_WALL_TORCH;
        if (block == Blocks.REDSTONE_TORCH)
            return Blocks.REDSTONE_WALL_TORCH;
        return null;
    }

    private static BlockState resolveUniversalOrientation(Block block, Direction clickedFace, ServerPlayer player) {
        BlockState def = block.defaultBlockState();

        // 1. Ladders
        if (block instanceof net.minecraft.world.level.block.LadderBlock) {
            if (clickedFace.getAxis().isHorizontal()) {
                return def.setValue(net.minecraft.world.level.block.LadderBlock.FACING, clickedFace);
            }
        }

        // 2. Torches (Unified Logic)
        Block wallTorch = getWallTorch(block);
        if (wallTorch != null && clickedFace.getAxis().isHorizontal()) {
            return wallTorch.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING,
                    clickedFace);
        }

        // 3. Vines
        if (block instanceof net.minecraft.world.level.block.VineBlock) {
            net.minecraft.core.Direction attachFace = clickedFace.getOpposite();
            if (attachFace.getAxis().isHorizontal() || attachFace == Direction.UP) {
                return def.setValue(
                        net.minecraft.world.level.block.VineBlock.getPropertyForFace(attachFace),
                        true);
            }
        }

        // 4. Doors (Miniature version)
        if (block instanceof net.minecraft.world.level.block.DoorBlock) {
            Direction facing = player.getDirection();
            return def.setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing)
                    .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        }

        // 5. Fence Gates
        if (block instanceof net.minecraft.world.level.block.FenceGateBlock) {
            Direction facing = player.getDirection();
            return def.setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, facing);
        }

        // 6. Beds (Miniature version)
        if (block instanceof net.minecraft.world.level.block.BedBlock) {
            Direction facing = player.getDirection();
            return def.setValue(net.minecraft.world.level.block.BedBlock.FACING, facing)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        }

        // 7. Generic Horizontal Facing blocks
        if (def.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            if (clickedFace.getAxis().isHorizontal()) {
                @SuppressWarnings("unchecked")
                net.minecraft.world.level.block.state.properties.Property<Direction> prop = (net.minecraft.world.level.block.state.properties.Property<Direction>) net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
                return def.setValue(prop, clickedFace);
            }
        }

        return def;
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer))
                return;
            ServerLevel level = serverPlayer.serverLevel();

            // Validate slot
            if (slotX < 0 || slotX >= 4 || slotY < 0 || slotY >= 4 || slotZ < 0 || slotZ >= 4)
                return;

            // Validate block
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == null || block == Blocks.AIR)
                return;
            if (!MicroBlockWhitelist.isAllowed(block)) {
                CreRaces.LOGGER.warn("MiniPlace: block {} is not whitelisted", blockId);
                return;
            }

            // Target slot should be empty (check via global helper)
            if (!MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, slotY, slotZ).isAir())
                return;

            // Compute the correct BlockState (including rotation)
            InteractionHand handUsed = serverPlayer.getUsedItemHand();
            ItemStack held = serverPlayer.getItemInHand(handUsed);
            if (held.isEmpty() || !BuiltInRegistries.BLOCK.getKey(
                    held.getItem() instanceof BlockItem bi ? bi.getBlock() : Blocks.AIR)
                    .equals(blockId)) {
                // Try to find the block in any hand if the primary hand doesn't match
                if (serverPlayer.getMainHandItem().getItem() instanceof BlockItem bi
                        && BuiltInRegistries.BLOCK.getKey(bi.getBlock()).equals(blockId)) {
                    held = serverPlayer.getMainHandItem();
                    handUsed = InteractionHand.MAIN_HAND;
                } else if (serverPlayer.getOffhandItem().getItem() instanceof BlockItem bi
                        && BuiltInRegistries.BLOCK.getKey(bi.getBlock()).equals(blockId)) {
                    held = serverPlayer.getOffhandItem();
                    handUsed = InteractionHand.OFF_HAND;
                } else {
                    return; // No matching block in hand
                }
            }

            BlockState placementState = block.defaultBlockState();
            if (held.getItem() instanceof BlockItem) {
                // ... (vx, vy, vz logic)
                double vx = hostPos.getX() + ((hitPos.x - hostPos.getX() - (slotX * 0.25)) * 4.0);
                double vy = hostPos.getY() + ((hitPos.y - hostPos.getY() - (slotY * 0.25)) * 4.0);
                double vz = hostPos.getZ() + ((hitPos.z - hostPos.getZ() - (slotZ * 0.25)) * 4.0);
                Vec3 virtualHitPos = new Vec3(vx, vy, vz);

                BlockPlaceContext placeContext = new BlockPlaceContext(
                        serverPlayer, handUsed, held,
                        new BlockHitResult(virtualHitPos, clickedFace, hostPos, false));

                // PRIORITIZE Wall Torch, Ladder, and Vine Logic if clicked appropriately
                Block wallTorch = getWallTorch(block);
                if (wallTorch != null && clickedFace.getAxis().isHorizontal()) {
                    placementState = wallTorch.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, clickedFace);
                } else if (block instanceof net.minecraft.world.level.block.LadderBlock ||
                        block instanceof net.minecraft.world.level.block.VineBlock) {
                    // Force bypass survival logic for climbables to ensure they attach to the
                    // micro-wall
                    placementState = resolveUniversalOrientation(block, clickedFace, serverPlayer);
                } else {
                    BlockState state = block.getStateForPlacement(placeContext);
                    if (state != null) {
                        placementState = state;
                    } else {
                        // Universal Fallback
                        placementState = resolveUniversalOrientation(block, clickedFace, serverPlayer);
                    }
                }
            }

            // Multi-slot placement for Doors and Beds — guard against OOB slot writes
            if (block instanceof net.minecraft.world.level.block.DoorBlock) {
                // Door needs slotY and slotY+1 — reject if top slot would overflow
                if (slotY + 1 >= 4)
                    return;
                MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ,
                        placementState.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
                MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY + 1, slotZ,
                        placementState.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            } else if (block instanceof net.minecraft.world.level.block.BedBlock) {
                Direction facing = placementState.getValue(net.minecraft.world.level.block.BedBlock.FACING);
                int headSlotX = slotX + facing.getStepX();
                int headSlotZ = slotZ + facing.getStepZ();
                // Bed head slot must stay within the 4x4x4 grid
                if (headSlotX < 0 || headSlotX >= 4 || headSlotZ < 0 || headSlotZ >= 4)
                    return;
                MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ,
                        placementState.setValue(net.minecraft.world.level.block.BedBlock.PART,
                                net.minecraft.world.level.block.state.properties.BedPart.FOOT));
                MicroBlockEntity.setSlotGlobal(level, hostPos, headSlotX, slotY, headSlotZ,
                        placementState.setValue(net.minecraft.world.level.block.BedBlock.PART,
                                net.minecraft.world.level.block.state.properties.BedPart.HEAD));
            } else {
                MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ, placementState);
            }

            // Consume item
            if (!serverPlayer.isCreative()) {
                held.shrink(1);
            }
        });
    }
}
