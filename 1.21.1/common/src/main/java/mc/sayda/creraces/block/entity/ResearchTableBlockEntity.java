package mc.sayda.creraces.block.entity;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.ability.HexRecipe;
import mc.sayda.creraces.ability.HexRecipeManager;
import mc.sayda.creraces.block.ResearchTableBlock;
import mc.sayda.creraces.item.InkAndQuillItem;
import mc.sayda.creraces.item.ScrollItem;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ResearchResultPacket;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.EssenceBeltHelper;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import mc.sayda.creraces.world.inventory.ResearchTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResearchTableBlockEntity extends BlockEntity implements ExtendedMenuProvider {

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchTableBlockEntity.this.setChanged();
        }
    };

    private final Map<HexPos, EssenceType> hexGrid = new HashMap<>();

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RESEARCH_TABLE_ENTITY.get(), pos, state);
    }

    public Container getInventory() {
        return inventory;
    }

    public Map<HexPos, EssenceType> getHexGrid() {
        return hexGrid;
    }

    public void placeEssence(HexPos pos, EssenceType essence) {
        hexGrid.put(pos, essence);
        setChanged();
    }

    public void removeEssence(HexPos pos) {
        hexGrid.remove(pos);
        setChanged();
    }

    public void clearGrid() {
        hexGrid.clear();
        setChanged();
    }

    public void attemptCraft(ServerPlayer player) {
        ItemStack quill = inventory.getItem(0);
        ItemStack scroll = inventory.getItem(1);

        if (!(quill.getItem() instanceof InkAndQuillItem) || !(scroll.getItem() instanceof ScrollItem)) {
            BoundaryHandler.sendResearchResult(player, new ResearchResultPacket(false, null));
            return;
        }

        Optional<HexRecipe> match = HexRecipeManager.match(hexGrid);
        if (match.isEmpty()) {
            BoundaryHandler.sendResearchResult(player, new ResearchResultPacket(false, null));
            return;
        }

        HexRecipe recipe = match.get();

        // Count essences required by the grid and consume from adjacent storage then belt (unless creative)
        if (!player.getAbilities().instabuild) {
            Direction tableFacing = getBlockState().getValue(ResearchTableBlock.FACING);
            List<BlockPos> tableParts = List.of(
                    worldPosition,
                    worldPosition.relative(tableFacing.getCounterClockWise()));
            Map<EssenceType, Integer> needed = new HashMap<>();
            for (EssenceType t : hexGrid.values()) {
                needed.merge(t, 1, Integer::sum);
            }
            for (var entry : needed.entrySet()) {
                if (EssenceBeltHelper.getEssenceCount(player, entry.getKey(), level, tableParts) < entry.getValue()) {
                    BoundaryHandler.sendResearchResult(player, new ResearchResultPacket(false, null));
                    return;
                }
            }
            for (var entry : needed.entrySet()) {
                EssenceBeltHelper.consumeEssence(player, entry.getKey(), entry.getValue(), level, tableParts);
            }

            int newDamage = quill.getDamageValue() + 1;
            if (newDamage >= quill.getMaxDamage()) {
                inventory.setItem(0, ItemStack.EMPTY);
            } else {
                ItemStack damagedQuill = quill.copy();
                damagedQuill.setDamageValue(newDamage);
                inventory.setItem(0, damagedQuill);
            }
        }
        inventory.setItem(1, ScrollItem.create(recipe.ability(), recipe.level()));

        clearGrid();
        BoundaryHandler.syncHexGrid(player, this);
        BoundaryHandler.sendResearchResult(player, new ResearchResultPacket(true, recipe.ability()));
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        // Snapshot adjacent-storage essence counts so the client screen can display them
        Direction facing = getBlockState().getValue(ResearchTableBlock.FACING);
        List<BlockPos> tableParts = List.of(worldPosition, worldPosition.relative(facing.getCounterClockWise()));
        for (EssenceType type : EssenceType.values()) {
            buf.writeVarInt(level != null ? EssenceBeltHelper.getStorageCount(type, level, tableParts) : 0);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.creraces.research_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, @Nonnull Inventory playerInv, @Nonnull Player player) {
        return new ResearchTableMenu(syncId, playerInv, inventory, worldPosition);
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // saveOptional, not save: 1.21 throws "Cannot encode empty ItemStack" on an empty slot, and
        // these two are read back with parseOptional.
        tag.put("quill", inventory.getItem(0).saveOptional(registries));
        tag.put("scroll", inventory.getItem(1).saveOptional(registries));

        ListTag gridTag = new ListTag();
        for (Map.Entry<HexPos, EssenceType> entry : hexGrid.entrySet()) {
            CompoundTag cell = new CompoundTag();
            cell.putInt("q", entry.getKey().q());
            cell.putInt("r", entry.getKey().r());
            cell.putString("essence", entry.getValue().getSerializedName());
            gridTag.add(cell);
        }
        tag.put("hex_grid", gridTag);
    }

    @Override
    protected void loadAdditional(@Nonnull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.setItem(0, ItemStack.parseOptional(registries, tag.getCompound("quill")));
        inventory.setItem(1, ItemStack.parseOptional(registries, tag.getCompound("scroll")));

        hexGrid.clear();
        ListTag gridTag = tag.getList("hex_grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < gridTag.size(); i++) {
            CompoundTag cell = gridTag.getCompound(i);
            HexPos pos = new HexPos(cell.getInt("q"), cell.getInt("r"));
            try {
                EssenceType essence = EssenceType.byId(cell.getString("essence"));
                hexGrid.put(pos, essence);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
