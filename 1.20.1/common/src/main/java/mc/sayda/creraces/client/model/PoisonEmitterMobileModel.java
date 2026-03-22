package mc.sayda.creraces.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Poison Emitter Mobile Model - ported from legacy Modelpoison_emitter_wheel2.
 * Features a rotating wheel animation.
 */
@SuppressWarnings("null")
public class PoisonEmitterMobileModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("creraces", "poison_emitter_mobile"), "main");
    
    private final ModelPart base;
    private final ModelPart wheel;

    public PoisonEmitterMobileModel(ModelPart root) {
        this.base = root.getChild("base");
        this.wheel = root.getChild("wheel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition base = partdefinition.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 2.7728F, -5.0003F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 11).addBox(-4.0F, -4.8272F, -4.0003F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 11).addBox(-1.0F, 1.9728F, -4.5003F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 11).addBox(-1.0F, 1.9728F, 2.4997F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 20).addBox(-0.5F, 9.4728F, -3.0003F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 9.0272F, 0.0003F, 0.0F, -1.5708F, 0.0F));

        base.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.5F, -2.0272F, -0.0003F, 3.1416F, 0.0F, -3.0107F));

        base.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.5F, -2.0272F, -0.0003F, 0.0F, 0.0F, -0.1309F));

        base.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.5F, 2.4728F, -0.0003F, 0.0F, 1.5708F, -0.1309F));

        base.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.5F, 2.4728F, -0.0003F, 0.0F, -1.5708F, 0.1309F));

        base.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.4728F, -4.5003F, -0.1309F, 0.0F, 0.0F));

        base.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0272F, -4.5003F, -0.1309F, 0.0F, 0.0F));

        base.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0272F, 4.9997F, 3.0107F, 0.0F, 3.1416F));

        base.addOrReplaceChild("cube_r8",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.4728F, 4.4997F, 3.0107F, 0.0F, 3.1416F));

        base.addOrReplaceChild("cube_r9",
                CubeListBuilder.create().texOffs(32, 11).addBox(4.0F, -14.5F, 4.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 9.9728F, -0.0003F, 0.1309F, 0.0F, -0.1309F));

        base.addOrReplaceChild("cube_r10",
                CubeListBuilder.create().texOffs(32, 11).addBox(-2.0F, -8.0F, 0.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, 1.9728F, -5.0003F, -0.1309F, 0.0F, -0.1309F));

        base.addOrReplaceChild("cube_r11",
                CubeListBuilder.create().texOffs(32, 11).addBox(-6.0F, -14.5F, -6.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 9.9728F, -0.0003F, -0.1309F, 0.0F, 0.1309F));

        base.addOrReplaceChild("cube_r12",
                CubeListBuilder.create().texOffs(32, 11).addBox(0.0F, -8.0F, -2.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 1.9728F, 4.9997F, 0.1309F, 0.0F, 0.1309F));

        PartDefinition wheelPart = partdefinition.addOrReplaceChild("wheel", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));
        wheelPart.addOrReplaceChild("cube_r13",
                CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 48, 48);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.wheel.xRot = limbSwing; // Rotate based on movement distance
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        wheel.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
