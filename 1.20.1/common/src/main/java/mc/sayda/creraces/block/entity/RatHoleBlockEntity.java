package mc.sayda.creraces.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import mc.sayda.creraces.registry.ModBlocks;
import java.util.UUID;

public class RatHoleBlockEntity extends BlockEntity {
    private BlockPos destination;
    private UUID ownerUUID;

    public RatHoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RAT_HOLE_ENTITY.get(), pos, state);
    }

    public void setDestination(BlockPos destination) {
        this.destination = destination;
        setChanged();
    }

    public BlockPos getDestination() {
        return destination;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        setChanged();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    protected void saveAdditional(@javax.annotation.Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        if (destination != null) {
            tag.putInt("destX", destination.getX());
            tag.putInt("destY", destination.getY());
            tag.putInt("destZ", destination.getZ());
        }
        if (ownerUUID != null) {
            tag.putUUID("owner", ownerUUID);
        }
    }

    @Override
    public void load(@javax.annotation.Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("destX") && tag.contains("destY") && tag.contains("destZ")) {
            destination = new BlockPos(tag.getInt("destX"), tag.getInt("destY"), tag.getInt("destZ"));
        }
        if (tag.hasUUID("owner")) {
            ownerUUID = tag.getUUID("owner");
        }
    }
}
