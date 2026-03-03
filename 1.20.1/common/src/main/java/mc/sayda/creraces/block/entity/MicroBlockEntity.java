package mc.sayda.creraces.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.Container;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import mc.sayda.creraces.util.PlatformServices;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores a 4x4x4 grid of mini BlockStates inside a single Minecraft block.
 * Index formula: x + 4*y + 16*z (x,y,z in 0..3)
 */
public class MicroBlockEntity extends BlockEntity {

    public static final int SIZE = 4;
    public static final int TOTAL = SIZE * SIZE * SIZE; // 64

    // ─── Interactive block inventories ───────────────────────────────────────────
    private final NonNullList<BlockState> slots = NonNullList.withSize(TOTAL, Blocks.AIR.defaultBlockState());
    private final Map<Integer, NonNullList<ItemStack>> inventories = new HashMap<>();
    private final Map<Integer, int[]> furnaceStates = new HashMap<>();
    // occupiedCount is always modified on the server tick thread; plain int is safe
    // here.
    private int occupiedCount = 0;
    private boolean hasFurnaces = false;
    private final java.util.List<Integer> furnaceIndices = new java.util.ArrayList<>();
    private final Map<Integer, RecipeManager.CachedCheck<Container, AbstractCookingRecipe>> recipeCache = new HashMap<>();

    // ─── Shape Caching ──────────────────────────────────────────────────────────
    private VoxelShape cachedOutlineShape = null;
    private VoxelShape cachedCollisionShape = null;
    private long renderVersion = 0;
    private long lastUseTime = -1;

    private final Map<Integer, MicroInventory> inventoryHolders = new HashMap<>();

    public net.minecraft.world.Container getInventory(int slotIdx, int size) {
        return inventoryHolders.computeIfAbsent(slotIdx, k -> new MicroInventory(this, slotIdx, size));
    }

    public MicroBlockEntity(BlockPos pos, BlockState state) {
        super(resolveEntityType(), pos, state);
    }

    /** Safely retrieves the block entity type; returns null before registration. */
    private static BlockEntityType<MicroBlockEntity> resolveEntityType() {
        var supplier = mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK_ENTITY;
        return supplier != null ? supplier.get() : null;
    }

    // ─── Index helpers ───────────────────────────────────────────────────────────

    public static int toIndex(int x, int y, int z) {
        return x + SIZE * y + SIZE * SIZE * z;
    }

    /**
     * Robustly converts a world coordinate fraction (0.0 to 1.0) to a 0..3 grid
     * slot.
     */
    public static int clampSlot(double frac) {
        double f = ((frac % 1.0) + 1.0) % 1.0;
        return Math.min((int) (f * 4), 3);
    }

    // ─── Slot accessors ──────────────────────────────────────────────────────────

    public BlockState getSlot(int x, int y, int z) {
        int idx = toIndex(x, y, z);
        if (idx < 0 || idx >= TOTAL)
            return Blocks.AIR.defaultBlockState();
        BlockState s = slots.get(idx);
        return s != null ? s : Blocks.AIR.defaultBlockState();
    }

    public void setSlot(int x, int y, int z, @Nullable BlockState state) {
        setSlot(x, y, z, state, true);
    }

    public void setSlot(int x, int y, int z, @Nullable BlockState state, boolean triggerUpdate) {
        int idx = toIndex(x, y, z);
        if (idx < 0 || idx >= TOTAL)
            return;

        BlockState prev = slots.get(idx);
        boolean prevAir = prev == null || prev.isAir();
        boolean newAir = state == null || state.isAir();

        if (prevAir && !newAir)
            occupiedCount++;
        else if (!prevAir && newAir) {
            occupiedCount--;
            // Drop items from any inventory stored in this slot
            if (inventories.containsKey(idx) && level != null && !level.isClientSide()) {
                NonNullList<ItemStack> inv = inventories.remove(idx);
                if (inv != null) {
                    for (ItemStack stack : inv) {
                        if (!stack.isEmpty()) {
                            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                                    level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                                    worldPosition.getZ() + 0.5, stack);
                            ie.setDefaultPickUpDelay();
                            level.addFreshEntity(ie);
                        }
                    }
                }
            }
            furnaceStates.remove(idx);
            inventoryHolders.remove(idx);
        }

        if (prev != null && !prevAir && (newAir || !prev.getBlock().equals(state.getBlock()))) {
            // Block type changed or removed — clear stale data
            inventories.remove(idx);
            furnaceStates.remove(idx);
            inventoryHolders.remove(idx);
        }

        slots.set(idx, newAir || state == null ? Blocks.AIR.defaultBlockState() : state);

        // Recalculate hasFurnaces and furnaceIndices
        hasFurnaces = false;
        furnaceIndices.clear();
        recipeCache.clear();
        for (int i = 0; i < TOTAL; i++) {
            BlockState s = slots.get(i);
            if (s != null && !s.isAir() && s.getBlock() instanceof AbstractFurnaceBlock) {
                hasFurnaces = true;
                furnaceIndices.add(i);
            }
        }

        setChanged();
        invalidateShapes();
        if (level != null && triggerUpdate) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (!level.isClientSide()) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                // Sync light level to host block
                int newLight = getTotalLightLevel();
                BlockState hostState = getBlockState();
                if (hostState.hasProperty(mc.sayda.creraces.block.MicroBlock.LIGHT)
                        && hostState.getValue(mc.sayda.creraces.block.MicroBlock.LIGHT) != newLight) {
                    level.setBlock(worldPosition,
                            hostState.setValue(mc.sayda.creraces.block.MicroBlock.LIGHT, newLight),
                            3);
                }
            }
        }
    }

    public int getTotalLightLevel() {
        int torchCount = 0;
        for (BlockState state : slots) {
            if (state == null || state.isAir())
                continue;
            Block block = state.getBlock();
            if (block instanceof net.minecraft.world.level.block.TorchBlock ||
                    block instanceof net.minecraft.world.level.block.WallTorchBlock ||
                    block instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
                    block instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
                // Check if redstone torch is actually lit
                if (block instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
                        block instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
                    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) &&
                            !state.getValue(
                                    net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                        continue;
                    }
                }
                torchCount++;
            }
        }
        return Math.min(torchCount * 5, 15);
    }

    public static BlockState getSlotGlobal(net.minecraft.world.level.Level level, BlockPos host, int sx, int sy,
            int sz) {
        int mx = host.getX() * 4 + sx;
        int my = host.getY() * 4 + sy;
        int mz = host.getZ() * 4 + sz;
        BlockPos targetHost = new BlockPos(mx >> 2, my >> 2, mz >> 2);
        if (level.getBlockEntity(targetHost) instanceof MicroBlockEntity micro) {
            return micro.getSlot(mx & 3, my & 3, mz & 3);
        }
        return Blocks.AIR.defaultBlockState();
    }

    public static void setSlotGlobal(net.minecraft.world.level.Level level, BlockPos host, int sx, int sy, int sz,
            BlockState state) {
        int mx = host.getX() * 4 + sx;
        int my = host.getY() * 4 + sy;
        int mz = host.getZ() * 4 + sz;
        BlockPos targetHost = new BlockPos(mx >> 2, my >> 2, mz >> 2);

        BlockState hostState = level.getBlockState(targetHost);
        if (hostState.canBeReplaced() && !state.isAir()) {
            level.setBlockAndUpdate(targetHost,
                    mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get().defaultBlockState());
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

    public VoxelShape getOrCreateShape(boolean isCollision, net.minecraft.world.level.BlockGetter level) {
        if (isCollision) {
            if (cachedCollisionShape == null) {
                cachedCollisionShape = computeShape(true, level);
            }
            return cachedCollisionShape;
        } else {
            if (cachedOutlineShape == null) {
                cachedOutlineShape = computeShape(false, level);
            }
            return cachedOutlineShape;
        }
    }

    private VoxelShape computeShape(boolean isCollision, net.minecraft.world.level.BlockGetter level) {
        VoxelShape combined = Shapes.empty();
        float scale = 1f / SIZE;
        for (int z = 0; z < SIZE; z++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    BlockState slotState = getSlot(x, y, z);
                    if (!slotState.isAir()) {
                        // Use the appropriate shape logic but scaled
                        if (level != null && worldPosition != null) {
                            VoxelShape slotShape = isCollision ? slotState.getCollisionShape(level, worldPosition)
                                    : slotState.getShape(level, worldPosition);

                            if (!slotShape.isEmpty()) {
                                VoxelShape scaled = scaleShape(slotShape, scale).move(x * scale, y * scale, z * scale);
                                combined = Shapes.or(combined, scaled);
                            }
                        }
                    }
                }
            }
        }
        return combined.optimize();
    }

    private static VoxelShape scaleShape(VoxelShape shape, double scale) {
        final VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            VoxelShape box = Shapes.box(x1 * scale, y1 * scale, z1 * scale, x2 * scale, y2 * scale, z2 * scale);
            result[0] = Shapes.or(result[0], box);
        });
        return result[0];
    }

    /** Triggers shape updates for the given slot and its micro-neighbors. */
    public void updateConnections(int x, int y, int z) {
        if (level == null)
            return;

        // Update the slot itself first to reflect neighbor changes
        refreshSlot(x, y, z);

        // Update 6 cardinal neighbors
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            int nx = x + dir.getStepX();
            int ny = y + dir.getStepY();
            int nz = z + dir.getStepZ();

            if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && nz >= 0 && nz < SIZE) {
                // Internal neighbor
                refreshSlot(nx, ny, nz);
            } else {
                // Boundary neighbor: check adjacent MicroBlock host
                BlockPos neighborPos = worldPosition.relative(dir);
                if (level.getBlockEntity(neighborPos) instanceof MicroBlockEntity other) {
                    int ox = (nx + SIZE) % SIZE;
                    int oy = (ny + SIZE) % SIZE;
                    int oz = (nz + SIZE) % SIZE;
                    other.refreshSlot(ox, oy, oz);
                    // Also trigger a re-render for the other host
                    level.sendBlockUpdated(neighborPos, other.getBlockState(), other.getBlockState(), 3);
                }
            }
        }
    }

    /** Refreshes all edge slots against world neighbors. */
    public void updateExternalNeighbors() {
        if (level == null)
            return;
        // Update all 6 faces' exterior slots
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
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void refreshSlot(int x, int y, int z) {
        BlockState state = getSlot(x, y, z);
        if (state.isAir())
            return;

        BlockState newState = state;

        // Survival check for wall-mounted/support-dependent blocks
        if (!canMicroSurvive(state, x, y, z)) {
            setSlot(x, y, z, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            // Trigger neighbor updates to allow neighbors to pop too if they depended on
            // this
            updateConnections(x, y, z);

            // Spawn item drop
            net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(state.getBlock().asItem());
            BlockPos currentPos = worldPosition;
            if (!drop.isEmpty() && level != null && !level.isClientSide() && currentPos != null) {
                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5,
                        drop);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
            return;
        }

        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockState neighbor = getMicroNeighbor(x, y, z, dir);
            if (level != null) {
                BlockPos neighborPos = isInternalNeighbor(x, y, z, dir) ? worldPosition : worldPosition.relative(dir);
                BlockState updated = newState.updateShape(dir, neighbor, level, worldPosition, neighborPos);

                // If the block wants to delete itself (turn to air) but it wasn't air before,
                // and we are ignoring survival rules, then we keep the original state.
                if (updated.isAir() && !newState.isAir()) {
                    continue;
                }

                newState = updated;
            }
        }

        if (newState != state) {
            slots.set(toIndex(x, y, z), newState);
            this.cachedCollisionShape = null;
            this.cachedOutlineShape = null;
            this.renderVersion++;
        }
    }

    private boolean canMicroSurvive(BlockState state, int x, int y, int z) {
        return true; // Ignore all placement/survival rules
    }

    private boolean isInternalNeighbor(int x, int y, int z, net.minecraft.core.Direction dir) {
        int nx = x + dir.getStepX();
        int ny = y + dir.getStepY();
        int nz = z + dir.getStepZ();
        return nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && nz >= 0 && nz < SIZE;
    }

    private BlockState getMicroNeighbor(int x, int y, int z, net.minecraft.core.Direction dir) {
        int nx = x + dir.getStepX();
        int ny = y + dir.getStepY();
        int nz = z + dir.getStepZ();

        if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE && nz >= 0 && nz < SIZE) {
            return getSlot(nx, ny, nz);
        }

        // Fallback: check real world neighbors
        BlockPos neighborPos = worldPosition.relative(dir);
        if (level != null
                && level.getBlockState(neighborPos).is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
            if (level.getBlockEntity(neighborPos) instanceof MicroBlockEntity otherMicro) {
                // Wrap around to the other side of the adjacent micro-block
                int ox = (nx + SIZE) % SIZE;
                int oy = (ny + SIZE) % SIZE;
                int oz = (nz + SIZE) % SIZE;
                return otherMicro.getSlot(ox, oy, oz);
            }
        }

        return level != null ? level.getBlockState(neighborPos) : Blocks.AIR.defaultBlockState();
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    // ─── Interactive block inventory accessors
    // ────────────────────────────────────

    /**
     * Gets or creates the item inventory for the given slot index with the
     * specified size.
     */
    public NonNullList<ItemStack> getOrCreateInventory(int slotIdx, int size) {
        return inventories.computeIfAbsent(slotIdx,
                k -> NonNullList.withSize(size, ItemStack.EMPTY));
    }

    /**
     * Gets or creates furnace state for the given slot index: { burnTime,
     * totalBurnTime, cookTime, cookTimeTotal }.
     */
    public int[] getOrCreateFurnaceState(int slotIdx) {
        return furnaceStates.computeIfAbsent(slotIdx, k -> new int[] { 0, 0, 0, 200 });
    }

    // ─── Furnace tick
    // ─────────────────────────────────────────────────────────────

    /** Called each server tick. Only does work if hasFurnaces flag is set. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
            BlockState blockState, MicroBlockEntity entity) {
        if (!entity.hasFurnaces || !mc.sayda.creraces.config.CreRacesConfig.MINI_FURNACE_ENABLED.get())
            return;

        for (int i : entity.furnaceIndices) {
            BlockState slotState = entity.slots.get(i);
            if (slotState == null || slotState.isAir())
                continue;
            if (!(slotState.getBlock() instanceof AbstractFurnaceBlock))
                continue;

            // Determine recipe type and speed multiplier
            boolean isBlast = slotState.getBlock() instanceof net.minecraft.world.level.block.BlastFurnaceBlock;
            boolean isSmoker = slotState.getBlock() instanceof net.minecraft.world.level.block.SmokerBlock;
            int speedMult = isBlast ? 2 : 1;

            // Use the wrapper for direct interaction
            var inv = (MicroInventory) entity.getInventory(i, 3);
            int[] state = entity.getOrCreateFurnaceState(i);

            ItemStack inputStack = inv.getItem(0);
            ItemStack fuelStack = inv.getItem(1);
            ItemStack outputStack = inv.getItem(2);

            boolean burning = state[0] > 0; // burnTime > 0

            // PERFORMANCE: Early exit if nothing to do
            if (!burning && inputStack.isEmpty()) {
                if (state[2] > 0) { // cookTime > 0
                    state[2] = 0;
                    entity.setChanged();
                }
                continue;
            }

            // Determine recipe type
            net.minecraft.world.item.crafting.RecipeType<AbstractCookingRecipe> recipeType = (net.minecraft.world.item.crafting.RecipeType<AbstractCookingRecipe>) (isSmoker
                    ? RecipeType.SMOKING
                    : isBlast ? RecipeType.BLASTING : RecipeType.SMELTING);

            // Get or create cached check
            var cachedCheck = entity.recipeCache.computeIfAbsent(i,
                    k -> RecipeManager.createCheck(recipeType));

            var recipeOpt = cachedCheck.getRecipeFor(inv, level);

            boolean hasRecipe = recipeOpt.isPresent();

            // Consume fuel if needed
            if (!burning && hasRecipe && !fuelStack.isEmpty()) {
                int fuelTime = PlatformServices.getBurnTime(fuelStack);
                if (fuelTime > 0) {
                    state[0] = fuelTime;
                    state[1] = fuelTime;
                    inv.removeItem(1, 1); // Shrink fuel
                    burning = true;
                    entity.setChanged();
                }
            }

            // Advance timers
            if (burning) {
                state[0] -= speedMult;
                if (state[0] <= 0) {
                    state[0] = 0;
                    burning = false; // Fuel ran out
                }
                entity.setChanged();
            }

            if (hasRecipe && burning) {
                AbstractCookingRecipe recipe = recipeOpt.get();
                int newTotal = recipe.getCookingTime();
                if (state[3] != newTotal) {
                    state[3] = newTotal;
                    entity.setChanged();
                }
                state[2] += speedMult;
                entity.setChanged();

                if (state[2] >= state[3]) {
                    // Produce output
                    ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
                    if (outputStack.isEmpty()) {
                        inv.setItem(2, result);
                    } else if (ItemStack.isSameItemSameTags(outputStack, result)
                            && outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                        outputStack.grow(result.getCount());
                        inv.setChanged();
                    } else {
                        // Output full or mismatch - stop cooking
                        state[2] -= speedMult;
                        continue;
                    }

                    if (!inputStack.isEmpty()) {
                        inv.removeItem(0, 1); // Shrink input
                    }
                    state[2] = 0;
                    entity.setChanged();
                }
            } else if (state[2] > 0) {
                state[2] = 0; // reset cook if no recipe or stopped burning
                entity.setChanged();
            }

            // Update lit state visually if it changed (moved to end of tick for accuracy)
            if (slotState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                net.minecraft.world.level.block.state.properties.BooleanProperty litProp = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
                boolean shouldBeLit = burning || state[0] > 0; // Lit if actively burning OR has remaining burnTime
                if (slotState.getValue(litProp) != shouldBeLit) {
                    entity.slots.set(i, slotState.setValue(litProp, shouldBeLit));
                    entity.setChanged();
                    BlockPos currentPos = entity.worldPosition;
                    BlockState hostState = entity.getBlockState();
                    if (currentPos != null && hostState != null) {
                        level.sendBlockUpdated(currentPos, hostState, hostState, 3);
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        return occupiedCount <= 0;
    }

    /**
     * Unified interaction handler for micro-block slots.
     * Handles specialized types (Jukebox, Chests, etc.) and generic properties
     * (OPEN, LIT).
     */
    @SuppressWarnings("null")
    public net.minecraft.world.InteractionResult handleSlotUse(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, int sx, int sy, int sz) {
        if (level == null)
            return net.minecraft.world.InteractionResult.PASS;

        if (level.isClientSide()) {
            BlockState slotState = getSlot(sx, sy, sz);
            if (slotState.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
                if (slotState.hasProperty(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD) &&
                        slotState.getValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
                    level.levelEvent(null, 1010, worldPosition, 0);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        BlockState slotState = getSlot(sx, sy, sz);
        if (slotState.isAir())
            return net.minecraft.world.InteractionResult.PASS;

        // 1. Custom Interactive Types (Crafting Table, Barrel, Furnace, Jukebox)
        if (slotState.getBlock() instanceof net.minecraft.world.level.block.CraftingTableBlock) {
            player.openMenu(
                    new mc.sayda.creraces.world.inventory.micro.MicroCraftingMenuProvider(level, worldPosition));
            return net.minecraft.world.InteractionResult.CONSUME;

        } else if (slotState.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock ||
                slotState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
            int slotIdx = toIndex(sx, sy, sz);
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (containerId, playerInventory, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(containerId,
                            playerInventory, new MicroInventory(this, slotIdx, 27)),
                    slotState.getBlock().getName()));
            return net.minecraft.world.InteractionResult.CONSUME;

        } else if (slotState.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            if (level.isClientSide())
                return net.minecraft.world.InteractionResult.SUCCESS;
            if (player instanceof mc.sayda.creraces.util.ISleepSlotTracker tracker) {
                tracker.creraces$setSleepSlot(toIndex(sx, sy, sz));
            }
            player.startSleepInBed(worldPosition);
            return net.minecraft.world.InteractionResult.SUCCESS;

        } else if (slotState.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock) {
            int slotIdx = toIndex(sx, sy, sz);
            player.openMenu(
                    new mc.sayda.creraces.world.inventory.micro.MicroFurnaceMenuProvider(this, slotIdx, slotState));
            return net.minecraft.world.InteractionResult.CONSUME;

        } else if (slotState.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
            // Client-side prediction for music stop
            if (level.isClientSide()) {
                if (slotState.hasProperty(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD) &&
                        slotState.getValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
                    level.levelEvent(null, 1010, getBlockPos(), 0);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            int slotIdx = toIndex(sx, sy, sz);
            NonNullList<ItemStack> inv = getOrCreateInventory(slotIdx, 1);
            ItemStack held = player.getItemInHand(hand);

            if (inv.get(0).isEmpty() && held.getItem() instanceof net.minecraft.world.item.RecordItem) {
                // Insert record
                inv.set(0, held.copy().split(1));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.levelEvent(null, 1010, getBlockPos(), net.minecraft.world.item.Item.getId(inv.get(0).getItem()));

                if (slotState
                        .hasProperty(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
                    setSlot(sx, sy, sz, slotState.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD, true));
                }
                setChanged();
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());

            } else if (!inv.get(0).isEmpty()) {
                // Eject record
                ItemStack record = inv.get(0).copy();
                inv.set(0, ItemStack.EMPTY);
                level.levelEvent(null, 1010, getBlockPos(), 0);

                if (slotState
                        .hasProperty(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
                    setSlot(sx, sy, sz, slotState.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD, false));
                }

                // Try to add to inventory first, then drop
                if (!player.getInventory().add(record)) {
                    net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                            level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            record);
                    ie.setDefaultPickUpDelay();
                    level.addFreshEntity(ie);
                }

                setChanged();
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
            }
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 2. Generic Interaction (Toggle OPEN, LIT, POWERED)
        BlockState newState = slotState;
        boolean changed = false;

        if (newState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
            newState = newState.cycle(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);
            changed = true;
        } else if (newState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
            newState = newState.cycle(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
            changed = true;
        } else if (newState
                .hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
            newState = newState.cycle(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
            changed = true;
        }

        if (changed) {
            if (newState.hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                // Multi-part (Door/Bed) sync
                var half = newState.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF);
                int otherY = (half == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) ? sy + 1
                        : sy - 1;
                setSlot(sx, sy, sz, newState, false);
                if (otherY >= 0 && otherY < SIZE) {
                    BlockState otherState = getSlot(sx, otherY, sz);
                    if (otherState.getBlock() == newState.getBlock()) {
                        BlockState updatedOther = otherState;
                        if (newState.hasProperty(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                            updatedOther = updatedOther.setValue(
                                    net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN,
                                    newState.getValue(
                                            net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN));
                        }
                        if (newState.hasProperty(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                            updatedOther = updatedOther.setValue(
                                    net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED,
                                    newState.getValue(
                                            net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED));
                        }
                        setSlot(sx, otherY, sz, updatedOther, false);
                    }
                }
                updateConnections(sx, sy, sz);
                if (otherY >= 0 && otherY < SIZE)
                    updateConnections(sx, otherY, sz);
            } else {
                setSlot(sx, sy, sz, newState);
                updateConnections(sx, sy, sz);
            }

            // Play sound/effect indicators
            if (newState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                level.playSound(null, worldPosition,
                        newState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                                ? net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN
                                : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            }

            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
        }

        return net.minecraft.world.InteractionResult.PASS;
    }

    /**
     * Drops all items from all internal inventories (Barrels, Furnaces, Jukeboxes).
     */
    public void dropAllInventories() {
        if (level == null || level.isClientSide())
            return;

        for (var entry : inventories.entrySet()) {
            NonNullList<ItemStack> inv = entry.getValue();
            if (inv != null) {
                // If this slot was a jukebox, stop the music
                BlockState s = slots.get(entry.getKey());
                if (s != null && s.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
                    level.levelEvent(null, 1010, worldPosition, 0);
                }

                for (ItemStack stack : inv) {
                    if (!stack.isEmpty()) {
                        net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                                level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                                worldPosition.getZ() + 0.5, stack.copy());
                        ie.setDefaultPickUpDelay();
                        level.addFreshEntity(ie);
                    }
                }
            }
        }
        inventories.clear();
        furnaceStates.clear();
        inventoryHolders.clear();
    }

    /** Iterate over all non-air slots. */
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

    // ─── NBT ─────────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag slotsTag = new CompoundTag();
        for (int i = 0; i < TOTAL; i++) {
            BlockState s = slots.get(i);
            if (s != null && !s.isAir()) {
                slotsTag.put("s" + i, net.minecraft.nbt.NbtUtils.writeBlockState(s));
            }
        }
        tag.put("slots", slotsTag);
        tag.putInt("count", occupiedCount);

        // Save inventories
        CompoundTag invTag = new CompoundTag();
        for (var entry : inventories.entrySet()) {
            ListTag listTag = new ListTag();
            NonNullList<ItemStack> items = entry.getValue();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                if (!stack.isEmpty()) {
                    stack.save(itemTag);
                }
                listTag.add(itemTag);
            }
            invTag.put("i" + entry.getKey(), listTag);
        }
        tag.put("inventories", invTag);

        // Save furnace states
        CompoundTag fsTag = new CompoundTag();
        for (var entry : furnaceStates.entrySet()) {
            fsTag.putIntArray("f" + entry.getKey(), entry.getValue());
        }
        tag.put("furnaceStates", fsTag);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        invalidateShapes();
        for (int i = 0; i < TOTAL; i++)
            slots.set(i, Blocks.AIR.defaultBlockState());
        occupiedCount = 0;
        hasFurnaces = false;
        furnaceIndices.clear();
        recipeCache.clear();

        if (tag.contains("slots")) {
            CompoundTag slotsTag = tag.getCompound("slots");
            for (String key : slotsTag.getAllKeys()) {
                try {
                    int idx = Integer.parseInt(key.substring(1)); // strip "s" prefix
                    if (slotsTag.contains(key, 10)) { // is compound (BlockState)
                        BlockState sideState = net.minecraft.nbt.NbtUtils.readBlockState(
                                BuiltInRegistries.BLOCK.asLookup(),
                                slotsTag.getCompound(key));
                        slots.set(idx, sideState);
                        occupiedCount++;
                        if (sideState.getBlock() instanceof AbstractFurnaceBlock) {
                            hasFurnaces = true;
                            furnaceIndices.add(idx);
                        }
                    } else if (slotsTag.contains(key, 8)) { // is string (Legacy ID)
                        ResourceLocation id = new ResourceLocation(slotsTag.getString(key));
                        net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(id);
                        if (block != null && block != Blocks.AIR) {
                            slots.set(idx, block.defaultBlockState());
                            occupiedCount++;
                            if (block instanceof AbstractFurnaceBlock)
                                hasFurnaces = true;
                        }
                    }
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("MicroBlockEntity: failed to load slot {}", key);
                }
            }
        }
        // fallback count from stored value
        if (occupiedCount == 0 && tag.contains("count")) {
            occupiedCount = tag.getInt("count");
        }

        // Load inventories
        inventories.clear();
        inventoryHolders.clear();
        if (tag.contains("inventories")) {
            CompoundTag invTag = tag.getCompound("inventories");
            for (String key : invTag.getAllKeys()) {
                int idx = Integer.parseInt(key.substring(1));
                ListTag listTag = invTag.getList(key, Tag.TAG_COMPOUND);

                // Determine size from block type if possible
                int size = 0;
                BlockState s = slots.get(idx);
                if (s != null) {
                    if (s.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock)
                        size = 27;
                    else if (s.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock)
                        size = 3;
                }
                if (size == 0)
                    size = listTag.size();

                NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);

                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag itemTag = listTag.getCompound(i);
                    int slot = itemTag.contains("Slot") ? itemTag.getInt("Slot") : i;
                    if (slot >= 0 && slot < size) {
                        items.set(slot, ItemStack.of(itemTag));
                    }
                }
                inventories.put(idx, items);
            }
        }

        // Load furnace states
        furnaceStates.clear();
        if (tag.contains("furnaceStates")) {
            CompoundTag fsTag = tag.getCompound("furnaceStates");
            for (String key : fsTag.getAllKeys()) {
                int idx = Integer.parseInt(key.substring(1));
                furnaceStates.put(idx, fsTag.getIntArray(key));
            }
        }
    }

    /** Full sync tag sent to client. */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    // ─── Inventory Wrapper
    // ────────────────────────────────────────────────────────

    public static class MicroInventory implements net.minecraft.world.Container {
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
            ItemStack stack = net.minecraft.world.ContainerHelper.removeItem(micro.getOrCreateInventory(slotIdx, size),
                    i, j);
            if (!stack.isEmpty())
                setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack stack = net.minecraft.world.ContainerHelper.takeItem(micro.getOrCreateInventory(slotIdx, size),
                    i);
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
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            micro.getOrCreateInventory(slotIdx, size).clear();
            setChanged();
        }
    }
}
