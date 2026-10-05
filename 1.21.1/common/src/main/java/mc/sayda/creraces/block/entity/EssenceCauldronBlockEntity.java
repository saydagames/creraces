package mc.sayda.creraces.block.entity;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class EssenceCauldronBlockEntity extends BlockEntity {

    @Nullable
    private EssenceType essenceType;

    public EssenceCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ESSENCE_CAULDRON_ENTITY.get(), pos, state);
    }

    @Nullable
    public EssenceType getEssenceType() {
        return essenceType;
    }

    public void setEssenceType(@Nullable EssenceType type) {
        this.essenceType = type;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (essenceType != null) {
            tag.putString("essence", essenceType.getSerializedName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("essence")) {
            try {
                essenceType = EssenceType.byId(tag.getString("essence"));
            } catch (IllegalArgumentException unknownId) {
                // An essence type that no longer exists: fall back to plain water
                essenceType = null;
            }
        } else {
            essenceType = null;
        }
        // Client-side this runs for block entity data packets too; force a chunk section re-render so
        // the color handler picks up the new essence type.
        if (level != null && level.isClientSide() && level.isLoaded(worldPosition)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
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
}
