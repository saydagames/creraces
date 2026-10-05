package mc.sayda.creraces.block.entity;

import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.MicroBlock;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.ISleepSlotTracker;
import mc.sayda.creraces.util.PlatformServices;
import mc.sayda.creraces.world.inventory.micro.MicroAnvilMenu;
import mc.sayda.creraces.world.inventory.micro.MicroBrewingMenu;
import mc.sayda.creraces.world.inventory.micro.MicroCartographyMenu;
import mc.sayda.creraces.world.inventory.micro.MicroCraftingMenuProvider;
import mc.sayda.creraces.world.inventory.micro.MicroEnchantingMenu;
import mc.sayda.creraces.world.inventory.micro.MicroFurnaceMenuProvider;
import mc.sayda.creraces.world.inventory.micro.MicroGrindstoneMenu;
import mc.sayda.creraces.world.inventory.micro.MicroLoomMenu;
import mc.sayda.creraces.world.inventory.micro.MicroMenuUtils;
import mc.sayda.creraces.world.inventory.micro.MicroSmithingMenu;
import mc.sayda.creraces.world.inventory.micro.MicroStonecutterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores a 4x4x4 grid of mini BlockStates inside a single Minecraft block.
 * Index formula: x + 4*y + 16*z (x,y,z in 0..3)
 */
@SuppressWarnings("null")
public class MicroBlockEntity extends BlockEntity {

    public static final int SIZE = 4;
    public static final int TOTAL = SIZE * SIZE * SIZE; // 64

    // Index layout of a furnace slot's state array; the array is saved to NBT as-is
    private static final int BURN_TIME = 0;
    private static final int BURN_DURATION = 1;
    private static final int COOK_TIME = 2;
    private static final int COOK_TIME_TOTAL = 3;

    // Index layout of a brewing stand slot's state array; the array is saved to NBT as-is
    private static final int BREW_TIME = 0;
    private static final int BREW_FUEL = 1;
    private static final int BREW_DURATION = 400;
    private static final int BREWS_PER_BLAZE_POWDER = 20;

    private final NonNullList<BlockState> slots = NonNullList.withSize(TOTAL, Blocks.AIR.defaultBlockState());
    // Per-slot state of interactive sub-blocks, keyed by slot index
    private final Map<Integer, NonNullList<ItemStack>> inventories = new HashMap<>();
    private final Map<Integer, MicroInventory> inventoryHolders = new HashMap<>();
    private final Map<Integer, int[]> furnaceStates = new HashMap<>();
    private final Map<Integer, int[]> campfireProgress = new HashMap<>();
    private final Map<Integer, int[]> brewingStates = new HashMap<>(); // {brewTime, fuel}
    private final Map<Integer, RecipeManager.CachedCheck<SingleRecipeInput, AbstractCookingRecipe>> recipeCache = new HashMap<>();
    // occupiedCount is always modified on the server tick thread; plain int is safe here.
    private int occupiedCount = 0;

    // Slots that need ticking, rebuilt whenever the grid changes
    private boolean hasFurnaces = false;
    private final List<Integer> furnaceIndices = new ArrayList<>();
    private boolean hasCampfires = false;
    private final List<Integer> campfireIndices = new ArrayList<>();
    private boolean hasBrewingStands = false;
    private final List<Integer> brewingIndices = new ArrayList<>();

    private VoxelShape cachedOutlineShape = null;
    private VoxelShape cachedCollisionShape = null;
    private long renderVersion = 0;
    private long lastUseTime = -1;

    public MicroBlockEntity(BlockPos pos, BlockState state) {
        super(resolveEntityType(), pos, state);
    }

    /** Null-safe lookup of the block entity type, which may not be registered yet. */
    private static BlockEntityType<MicroBlockEntity> resolveEntityType() {
        var supplier = ModBlocks.MICRO_BLOCK_ENTITY;
        return supplier != null ? supplier.get() : null;
    }

    public MicroInventory getInventory(int slotIdx, int size) {
        return inventoryHolders.computeIfAbsent(slotIdx, k -> new MicroInventory(this, slotIdx, size));
    }

    public static int toIndex(int x, int y, int z) {
        return x + SIZE * y + SIZE * SIZE * z;
    }

    private static boolean inGrid(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    /** Maps a coordinate inside the host block (only its fractional part counts) to a 0..3 sub-slot. */
    public static int clampSlot(double frac) {
        double f = ((frac % 1.0) + 1.0) % 1.0;
        return Math.min((int) (f * 4), 3);
    }

    /** The bucket a liquid sub-slot scoops into, or null if the slot holds no liquid. */
    @Nullable
    public static Item filledBucketFor(BlockState slotState) {
        if (slotState.is(Blocks.WATER)) return Items.WATER_BUCKET;
        if (slotState.is(Blocks.LAVA)) return Items.LAVA_BUCKET;
        return null;
    }

    /** Index of the first slot holding a bed (in x + 4*y + 16*z order), or -1 if there is none. */
    public static int findBedSlot(MicroBlockEntity micro) {
        for (int i = 0; i < TOTAL; i++) {
            if (micro.slots.get(i).getBlock() instanceof BedBlock) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Where an entity should stand when getting up from, or respawning at, a bed in the given slot.
     * Shared by the sleep and respawn mixins so both place the entity identically.
     */
    public static Vec3 computeBedStandPosition(BlockPos hostPos, BlockState bedState, int x, int y, int z) {
        double scale = 1.0 / SIZE;
        Direction facing = bedState.getValue(BedBlock.FACING);
        BedPart part = bedState.getValue(BedBlock.PART);

        double subBlockX = (x * scale) + (scale / 2.0);
        double subBlockY = (y * scale);
        double subBlockZ = (z * scale) + (scale / 2.0);
        double bedPillowHeight = 0.6875 * scale;

        // Both halves land on the same spot, 1.175 slots from the head's centre towards the foot
        // (tuned by eye).
        if (part == BedPart.HEAD) {
            subBlockX += facing.getOpposite().getStepX() * (scale * 1.175);
            subBlockZ += facing.getOpposite().getStepZ() * (scale * 1.175);
        } else {
            subBlockX += facing.getOpposite().getStepX() * (scale * 0.175);
            subBlockZ += facing.getOpposite().getStepZ() * (scale * 0.175);
        }

        return new Vec3(
                hostPos.getX() + subBlockX,
                hostPos.getY() + subBlockY + bedPillowHeight,
                hostPos.getZ() + subBlockZ);
    }

    public BlockState getSlot(int x, int y, int z) {
        int idx = toIndex(x, y, z);
        if (idx < 0 || idx >= TOTAL)
            return Blocks.AIR.defaultBlockState();
        return slots.get(idx);
    }

    public void setSlot(int x, int y, int z, @Nullable BlockState state) {
        setSlot(x, y, z, state, true);
    }

    public void setSlot(int x, int y, int z, @Nullable BlockState state, boolean triggerUpdate) {
        int idx = toIndex(x, y, z);
        if (idx < 0 || idx >= TOTAL)
            return;

        BlockState prev = slots.get(idx);
        boolean prevAir = prev.isAir();
        boolean newAir = state == null || state.isAir();

        if (prevAir && !newAir) {
            occupiedCount++;
        } else if (!prevAir && newAir) {
            occupiedCount--;
        }
        if (!prevAir && (newAir || !prev.getBlock().equals(state.getBlock()))) {
            // The block in this slot changed, so its items spill out and its stored data no longer applies
            dropSlotInventory(idx);
            clearSlotData(idx);
        }

        slots.set(idx, newAir ? Blocks.AIR.defaultBlockState() : state);
        rescanTickingSlots();

        setChanged();
        invalidateShapes();
        if (level != null && triggerUpdate) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            if (!level.isClientSide()) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                syncHostLight();
                if (newAir && isEmpty()) {
                    level.removeBlock(worldPosition, false);
                }
            }
        }
    }

    private void dropSlotInventory(int idx) {
        if (level == null || level.isClientSide()) return;
        NonNullList<ItemStack> inv = inventories.remove(idx);
        if (inv != null) {
            for (ItemStack stack : inv) {
                if (!stack.isEmpty()) {
                    dropAtHost(stack);
                }
            }
        }
    }

    private void clearSlotData(int idx) {
        inventories.remove(idx);
        furnaceStates.remove(idx);
        campfireProgress.remove(idx);
        brewingStates.remove(idx);
        inventoryHolders.remove(idx);
    }

    private void rescanTickingSlots() {
        hasFurnaces = scanForBlockType(AbstractFurnaceBlock.class, furnaceIndices);
        recipeCache.clear();
        hasCampfires = scanForBlockType(CampfireBlock.class, campfireIndices);
        hasBrewingStands = scanForBlockType(BrewingStandBlock.class, brewingIndices);
    }

    private boolean scanForBlockType(Class<? extends Block> blockClass, List<Integer> indices) {
        indices.clear();
        for (int i = 0; i < TOTAL; i++) {
            if (blockClass.isInstance(slots.get(i).getBlock())) {
                indices.add(i);
            }
        }
        return !indices.isEmpty();
    }

    private void syncHostLight() {
        int newLight = getTotalLightLevel();
        BlockState hostState = getBlockState();
        if (hostState.hasProperty(MicroBlock.LIGHT) && hostState.getValue(MicroBlock.LIGHT) != newLight) {
            level.setBlock(worldPosition, hostState.setValue(MicroBlock.LIGHT, newLight), Block.UPDATE_ALL);
        }
    }

    private int getTotalLightLevel() {
        int lightSources = 0;
        for (BlockState state : slots) {
            Block block = state.getBlock();
            boolean isRedstoneTorch = block instanceof RedstoneTorchBlock || block instanceof RedstoneWallTorchBlock;
            boolean isLightSource = isRedstoneTorch || block instanceof TorchBlock
                    || block instanceof WallTorchBlock || block instanceof LanternBlock;
            if (!isLightSource) continue;
            // Redstone torches only count when lit
            if (isRedstoneTorch && state.hasProperty(BlockStateProperties.LIT)
                    && !state.getValue(BlockStateProperties.LIT)) {
                continue;
            }
            lightSources++;
        }
        return Math.min(lightSources * CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH.get(),
                CreRacesConfig.MICRO_BLOCK_MAX_LIGHT.get());
    }

    /** Reads a sub-slot by host-relative coordinates that may spill over into a neighbouring host. */
    public static BlockState getSlotGlobal(Level level, BlockPos host, int sx, int sy, int sz) {
        int mx = host.getX() * 4 + sx;
        int my = host.getY() * 4 + sy;
        int mz = host.getZ() * 4 + sz;
        BlockPos targetHost = new BlockPos(mx >> 2, my >> 2, mz >> 2);
        if (level.getBlockEntity(targetHost) instanceof MicroBlockEntity micro) {
            return micro.getSlot(mx & 3, my & 3, mz & 3);
        }
        BlockState hostState = level.getBlockState(targetHost);
        return hostState.isAir() || hostState.canBeReplaced() ? Blocks.AIR.defaultBlockState() : hostState;
    }

    /** Writes a sub-slot like getSlotGlobal reads one, creating the neighbouring host when needed. */
    public static void setSlotGlobal(Level level, BlockPos host, int sx, int sy, int sz, BlockState state) {
        int mx = host.getX() * 4 + sx;
        int my = host.getY() * 4 + sy;
        int mz = host.getZ() * 4 + sz;
        BlockPos targetHost = new BlockPos(mx >> 2, my >> 2, mz >> 2);

        BlockState hostState = level.getBlockState(targetHost);
        if (hostState.canBeReplaced() && !state.isAir()) {
            level.setBlockAndUpdate(targetHost, ModBlocks.MICRO_BLOCK.get().defaultBlockState());
        }

        if (level.getBlockEntity(targetHost) instanceof MicroBlockEntity micro) {
            micro.setSlot(mx & 3, my & 3, mz & 3, state);
            micro.updateConnections(mx & 3, my & 3, mz & 3);
        }
    }

    public void invalidateShapes() {
        this.cachedOutlineShape = null;
        this.cachedCollisionShape = null;
        this.renderVersion++;
    }

    public long getRenderVersion() {
        return renderVersion;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public VoxelShape getOrCreateShape(boolean isCollision, BlockGetter level) {
        if (isCollision) {
            if (cachedCollisionShape == null) {
                cachedCollisionShape = computeShape(true, level);
            }
            return cachedCollisionShape;
        }
        if (cachedOutlineShape == null) {
            cachedOutlineShape = computeShape(false, level);
        }
        return cachedOutlineShape;
    }

    private VoxelShape computeShape(boolean isCollision, BlockGetter level) {
        if (level == null) {
            return MicroBlock.BOX;
        }
        VoxelShape combined = Shapes.empty();
        float scale = 1f / SIZE;
        for (int z = 0; z < SIZE; z++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    BlockState slotState = getSlot(x, y, z);
                    if (slotState.isAir()) continue;
                    VoxelShape slotShape = isCollision
                            ? slotState.getCollisionShape(level, worldPosition)
                            : slotState.getShape(level, worldPosition);
                    if (!slotShape.isEmpty()) {
                        VoxelShape scaled = scaleShape(slotShape, scale).move(x * scale, y * scale, z * scale);
                        combined = Shapes.or(combined, scaled);
                    }
                }
            }
        }
        return combined.isEmpty() ? MicroBlock.BOX : combined.optimize();
    }

    private static VoxelShape scaleShape(VoxelShape shape, double scale) {
        final VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            VoxelShape box = Shapes.box(x1 * scale, y1 * scale, z1 * scale, x2 * scale, y2 * scale, z2 * scale);
            result[0] = Shapes.or(result[0], box);
        });
        return result[0];
    }

    /** Refreshes the connection state (fences, panes, walls...) of a slot and its six neighbours. */
    public void updateConnections(int x, int y, int z) {
        if (level == null)
            return;

        refreshSlot(x, y, z);

        for (Direction dir : Direction.values()) {
            int nx = x + dir.getStepX();
            int ny = y + dir.getStepY();
            int nz = z + dir.getStepZ();

            if (inGrid(nx, ny, nz)) {
                refreshSlot(nx, ny, nz);
            } else {
                // The neighbour lives in the adjacent host, if there is one
                BlockPos neighborPos = worldPosition.relative(dir);
                if (level.getBlockEntity(neighborPos) instanceof MicroBlockEntity other) {
                    other.refreshSlot((nx + SIZE) % SIZE, (ny + SIZE) % SIZE, (nz + SIZE) % SIZE);
                    level.sendBlockUpdated(neighborPos, other.getBlockState(), other.getBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Refreshes every slot on the grid's outer faces against the world around the host. */
    public void updateExternalNeighbors() {
        if (level == null)
            return;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                refreshSlot(i, 0, j); // Bottom
                refreshSlot(i, SIZE - 1, j); // Top
                refreshSlot(i, j, 0); // North
                refreshSlot(i, j, SIZE - 1); // South
                refreshSlot(0, i, j); // West
                refreshSlot(SIZE - 1, i, j); // East
            }
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private void refreshSlot(int x, int y, int z) {
        BlockState state = getSlot(x, y, z);
        if (state.isAir() || level == null)
            return;

        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            BlockState neighbor = getMicroNeighbor(x, y, z, dir);
            boolean internal = inGrid(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
            BlockPos neighborPos = internal ? worldPosition : worldPosition.relative(dir);
            BlockState updated = newState.updateShape(dir, neighbor, level, worldPosition, neighborPos);

            // Sub-blocks ignore survival rules: keep the old state rather than letting it turn to air
            if (updated.isAir()) {
                continue;
            }
            newState = updated;
        }

        if (newState != state) {
            slots.set(toIndex(x, y, z), newState);
            invalidateShapes();
        }
    }

    private BlockState getMicroNeighbor(int x, int y, int z, Direction dir) {
        int nx = x + dir.getStepX();
        int ny = y + dir.getStepY();
        int nz = z + dir.getStepZ();

        if (inGrid(nx, ny, nz)) {
            return getSlot(nx, ny, nz);
        }

        BlockPos neighborPos = worldPosition.relative(dir);
        if (level == null) {
            return Blocks.AIR.defaultBlockState();
        }
        if (level.getBlockState(neighborPos).is(ModBlocks.MICRO_BLOCK.get())
                && level.getBlockEntity(neighborPos) instanceof MicroBlockEntity otherMicro) {
            // Wrap around to the facing side of the adjacent host's grid
            return otherMicro.getSlot((nx + SIZE) % SIZE, (ny + SIZE) % SIZE, (nz + SIZE) % SIZE);
        }
        return level.getBlockState(neighborPos);
    }

    public NonNullList<ItemStack> getOrCreateInventory(int slotIdx, int size) {
        return inventories.computeIfAbsent(slotIdx, k -> NonNullList.withSize(size, ItemStack.EMPTY));
    }

    /** Furnace state for a slot: { burnTime, burnDuration, cookTime, cookTimeTotal }. */
    public int[] getOrCreateFurnaceState(int slotIdx) {
        return furnaceStates.computeIfAbsent(slotIdx, k -> new int[] { 0, 0, 0, 200 });
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, MicroBlockEntity entity) {
        if (entity.hasFurnaces && CreRacesConfig.MINI_FURNACE_ENABLED.get()) {
            entity.tickFurnaces(level);
        }
        if (entity.hasCampfires && CreRacesConfig.MINI_CAMPFIRE_ENABLED.get()) {
            entity.tickCampfires(level);
        }
        if (entity.hasBrewingStands && CreRacesConfig.MINI_BREWING_STAND_ENABLED.get()) {
            entity.tickBrewingStands(level);
        }
    }

    @SuppressWarnings("unchecked")
    private void tickFurnaces(Level level) {
        for (int i : furnaceIndices) {
            BlockState slotState = slots.get(i);
            if (!(slotState.getBlock() instanceof AbstractFurnaceBlock)) continue;

            boolean isBlast = slotState.getBlock() instanceof BlastFurnaceBlock;
            boolean isSmoker = slotState.getBlock() instanceof SmokerBlock;

            MicroInventory inv = getInventory(i, 3);
            int[] state = getOrCreateFurnaceState(i);
            ItemStack inputStack = inv.getItem(0);
            ItemStack fuelStack = inv.getItem(1);
            ItemStack outputStack = inv.getItem(2);
            boolean burning = state[BURN_TIME] > 0;

            if (!burning && inputStack.isEmpty()) {
                if (state[COOK_TIME] > 0) {
                    state[COOK_TIME] = 0;
                    setChanged();
                }
                continue;
            }

            RecipeType<AbstractCookingRecipe> recipeType = (RecipeType<AbstractCookingRecipe>) (isSmoker
                    ? RecipeType.SMOKING
                    : isBlast ? RecipeType.BLASTING : RecipeType.SMELTING);
            var cachedCheck = recipeCache.computeIfAbsent(i, k -> RecipeManager.createCheck(recipeType));
            var recipeOpt = cachedCheck.getRecipeFor(new SingleRecipeInput(inputStack), level);
            boolean hasRecipe = recipeOpt.isPresent();

            if (!burning && hasRecipe && !fuelStack.isEmpty()) {
                int fuelTime = PlatformServices.getBurnTime(fuelStack);
                // Blasting and smoking recipes already cook twice as fast; vanilla halves the fuel time instead
                if (isBlast || isSmoker) {
                    fuelTime /= 2;
                }
                if (fuelTime > 0) {
                    state[BURN_TIME] = fuelTime;
                    state[BURN_DURATION] = fuelTime;
                    consumeFuel(inv);
                    burning = true;
                    setChanged();
                }
            }

            if (burning) {
                state[BURN_TIME]--;
                if (state[BURN_TIME] <= 0) {
                    state[BURN_TIME] = 0;
                    burning = false;
                }
                setChanged();
            }

            if (hasRecipe && burning) {
                AbstractCookingRecipe recipe = recipeOpt.get().value();
                int newTotal = recipe.getCookingTime();
                if (state[COOK_TIME_TOTAL] != newTotal) {
                    state[COOK_TIME_TOTAL] = newTotal;
                    setChanged();
                }
                state[COOK_TIME]++;
                setChanged();

                if (state[COOK_TIME] >= state[COOK_TIME_TOTAL]) {
                    ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
                    if (outputStack.isEmpty()) {
                        inv.setItem(2, result);
                    } else if (ItemStack.isSameItemSameComponents(outputStack, result)
                            && outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                        outputStack.grow(result.getCount());
                        inv.setChanged();
                    } else {
                        // Output slot is full or holds something else: wait at the finish line
                        state[COOK_TIME]--;
                        continue;
                    }

                    if (!inputStack.isEmpty()) {
                        inv.removeItem(0, 1);
                    }
                    state[COOK_TIME] = 0;
                    setChanged();
                }
            } else if (state[COOK_TIME] > 0) {
                state[COOK_TIME] = 0;
                setChanged();
            }

            if (slotState.hasProperty(BlockStateProperties.LIT) && slotState.getValue(BlockStateProperties.LIT) != burning) {
                slots.set(i, slotState.setValue(BlockStateProperties.LIT, burning));
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Burns one fuel item, leaving its crafting remainder behind like vanilla (a lava bucket's empty bucket). */
    private static void consumeFuel(MicroInventory inv) {
        Item fuel = inv.getItem(1).getItem();
        inv.removeItem(1, 1);
        Item remainder = fuel.getCraftingRemainingItem();
        if (remainder != null && inv.getItem(1).isEmpty()) {
            inv.setItem(1, new ItemStack(remainder));
        }
    }

    private void tickCampfires(Level level) {
        for (int i : campfireIndices) {
            BlockState slotState = slots.get(i);
            if (!(slotState.getBlock() instanceof CampfireBlock) || !slotState.getValue(BlockStateProperties.LIT))
                continue;

            NonNullList<ItemStack> items = getOrCreateInventory(i, 4);
            int[] progress = campfireProgress.computeIfAbsent(i, k -> new int[4]);
            boolean changed = false;

            for (int slot = 0; slot < 4; slot++) {
                ItemStack item = items.get(slot);
                if (item.isEmpty()) {
                    progress[slot] = 0;
                    continue;
                }
                Optional<CampfireCookingRecipe> recipe = findCampfireRecipe(level, item);
                if (recipe.isEmpty()) continue;

                progress[slot]++;
                changed = true;
                if (progress[slot] >= recipe.get().getCookingTime()) {
                    ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
                    items.set(slot, ItemStack.EMPTY);
                    progress[slot] = 0;
                    ItemEntity drop = new ItemEntity(level, worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, result);
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
            }

            if (changed) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // Uses the raw item list and mirrors vanilla BrewingStandBlockEntity.doBrew
    private void tickBrewingStands(Level level) {
        for (int i : brewingIndices) {
            BlockState slotState = slots.get(i);
            if (!(slotState.getBlock() instanceof BrewingStandBlock)) continue;

            NonNullList<ItemStack> items = getOrCreateInventory(i, 5);
            int[] brewing = brewingStates.computeIfAbsent(i, k -> new int[] { 0, 0 });
            boolean changed = false;

            ItemStack fuelStack = items.get(4);
            if (brewing[BREW_FUEL] <= 0 && fuelStack.is(Items.BLAZE_POWDER)) {
                brewing[BREW_FUEL] = BREWS_PER_BLAZE_POWDER;
                fuelStack.shrink(1);
                if (fuelStack.isEmpty()) items.set(4, ItemStack.EMPTY);
                changed = true;
            }

            ItemStack ingredient = items.get(3);
            boolean brewable = isBrewable(level.potionBrewing(), items);
            if (brewing[BREW_TIME] > 0) {
                brewing[BREW_TIME]--;
                changed = true;
                if (brewing[BREW_TIME] == 0 && brewable) {
                    for (int slot = 0; slot < 3; slot++) {
                        items.set(slot, level.potionBrewing().mix(ingredient, items.get(slot)));
                    }
                    ingredient.shrink(1);
                    if (ingredient.isEmpty()) items.set(3, ItemStack.EMPTY);
                    level.levelEvent(LevelEvent.SOUND_BREWING_STAND_BREW, worldPosition, 0);
                } else if (!brewable) {
                    brewing[BREW_TIME] = 0;
                }
            } else if (brewing[BREW_FUEL] > 0 && brewable) {
                brewing[BREW_FUEL]--;
                brewing[BREW_TIME] = BREW_DURATION;
                changed = true;
            }

            if (changed) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Vanilla's brewable check: the ingredient has to turn at least one of the bottles into something. */
    private static boolean isBrewable(PotionBrewing potionBrewing, NonNullList<ItemStack> items) {
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty() || !potionBrewing.isIngredient(ingredient)) return false;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = items.get(slot);
            if (!bottle.isEmpty() && potionBrewing.hasMix(bottle, ingredient)) return true;
        }
        return false;
    }

    private static Optional<CampfireCookingRecipe> findCampfireRecipe(Level level, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    public boolean isEmpty() {
        return occupiedCount <= 0;
    }

    /**
     * Right-click on one sub-slot. Containers and workstations open their menu, a handful of blocks get a
     * hand-written copy of their vanilla behaviour, and anything else toggles OPEN, LIT or POWERED.
     */
    public InteractionResult handleSlotUse(Player player, InteractionHand hand, int sx, int sy, int sz) {
        if (level == null)
            return InteractionResult.PASS;

        if (level.isClientSide()) {
            // Predict the music stopping when a record is about to be ejected
            BlockState slotState = getSlot(sx, sy, sz);
            if (slotState.getBlock() instanceof JukeboxBlock && slotState.hasProperty(BlockStateProperties.HAS_RECORD)
                    && slotState.getValue(BlockStateProperties.HAS_RECORD)) {
                stopRecord();
            }
            return InteractionResult.SUCCESS;
        }

        BlockState slotState = getSlot(sx, sy, sz);
        ItemStack held = player.getItemInHand(hand);
        if (tryUseBucket(player, hand, held, slotState, sx, sy, sz)) {
            return InteractionResult.SUCCESS;
        }
        if (slotState.isAir())
            return InteractionResult.PASS;

        // Checks run most specific first: smithing and fletching tables are crafting tables too
        Block block = slotState.getBlock();
        int slotIdx = toIndex(sx, sy, sz);
        if (block instanceof FletchingTableBlock) {
            return InteractionResult.PASS; // no GUI in vanilla
        } else if (block instanceof SmithingTableBlock) {
            openMenu(player, (id, inv, p) -> new MicroSmithingMenu(id, inv, access()),
                    Component.translatable("container.upgrade"));
            return InteractionResult.CONSUME;
        } else if (block instanceof CraftingTableBlock) {
            if (player instanceof ServerPlayer sp) {
                MenuRegistry.openMenu(sp, new MicroCraftingMenuProvider(level, worldPosition));
            }
            return InteractionResult.CONSUME;
        } else if (block instanceof BarrelBlock || block instanceof ChestBlock) {
            openMenu(player, (id, inv, p) -> ChestMenu.threeRows(id, inv, new MicroInventory(this, slotIdx, 27)),
                    block.getName());
            return InteractionResult.CONSUME;
        } else if (block instanceof EnderChestBlock) {
            openMenu(player, (id, inv, p) -> ChestMenu.threeRows(id, inv, player.getEnderChestInventory()),
                    Component.translatable("container.enderchest"));
            return InteractionResult.CONSUME;
        } else if (block instanceof BedBlock) {
            if (player instanceof ISleepSlotTracker tracker) {
                tracker.creraces$setSleepSlot(slotIdx);
            }
            player.startSleepInBed(worldPosition);
            return InteractionResult.SUCCESS;
        } else if (block instanceof AbstractFurnaceBlock) {
            if (player instanceof ServerPlayer sp) {
                MenuRegistry.openMenu(sp, new MicroFurnaceMenuProvider(this, slotIdx, slotState));
            }
            return InteractionResult.CONSUME;
        } else if (block instanceof JukeboxBlock) {
            return useJukebox(slotState, player, held, sx, sy, sz);
        } else if (block instanceof AnvilBlock) {
            openMenu(player, (id, inv, p) -> new MicroAnvilMenu(id, inv, access()),
                    Component.translatable("container.repair"));
            return InteractionResult.CONSUME;
        } else if (block instanceof StonecutterBlock) {
            openMenu(player, (id, inv, p) -> new MicroStonecutterMenu(id, inv, access()),
                    Component.translatable("container.stonecutter"));
            return InteractionResult.CONSUME;
        } else if (block instanceof GrindstoneBlock) {
            openMenu(player, (id, inv, p) -> new MicroGrindstoneMenu(id, inv, access()),
                    Component.translatable("container.grindstone_title"));
            return InteractionResult.CONSUME;
        } else if (block instanceof EnchantingTableBlock) {
            openMenu(player, (id, inv, p) -> new MicroEnchantingMenu(id, inv, access()),
                    Component.translatable("container.enchant"));
            return InteractionResult.CONSUME;
        } else if (block instanceof LoomBlock) {
            openMenu(player, (id, inv, p) -> new MicroLoomMenu(id, inv, access()),
                    Component.translatable("container.loom"));
            return InteractionResult.CONSUME;
        } else if (block instanceof CartographyTableBlock) {
            openMenu(player, (id, inv, p) -> new MicroCartographyMenu(id, inv, access()),
                    Component.translatable("container.cartography_table"));
            return InteractionResult.CONSUME;
        } else if (block instanceof BrewingStandBlock) {
            ContainerData brewData = MicroMenuUtils.dataView(
                    brewingStates.computeIfAbsent(slotIdx, k -> new int[] { 0, 0 }));
            openMenu(player, (id, inv, p) -> new MicroBrewingMenu(id, inv, getInventory(slotIdx, 5), brewData, access()),
                    Component.translatable("container.brewing"));
            return InteractionResult.CONSUME;
        } else if (block == Blocks.LODESTONE) {
            return useLodestone(held);
        } else if (block instanceof FlowerPotBlock pot) {
            return useFlowerPot(pot, player, held, sx, sy, sz);
        } else if (block instanceof ComposterBlock) {
            return useComposter(slotState, player, held, sx, sy, sz);
        } else if (block instanceof CampfireBlock) {
            return useCampfire(slotState, player, held, sx, sy, sz);
        } else if (block instanceof NoteBlock) {
            return useNoteBlock(slotState, sx, sy, sz);
        } else if (block instanceof BellBlock) {
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 1.0f);
            return InteractionResult.SUCCESS;
        } else if (block instanceof RespawnAnchorBlock) {
            return useRespawnAnchor(slotState, player, held, sx, sy, sz);
        } else if (block instanceof LecternBlock) {
            return useLectern(slotState, player, held, sx, sy, sz);
        } else if (block instanceof ChiseledBookShelfBlock) {
            return useChiseledBookshelf(slotState, player, held, sx, sy, sz);
        } else if (block instanceof AbstractCauldronBlock) {
            return useCauldron(slotState, player, hand, held, sx, sy, sz);
        }

        return toggleGenericProperty(slotState, sx, sy, sz);
    }

    private ContainerLevelAccess access() {
        return ContainerLevelAccess.create(level, worldPosition);
    }

    private static void openMenu(Player player, MenuConstructor menu, Component title) {
        if (player instanceof ServerPlayer sp) {
            MenuRegistry.openMenu(sp, new SimpleMenuProvider(menu, title));
        }
    }

    /** Fills an empty slot from a water or lava bucket, or scoops a liquid slot into an empty bucket. */
    private boolean tryUseBucket(Player player, InteractionHand hand, ItemStack held, BlockState slotState,
            int sx, int sy, int sz) {
        if (slotState.isAir()) {
            BlockState liquid = held.is(Items.WATER_BUCKET) ? Blocks.WATER.defaultBlockState()
                    : held.is(Items.LAVA_BUCKET) ? Blocks.LAVA.defaultBlockState()
                    : null;
            if (liquid == null) return false;
            setSlot(sx, sy, sz, liquid);
            if (!player.getAbilities().instabuild)
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            setChanged();
            return true;
        }

        Item filled = filledBucketFor(slotState);
        if (filled == null || !held.is(Items.BUCKET)) return false;
        setSlot(sx, sy, sz, Blocks.AIR.defaultBlockState());
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            giveOrDropAtPlayer(player, new ItemStack(filled));
        }
        setChanged();
        return true;
    }

    private InteractionResult useJukebox(BlockState slotState, Player player, ItemStack held, int sx, int sy, int sz) {
        NonNullList<ItemStack> inv = getOrCreateInventory(toIndex(sx, sy, sz), 1);
        boolean tracksRecord = slotState.hasProperty(BlockStateProperties.HAS_RECORD);

        if (inv.get(0).isEmpty() && isRecord(held)) {
            inv.set(0, held.copy().split(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            playRecord(inv.get(0));
            if (tracksRecord) {
                setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.HAS_RECORD, true));
            }
            setChanged();
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!inv.get(0).isEmpty()) {
            ItemStack record = inv.get(0).copy();
            inv.set(0, ItemStack.EMPTY);
            stopRecord();
            if (tracksRecord) {
                setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.HAS_RECORD, false));
            }
            giveOrDropAtHost(player, record);
            setChanged();
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    private static boolean isRecord(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    private void playRecord(ItemStack record) {
        // The play event carries the song's registry id, not the record item's id
        JukeboxSong.fromStack(level.registryAccess(), record).ifPresent(song -> level.levelEvent(null,
                LevelEvent.SOUND_PLAY_JUKEBOX_SONG, worldPosition,
                level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(song.value())));
    }

    // Songs are keyed by the host position, so this stops whatever any jukebox in the grid plays
    private void stopRecord() {
        level.levelEvent(null, LevelEvent.SOUND_STOP_JUKEBOX_SONG, worldPosition, 0);
    }

    private InteractionResult useLodestone(ItemStack held) {
        if (!held.is(Items.COMPASS)) {
            return InteractionResult.PASS;
        }
        bindCompassToHost(held);
        level.playSound(null, worldPosition, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void bindCompassToHost(ItemStack compass) {
        compass.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), worldPosition)), true));
    }

    private InteractionResult useFlowerPot(FlowerPotBlock pot, Player player, ItemStack held, int sx, int sy, int sz) {
        Block plant = potContent(pot);
        if (plant != Blocks.AIR) {
            giveOrDropAtHost(player, new ItemStack(plant.asItem()));
            setSlot(sx, sy, sz, Blocks.FLOWER_POT.defaultBlockState());
            setChanged();
            return InteractionResult.SUCCESS;
        }

        if (!held.isEmpty() && held.getItem() instanceof BlockItem blockItem) {
            Optional<Block> pottedVariant = pottedVariantOf(blockItem.getBlock());
            if (pottedVariant.isPresent()) {
                setSlot(sx, sy, sz, pottedVariant.get().defaultBlockState());
                if (!player.getAbilities().instabuild) held.shrink(1);
                setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // Scans the registry instead of FlowerPotBlock.getFullPotsView(), which only exists on Forge
    private static Optional<Block> pottedVariantOf(Block plant) {
        return BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof FlowerPotBlock pot && potContent(pot) != Blocks.AIR && potContent(pot) == plant)
                .findFirst();
    }

    private static Block potContent(FlowerPotBlock pot) {
        return pot.getPotted();
    }

    private InteractionResult useComposter(BlockState slotState, Player player, ItemStack held, int sx, int sy, int sz) {
        int composterLevel = slotState.getValue(BlockStateProperties.LEVEL_COMPOSTER);
        if (composterLevel == 8) {
            giveOrDropAtHost(player, new ItemStack(Items.BONE_MEAL));
            setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0));
            setChanged();
            return InteractionResult.SUCCESS;
        }

        float chance = held.isEmpty() ? 0.0f : ComposterBlock.COMPOSTABLES.getFloat(held.getItem());
        if (chance <= 0.0f) {
            return InteractionResult.PASS;
        }
        if (level.getRandom().nextFloat() < chance) {
            setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.LEVEL_COMPOSTER, composterLevel + 1));
            setChanged();
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult useCampfire(BlockState slotState, Player player, ItemStack held, int sx, int sy, int sz) {
        boolean lit = slotState.getValue(BlockStateProperties.LIT);
        if (lit && !held.isEmpty() && findCampfireRecipe(level, held).isPresent()) {
            NonNullList<ItemStack> items = getOrCreateInventory(toIndex(sx, sy, sz), 4);
            for (int s = 0; s < 4; s++) {
                if (items.get(s).isEmpty()) {
                    items.set(s, held.copyWithCount(1));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setChanged();
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        // Not cookable or not lit: toggle the fire instead
        setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.LIT, !lit));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult useNoteBlock(BlockState slotState, int sx, int sy, int sz) {
        int newNote = (slotState.getValue(NoteBlock.NOTE) + 1) % 25;
        NoteBlockInstrument instrument = slotState.getValue(NoteBlock.INSTRUMENT);
        setSlot(sx, sy, sz, slotState.setValue(NoteBlock.NOTE, newNote));
        float pitch = (float) Math.pow(2.0, (newNote - 12) / 12.0);
        level.playSound(null, worldPosition, instrument.getSoundEvent().value(), SoundSource.RECORDS, 3.0f, pitch);
        setChanged();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult useRespawnAnchor(BlockState slotState, Player player, ItemStack held,
            int sx, int sy, int sz) {
        int charge = slotState.getValue(RespawnAnchorBlock.CHARGE);
        if (!held.is(Items.GLOWSTONE) || charge >= 4) {
            return InteractionResult.PASS;
        }
        setSlot(sx, sy, sz, slotState.setValue(RespawnAnchorBlock.CHARGE, charge + 1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0f, 1.0f);
        setChanged();
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private InteractionResult useLectern(BlockState slotState, Player player, ItemStack held, int sx, int sy, int sz) {
        NonNullList<ItemStack> books = getOrCreateInventory(toIndex(sx, sy, sz), 1);
        boolean hasBook = slotState.getValue(BlockStateProperties.HAS_BOOK);
        if (!hasBook && !held.isEmpty() && held.is(ItemTags.LECTERN_BOOKS)) {
            books.set(0, held.copy().split(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            setSlot(sx, sy, sz, slotState.setValue(BlockStateProperties.HAS_BOOK, true));
            level.playSound(null, worldPosition, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0f, 1.0f);
            setChanged();
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (hasBook && !books.get(0).isEmpty() && player instanceof ServerPlayer sp) {
            // A throwaway lectern entity supplies vanilla's reading menu
            LecternBlockEntity lectern = new LecternBlockEntity(worldPosition, slotState);
            lectern.setLevel(level);
            lectern.setBook(books.get(0));
            sp.openMenu(lectern);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult useChiseledBookshelf(BlockState slotState, Player player, ItemStack held,
            int sx, int sy, int sz) {
        NonNullList<ItemStack> books = getOrCreateInventory(toIndex(sx, sy, sz), 6);
        List<BooleanProperty> shelfSlots = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES;
        if (!held.isEmpty() && held.is(ItemTags.BOOKSHELF_BOOKS)) {
            for (int i = 0; i < 6; i++) {
                if (books.get(i).isEmpty()) {
                    books.set(i, held.copy().split(1));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    setSlot(sx, sy, sz, slotState.setValue(shelfSlots.get(i), true));
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT,
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                    setChanged();
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
            return InteractionResult.PASS;
        }
        if (held.isEmpty()) {
            for (int i = 5; i >= 0; i--) {
                if (!books.get(i).isEmpty()) {
                    ItemStack book = books.get(i).copy();
                    books.set(i, ItemStack.EMPTY);
                    giveOrDropAtPlayer(player, book);
                    setSlot(sx, sy, sz, slotState.setValue(shelfSlots.get(i), false));
                    level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_PICKUP,
                            SoundSource.BLOCKS, 1.0f, 1.0f);
                    setChanged();
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult useCauldron(BlockState slotState, Player player, InteractionHand hand, ItemStack held,
            int sx, int sy, int sz) {
        Block block = slotState.getBlock();
        if (block == Blocks.CAULDRON) {
            if (held.is(Items.WATER_BUCKET)) {
                setSlot(sx, sy, sz, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else if (held.is(Items.LAVA_BUCKET)) {
                setSlot(sx, sy, sz, Blocks.LAVA_CAULDRON.defaultBlockState());
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        } else if (block instanceof LayeredCauldronBlock) {
            if (held.is(Items.BUCKET)) {
                int waterLevel = slotState.getValue(LayeredCauldronBlock.LEVEL);
                BlockState next = waterLevel <= 1
                        ? Blocks.CAULDRON.defaultBlockState()
                        : slotState.setValue(LayeredCauldronBlock.LEVEL, waterLevel - 1);
                setSlot(sx, sy, sz, next);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    giveOrDropAtPlayer(player, new ItemStack(Items.WATER_BUCKET));
                }
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        } else if (block instanceof LavaCauldronBlock) {
            if (held.is(Items.BUCKET)) {
                setSlot(sx, sy, sz, Blocks.CAULDRON.defaultBlockState());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    giveOrDropAtPlayer(player, new ItemStack(Items.LAVA_BUCKET));
                }
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
                setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult toggleGenericProperty(BlockState slotState, int sx, int sy, int sz) {
        BlockState newState;
        if (slotState.hasProperty(BlockStateProperties.OPEN)) {
            newState = slotState.cycle(BlockStateProperties.OPEN);
        } else if (slotState.hasProperty(BlockStateProperties.LIT)) {
            newState = slotState.cycle(BlockStateProperties.LIT);
        } else if (slotState.hasProperty(BlockStateProperties.POWERED)) {
            newState = slotState.cycle(BlockStateProperties.POWERED);
        } else {
            return InteractionResult.PASS;
        }

        if (newState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            // Doors: mirror the change onto the other half
            DoubleBlockHalf half = newState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            int otherY = half == DoubleBlockHalf.LOWER ? sy + 1 : sy - 1;
            boolean otherInGrid = otherY >= 0 && otherY < SIZE;
            setSlot(sx, sy, sz, newState, false);
            if (otherInGrid) {
                BlockState otherState = getSlot(sx, otherY, sz);
                if (otherState.getBlock() == newState.getBlock()) {
                    BlockState updatedOther = otherState;
                    if (newState.hasProperty(BlockStateProperties.OPEN)) {
                        updatedOther = updatedOther.setValue(BlockStateProperties.OPEN,
                                newState.getValue(BlockStateProperties.OPEN));
                    }
                    if (newState.hasProperty(BlockStateProperties.POWERED)) {
                        updatedOther = updatedOther.setValue(BlockStateProperties.POWERED,
                                newState.getValue(BlockStateProperties.POWERED));
                    }
                    setSlot(sx, otherY, sz, updatedOther, false);
                }
            }
            updateConnections(sx, sy, sz);
            if (otherInGrid)
                updateConnections(sx, otherY, sz);
        } else {
            setSlot(sx, sy, sz, newState);
            updateConnections(sx, sy, sz);
        }

        if (newState.hasProperty(BlockStateProperties.OPEN)) {
            level.playSound(null, worldPosition,
                    newState.getValue(BlockStateProperties.OPEN)
                            ? SoundEvents.WOODEN_DOOR_OPEN
                            : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void giveOrDropAtPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void giveOrDropAtHost(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            dropAtHost(stack);
        }
    }

    private void dropAtHost(ItemStack stack) {
        ItemEntity drop = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, stack);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }

    /** Drops the contents of every sub-block container (barrels, furnaces, jukeboxes...). */
    public void dropAllInventories() {
        if (level == null || level.isClientSide())
            return;

        for (var entry : inventories.entrySet()) {
            if (slots.get(entry.getKey()).getBlock() instanceof JukeboxBlock) {
                stopRecord();
            }
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) {
                    dropAtHost(stack.copy());
                }
            }
        }
        inventories.clear();
        furnaceStates.clear();
        campfireProgress.clear();
        brewingStates.clear();
        inventoryHolders.clear();
    }

    /** Visits every non-air slot. */
    public void forEachOccupied(SlotConsumer consumer) {
        for (int z = 0; z < SIZE; z++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    BlockState s = getSlot(x, y, z);
                    if (!s.isAir())
                        consumer.accept(x, y, z, s);
                }
            }
        }
    }

    @FunctionalInterface
    public interface SlotConsumer {
        void accept(int x, int y, int z, BlockState state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < TOTAL; i++) {
            BlockState s = slots.get(i);
            if (!s.isAir()) {
                slotsTag.put("s" + i, NbtUtils.writeBlockState(s));
            }
        }
        tag.put("slots", slotsTag);
        tag.putInt("count", occupiedCount);

        CompoundTag invTag = new CompoundTag();
        for (var entry : inventories.entrySet()) {
            ListTag listTag = new ListTag();
            NonNullList<ItemStack> items = entry.getValue();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                if (!stack.isEmpty()) {
                    stack.save(registries, itemTag);
                }
                listTag.add(itemTag);
            }
            invTag.put("i" + entry.getKey(), listTag);
        }
        tag.put("inventories", invTag);

        tag.put("furnaceStates", writeIntArrays(furnaceStates, "f"));
        tag.put("campfireProgress", writeIntArrays(campfireProgress, "c"));
        tag.put("brewingStates", writeIntArrays(brewingStates, "b"));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        invalidateShapes();
        for (int i = 0; i < TOTAL; i++)
            slots.set(i, Blocks.AIR.defaultBlockState());
        occupiedCount = 0;

        CompoundTag slotsTag = tag.getCompound("slots");
        for (String key : slotsTag.getAllKeys()) {
            try {
                int idx = Integer.parseInt(key.substring(1)); // strip the "s" prefix
                BlockState slotState = readSlotState(slotsTag, key);
                if (slotState != null) {
                    slots.set(idx, slotState);
                    occupiedCount++;
                }
            } catch (Exception e) {
                CreRaces.LOGGER.warn("MicroBlockEntity: failed to load slot {}", key, e);
            }
        }
        rescanTickingSlots();
        // Keep a non-empty host alive even if none of its slots could be read
        if (occupiedCount == 0 && tag.contains("count")) {
            occupiedCount = tag.getInt("count");
        }

        inventories.clear();
        inventoryHolders.clear();
        CompoundTag invTag = tag.getCompound("inventories");
        for (String key : invTag.getAllKeys()) {
            int idx = Integer.parseInt(key.substring(1));
            ListTag listTag = invTag.getList(key, Tag.TAG_COMPOUND);
            int size = inventorySizeFor(slots.get(idx));
            if (size == 0)
                size = listTag.size();

            NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.contains("Slot") ? itemTag.getInt("Slot") : i;
                // Empty slots are saved as just their index, with no item id to parse.
                if (slot >= 0 && slot < size && itemTag.contains("id")) {
                    items.set(slot, ItemStack.parseOptional(registries, itemTag));
                }
            }
            inventories.put(idx, items);
        }

        readIntArrays(tag, "furnaceStates", furnaceStates);
        readIntArrays(tag, "campfireProgress", campfireProgress);
        readIntArrays(tag, "brewingStates", brewingStates);
    }

    /** Reads one saved slot: a full block state, or a bare block id from older saves. Null if it is air. */
    @Nullable
    private static BlockState readSlotState(CompoundTag slotsTag, String key) {
        if (slotsTag.contains(key, Tag.TAG_COMPOUND)) {
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), slotsTag.getCompound(key));
        }
        if (slotsTag.contains(key, Tag.TAG_STRING)) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(slotsTag.getString(key)));
            if (block != Blocks.AIR) {
                return block.defaultBlockState();
            }
        }
        return null;
    }

    /** Container size for a slot's saved inventory, or 0 to take it from the saved list. */
    private static int inventorySizeFor(BlockState slotState) {
        Block block = slotState.getBlock();
        if (block instanceof BarrelBlock) return 27;
        if (block instanceof AbstractFurnaceBlock) return 3;
        if (block instanceof CampfireBlock) return 4;
        if (block instanceof BrewingStandBlock) return 5;
        return 0;
    }

    private static CompoundTag writeIntArrays(Map<Integer, int[]> arrays, String keyPrefix) {
        CompoundTag tag = new CompoundTag();
        for (var entry : arrays.entrySet()) {
            tag.putIntArray(keyPrefix + entry.getKey(), entry.getValue());
        }
        return tag;
    }

    /** Inverse of writeIntArrays; keys are the one-letter prefix followed by the slot index. */
    private static void readIntArrays(CompoundTag tag, String key, Map<Integer, int[]> into) {
        into.clear();
        CompoundTag arraysTag = tag.getCompound(key);
        for (String arrayKey : arraysTag.getAllKeys()) {
            into.put(Integer.parseInt(arrayKey.substring(1)), arraysTag.getIntArray(arrayKey));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /** A Container view of one slot's inventory, backed live by the entity's map. */
    public static class MicroInventory implements Container {
        private final MicroBlockEntity micro;
        private final int slotIdx;
        private final int size;

        public MicroInventory(MicroBlockEntity micro, int slotIdx, int size) {
            this.micro = micro;
            this.slotIdx = slotIdx;
            this.size = size;
        }

        @Override
        public int getContainerSize() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return micro.getOrCreateInventory(slotIdx, size).stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int i) {
            return micro.getOrCreateInventory(slotIdx, size).get(i);
        }

        @Override
        public ItemStack removeItem(int i, int j) {
            ItemStack stack = ContainerHelper.removeItem(micro.getOrCreateInventory(slotIdx, size), i, j);
            if (!stack.isEmpty())
                setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack stack = ContainerHelper.takeItem(micro.getOrCreateInventory(slotIdx, size), i);
            setChanged();
            return stack;
        }

        @Override
        public void setItem(int i, ItemStack stack) {
            micro.getOrCreateInventory(slotIdx, size).set(i, stack);
            if (stack.getCount() > getMaxStackSize())
                stack.setCount(getMaxStackSize());
            setChanged();
        }

        @Override
        public void setChanged() {
            micro.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            micro.getOrCreateInventory(slotIdx, size).clear();
            setChanged();
        }
    }

}
