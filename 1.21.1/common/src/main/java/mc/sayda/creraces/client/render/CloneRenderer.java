package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.entity.CloneEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a clone as its owner. The owner's own renderer does the drawing, with the clone's facing and
 * hurt flash lent to the owner for that one draw, so skin, armour, held items and Twilight Lib addons
 * all match. A clone whose owner this client can't see falls back to a plain player model.
 */
@SuppressWarnings("null")
public class CloneRenderer extends LivingEntityRenderer<CloneEntity, PlayerModel<CloneEntity>> {

    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public void render(CloneEntity clone, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light) {
        Player owner = clone.owner();
        if (owner == null) {
            super.render(clone, yaw, partialTick, poseStack, buffers, light);
            return;
        }
        EntityRenderer<? super Player> renderer = this.entityRenderDispatcher.getRenderer(owner);
        Facing ownFacing = Facing.of(owner);
        Facing.of(clone).applyTo(owner);
        try {
            renderer.render(owner, yaw, partialTick, poseStack, buffers, light);
        } finally {
            ownFacing.applyTo(owner);
        }
    }

    /** Crouching lowers a player's model; the clone follows its owner's renderer there too. */
    @Override
    public Vec3 getRenderOffset(CloneEntity clone, float partialTick) {
        Player owner = clone.owner();
        return owner != null
                ? this.entityRenderDispatcher.getRenderer(owner).getRenderOffset(owner, partialTick)
                : super.getRenderOffset(clone, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity clone) {
        return DefaultPlayerSkin.getDefaultTexture();
    }

    /** The rotations and hurt flash a renderer reads from an entity. */
    private record Facing(float yRot, float yRotO, float xRot, float xRotO, float bodyRot, float bodyRotO,
            float headRot, float headRotO, int hurtTime) {
        static Facing of(LivingEntity entity) {
            return new Facing(entity.getYRot(), entity.yRotO, entity.getXRot(), entity.xRotO, entity.yBodyRot,
                    entity.yBodyRotO, entity.yHeadRot, entity.yHeadRotO, entity.hurtTime);
        }

        void applyTo(LivingEntity entity) {
            entity.setYRot(yRot);
            entity.yRotO = yRotO;
            entity.setXRot(xRot);
            entity.xRotO = xRotO;
            entity.yBodyRot = bodyRot;
            entity.yBodyRotO = bodyRotO;
            entity.yHeadRot = headRot;
            entity.yHeadRotO = headRotO;
            entity.hurtTime = hurtTime;
        }
    }
}
