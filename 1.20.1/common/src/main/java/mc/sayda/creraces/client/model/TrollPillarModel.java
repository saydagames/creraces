package mc.sayda.creraces.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Client-side model for the Troll Pillar entity.
 * Ported from legacy ModelTrollPillar2 (Blockbench 4.2.5, 1.17-1.18 format).
 * Texture: {@code creraces:textures/entities/black_stone_bricks.png}
 */
public class TrollPillarModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("creraces", "troll_pillar"), "main");

    private final ModelPart bot;
    private final ModelPart top;

    public TrollPillarModel(ModelPart root) {
        this.bot = root.getChild("bot");
        this.top = root.getChild("top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bot = root.addOrReplaceChild("bot",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.6751F, -7.25F, -16.1761F, 8.0F, 13.0F, 9.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(3.3249F, -0.25F, -15.1761F, 2.0F, 6.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(3.3249F, 1.75F, -8.1761F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(3.3249F, -6.25F, -14.1761F, 1.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(3.3249F, -4.25F, -9.1761F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(4.3249F, -5.25F, -13.1761F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-6.6751F, -0.25F, -15.1761F, 2.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-5.6751F, -5.25F, -15.1761F, 1.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-6.6751F, 0.75F, -16.1761F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-5.6751F, -6.25F, -14.1761F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-6.6751F, -4.25F, -14.1761F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-6.6751F, -2.25F, -10.1761F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-5.6751F, -3.25F, -9.1761F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.6751F, 18.25F, 12.1761F));

        bot.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(16.0F, -3.25F, 1.25F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(17.0F, -3.25F, -2.75F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(16.0F, -5.25F, -3.75F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(16.0F, -0.25F, -4.75F, 2.0F, 6.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(16.0F, 1.75F, -5.75F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(5.3249F, -6.25F, -3.75F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(5.3249F, 2.75F, 1.25F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(6.3249F, -0.25F, 1.25F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(6.3249F, -5.25F, -0.75F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(5.3249F, -0.25F, -4.75F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition top = root.addOrReplaceChild("top",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.6751F, -7.25F, -14.1761F, 4.0F, 13.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-1.6751F, -13.25F, -13.1761F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(1.3249F, -0.25F, -14.1761F, 2.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(1.3249F, 1.75F, -10.1761F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(1.3249F, -5.25F, -13.1761F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-3.6751F, -0.25F, -14.1761F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.6751F, 0.75F, -13.1761F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.6751F, 2.75F, -11.1761F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-3.6751F, -5.25F, -13.1761F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-2.6751F, -11.25F, -13.1761F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(0.3249F, -11.25F, -12.1761F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.6751F, 5.25F, 12.1761F));

        top.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(14.0F, -5.25F, -1.75F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(15.2249F, 2.75F, -1.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(15.2249F, 1.75F, -0.75F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(14.2249F, -0.25F, -2.75F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(8.3249F, -3.25F, -0.75F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(8.3249F, -5.25F, -1.75F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(8.3249F, 0.75F, -2.75F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(7.3249F, 1.75F, -1.75F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(8.3249F, -0.25F, -1.75F, 1.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // Stationary entity — no animation
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
            int packedOverlay, float red, float green, float blue, float alpha) {
        bot.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        top.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
