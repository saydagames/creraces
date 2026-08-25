package mc.sayda.creraces.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class CustomBoatItem extends Item {
    private final Supplier<? extends EntityType<? extends Boat>> entityType;

    public CustomBoatItem(Supplier<? extends EntityType<? extends Boat>> entityType, Item.Properties props) {
        super(props);
        this.entityType = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        HitResult hitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        }
        Vec3 viewVec = player.getViewVector(1.0F);
        for (net.minecraft.world.entity.Entity entity : level.getEntities(player,
                player.getBoundingBox().expandTowards(viewVec.scale(5.0D)).inflate(1.0D))) {
            if (entity.isPickable() && entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition())) {
                return InteractionResultHolder.pass(itemstack);
            }
        }
        if (hitresult.getType() == HitResult.Type.BLOCK) {
            if (!level.isClientSide) {
                Boat boat = entityType.get().create(level);
                if (boat != null) {
                    Vec3 loc = hitresult.getLocation();
                    boat.moveTo(loc.x, loc.y, loc.z, player.getYRot(), 0.0F);
                    boat.setYHeadRot(player.getYRot());
                    level.addFreshEntity(boat);
                    level.gameEvent(GameEvent.ENTITY_PLACE, boat.position(), GameEvent.Context.of(player));
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
        }
        return InteractionResultHolder.pass(itemstack);
    }
}
