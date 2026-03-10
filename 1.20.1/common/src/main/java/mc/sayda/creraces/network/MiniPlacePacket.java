package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;

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
@SuppressWarnings("null")
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

            if (!mc.sayda.creraces.capability.DataUtils.canInteractWithMiniBuild(serverPlayer)) {
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();

            // Validate slot
            if (slotX < 0 || slotX >= 4 || slotY < 0 || slotY >= 4 || slotZ < 0 || slotZ >= 4)
                return;

            // Validate player state
            var vars = mc.sayda.creraces.capability.DataUtils.getVariables(serverPlayer).orElse(null);
            if (vars == null || !vars.isSmallBuild()) {
                mc.sayda.creraces.CreRaces.LOGGER.warn(
                        "MiniPlace: Player {} is not in smallBuild mode (vars={}, smallBuild={})",
                        serverPlayer.getName().getString(), vars != null, vars != null && vars.isSmallBuild());
                return;
            }

            // Validate block
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
                mc.sayda.creraces.CreRaces.LOGGER.warn("MiniPlace: invalid block ID {}", blockId);
                return;
            }
            // Whitelist check (Configurable)
            if (mc.sayda.creraces.config.CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get() &&
                    !mc.sayda.creraces.engine.MicroBlockWhitelist.isAllowed(block)) {
                CreRaces.LOGGER.warn("MiniPlace: block {} is not whitelisted", blockId);
                return;
            }

            if (!mc.sayda.creraces.config.CreRacesConfig.MINI_BUILD_ENABLED.get()) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Rejected placement: Feature disabled in config.");
                return;
            }

            // Target slot should be empty (check via global helper)
            if (!MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, slotY, slotZ).isAir()) {
                return;
            }

            Block heldBlock = BuiltInRegistries.BLOCK.get(blockId);
            if (heldBlock == Blocks.AIR) {
                return;
            }

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
                    CreRaces.LOGGER.warn("MiniPlacePacket: Rejected placement: Player {} not holding block {}",
                            serverPlayer.getName().getString(), blockId);
                    return; // No matching block in hand
                }
            }

            BlockState placementState = block.defaultBlockState();
            if (held.getItem() instanceof BlockItem) {
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
                    placementState = resolveUniversalOrientation(block, clickedFace, serverPlayer);
                } else {
                    BlockState state = block.getStateForPlacement(placeContext);
                    if (state != null) {
                        placementState = state;
                    } else {
                        placementState = resolveUniversalOrientation(block, clickedFace, serverPlayer);
                    }
                }
            }

            // Multi-slot occupancy check
            if (block instanceof net.minecraft.world.level.block.DoorBlock) {
                if (slotY + 1 >= 4) {
                    CreRaces.LOGGER.warn("MiniPlacePacket: Door placement rejected: Top of host.");
                    return;
                }
                if (!MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, slotY + 1, slotZ).isAir()) {
                    CreRaces.LOGGER.warn("MiniPlacePacket: Door placement rejected: Upper slot occupied.");
                    return;
                }

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

                if (!MicroBlockEntity.getSlotGlobal(level, hostPos, headSlotX, slotY, headSlotZ).isAir())
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

            mc.sayda.creraces.CreRaces.LOGGER.info("MiniPlacePacket: Placing {} for {} at {} slot {},{},{}", blockId,
                    serverPlayer.getName().getString(), hostPos, slotX, slotY, slotZ);

            // Consume item
            if (!serverPlayer.isCreative()) {
                held.shrink(1);
            }
        });
    }
}
