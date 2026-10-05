package mc.sayda.creraces.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
        ItemStack itemStack = player.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        }
        Vec3 viewVec = player.getViewVector(1.0F);
        for (Entity entity : level.getEntities(player,
                player.getBoundingBox().expandTowards(viewVec.scale(5.0D)).inflate(1.0D))) {
            if (entity.isPickable() && entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition())) {
                return InteractionResultHolder.pass(itemStack);
            }
        }
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Built on both sides, like vanilla, so the client also refuses a spot where the boat won't fit
            Boat boat = entityType.get().create(level);
            if (boat != null) {
                Vec3 loc = hitResult.getLocation();
                boat.moveTo(loc.x, loc.y, loc.z, player.getYRot(), 0.0F);
                boat.setYHeadRot(player.getYRot());
                if (!level.noCollision(boat, boat.getBoundingBox())) {
                    return InteractionResultHolder.fail(itemStack);
                }
                if (!level.isClientSide) {
                    level.addFreshEntity(boat);
                    level.gameEvent(GameEvent.ENTITY_PLACE, boat.position(), GameEvent.Context.of(player));
                    if (!player.getAbilities().instabuild) {
                        itemStack.shrink(1);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
        }
        return InteractionResultHolder.pass(itemStack);
    }
}
