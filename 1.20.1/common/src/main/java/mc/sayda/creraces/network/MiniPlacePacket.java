package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.MicroBlockWhitelist;
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** C2S: places the held block as a mini-block in one slot of a micro block. */
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

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer))
                return;
            if (!MiniBuildRequests.canTarget(serverPlayer, hostPos, slotX, slotY, slotZ, "MiniPlacePacket"))
                return;

            ServerLevel level = serverPlayer.serverLevel();

            IPlayerVariables vars = DataUtils.getVariables(serverPlayer).orElse(null);
            if (vars == null || !vars.isSmallBuild()) {
                CreRaces.LOGGER.warn("MiniPlace: Player {} is not in smallBuild mode (vars={}, smallBuild={})",
                        serverPlayer.getName().getString(), vars != null, vars != null && vars.isSmallBuild());
                return;
            }

            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == Blocks.AIR) {
                CreRaces.LOGGER.warn("MiniPlace: invalid block ID {}", blockId);
                return;
            }
            if (CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get() && !MicroBlockWhitelist.isAllowed(block)) {
                CreRaces.LOGGER.warn("MiniPlace: block {} is not whitelisted", blockId);
                return;
            }
            if (!CreRacesConfig.MINI_BUILD_ENABLED.get()) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Rejected placement: Feature disabled in config.");
                return;
            }
            if (CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST.get().contains(level.dimension().location().toString())) {
                return;
            }
            if (!MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, slotY, slotZ).isAir()) {
                return;
            }

            InteractionHand hand = findHandHolding(serverPlayer, blockId);
            if (hand == null) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Rejected placement: Player {} not holding block {}",
                        serverPlayer.getName().getString(), blockId);
                return;
            }
            ItemStack held = serverPlayer.getItemInHand(hand);

            BlockState placementState = resolvePlacementState(serverPlayer, hand, held, block);
            if (!placeInSlots(level, block, placementState)) {
                return;
            }

            CreRaces.LOGGER.debug("MiniPlacePacket: Placing {} for {} at {} slot {},{},{}", blockId,
                    serverPlayer.getName().getString(), hostPos, slotX, slotY, slotZ);

            if (!serverPlayer.isCreative()) {
                held.shrink(1);
            }
        });
    }

    /** The hand in use gets first pick, then main hand, then off hand; null if neither holds the block. */
    private static InteractionHand findHandHolding(ServerPlayer player, ResourceLocation blockId) {
        InteractionHand usedHand = player.getUsedItemHand();
        if (holdsBlock(player.getItemInHand(usedHand), blockId)) return usedHand;
        if (holdsBlock(player.getMainHandItem(), blockId)) return InteractionHand.MAIN_HAND;
        if (holdsBlock(player.getOffhandItem(), blockId)) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean holdsBlock(ItemStack stack, ResourceLocation blockId) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem
                && BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).equals(blockId);
    }

    private BlockState resolvePlacementState(ServerPlayer player, InteractionHand hand, ItemStack held, Block block) {
        // Wall torches, ladders and vines depend on the clicked face alone, which vanilla's placement
        // logic would get wrong at slot scale
        Block wallTorch = getWallTorch(block);
        if ((wallTorch != null && clickedFace.getAxis().isHorizontal())
                || block instanceof LadderBlock || block instanceof VineBlock) {
            return resolveUniversalOrientation(block, clickedFace, player);
        }

        // Scale the hit from inside the slot up to a whole block, so vanilla sees a normal-sized click
        Vec3 virtualHitPos = new Vec3(
                toFullBlock(hitPos.x, hostPos.getX(), slotX),
                toFullBlock(hitPos.y, hostPos.getY(), slotY),
                toFullBlock(hitPos.z, hostPos.getZ(), slotZ));
        BlockPlaceContext placeContext = new BlockPlaceContext(player, hand, held,
                new BlockHitResult(virtualHitPos, clickedFace, hostPos, false));
        BlockState state = block.getStateForPlacement(placeContext);
        return state != null ? state : resolveUniversalOrientation(block, clickedFace, player);
    }

    private static double toFullBlock(double hit, int hostCoord, int slot) {
        return hostCoord + (hit - hostCoord - slot * (1.0 / MicroBlockEntity.SIZE)) * MicroBlockEntity.SIZE;
    }

    /** Writes the state into its slot, plus the second slot for doors and beds. False if it doesn't fit. */
    private boolean placeInSlots(ServerLevel level, Block block, BlockState placementState) {
        if (block instanceof DoorBlock) {
            if (!MiniBuildRequests.isInGrid(slotY + 1)) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Door placement rejected: Top of host.");
                return false;
            }
            if (!MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, slotY + 1, slotZ).isAir()) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Door placement rejected: Upper slot occupied.");
                return false;
            }
            MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ,
                    placementState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY + 1, slotZ,
                    placementState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        } else if (block instanceof BedBlock) {
            Direction facing = placementState.getValue(BedBlock.FACING);
            int headSlotX = slotX + facing.getStepX();
            int headSlotZ = slotZ + facing.getStepZ();
            if (!MiniBuildRequests.isInGrid(headSlotX) || !MiniBuildRequests.isInGrid(headSlotZ)) {
                CreRaces.LOGGER.warn("MiniPlacePacket: Bed placement rejected: Head slot out of bounds at {},{}",
                        headSlotX, headSlotZ);
                return false;
            }
            if (!MicroBlockEntity.getSlotGlobal(level, hostPos, headSlotX, slotY, headSlotZ).isAir()) {
                return false;
            }
            MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ,
                    placementState.setValue(BedBlock.PART, BedPart.FOOT));
            MicroBlockEntity.setSlotGlobal(level, hostPos, headSlotX, slotY, headSlotZ,
                    placementState.setValue(BedBlock.PART, BedPart.HEAD));
        } else {
            MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, slotY, slotZ, placementState);
        }
        return true;
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

    /** Orients a block from the clicked face and the player's facing alone. */
    private static BlockState resolveUniversalOrientation(Block block, Direction clickedFace, ServerPlayer player) {
        BlockState def = block.defaultBlockState();

        if (block instanceof LadderBlock && clickedFace.getAxis().isHorizontal()) {
            return def.setValue(LadderBlock.FACING, clickedFace);
        }

        Block wallTorch = getWallTorch(block);
        if (wallTorch != null && clickedFace.getAxis().isHorizontal()) {
            return wallTorch.defaultBlockState().setValue(WallTorchBlock.FACING, clickedFace);
        }

        if (block instanceof VineBlock) {
            Direction attachFace = clickedFace.getOpposite();
            if (attachFace.getAxis().isHorizontal() || attachFace == Direction.UP) {
                return def.setValue(VineBlock.getPropertyForFace(attachFace), true);
            }
        }

        if (block instanceof DoorBlock) {
            return def.setValue(DoorBlock.FACING, player.getDirection())
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        }

        if (block instanceof FenceGateBlock) {
            return def.setValue(FenceGateBlock.FACING, player.getDirection());
        }

        if (block instanceof BedBlock) {
            return def.setValue(BedBlock.FACING, player.getDirection())
                    .setValue(BedBlock.PART, BedPart.FOOT);
        }

        if (def.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && clickedFace.getAxis().isHorizontal()) {
            return def.setValue(BlockStateProperties.HORIZONTAL_FACING, clickedFace);
        }

        return def;
    }
}
