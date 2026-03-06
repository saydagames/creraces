package mc.sayda.creraces.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

@SuppressWarnings("null")
public class PoisonEmitterModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("creraces", "poison_emitter"), "main");
    private final ModelPart base;

    public PoisonEmitterModel(ModelPart root) {
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition base = partdefinition.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -2.2F, -5.0F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(0, 11)
                        .addBox(-4.0F, -14.8F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        base.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.5F, -12.0F, 0.0F, 3.1416F, 0.0F, -3.0107F));
        base.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.5F, -12.0F, 0.0F, 0.0F, 0.0F, -0.1309F));
        base.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 14.0F, 1.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.5F, -7.5F, 0.0F, 0.0F, 1.5708F, -0.1309F));
        base.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 14.0F, 1.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.5F, -7.5F, 0.0F, 0.0F, -1.5708F, 0.1309F));
        base.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 14.0F, 1.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -7.5F, -4.5F, -0.1309F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -12.0F, -4.5F, -0.1309F, 0.0F, 0.0F));
        base.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 3.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -12.0F, 5.0F, 3.0107F, 0.0F, 3.1416F));
        base.addOrReplaceChild("cube_r8",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -7.5F, -0.5F, 8.0F, 14.0F, 1.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -7.5F, 4.5F, 3.0107F, 0.0F, 3.1416F));
        base.addOrReplaceChild("cube_r9",
                CubeListBuilder.create().texOffs(32, 11).addBox(4.0F, -14.5F, 4.0F, 2.0F, 16.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, -0.1309F));
        base.addOrReplaceChild("cube_r10",
                CubeListBuilder.create().texOffs(32, 11).addBox(-2.0F, -8.0F, 0.0F, 2.0F, 16.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -8.0F, -5.0F, -0.1309F, 0.0F, -0.1309F));
        base.addOrReplaceChild("cube_r11",
                CubeListBuilder.create().texOffs(32, 11).addBox(-6.0F, -14.5F, -6.0F, 2.0F, 16.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.1309F));
        base.addOrReplaceChild("cube_r12",
                CubeListBuilder.create().texOffs(32, 11).addBox(0.0F, -8.0F, -2.0F, 2.0F, 16.0F, 2.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -8.0F, 5.0F, 0.1309F, 0.0F, 0.1309F));
        return LayerDefinition.create(meshdefinition, 48, 48);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.base.xRot = (Mth.sin(ageInTicks * 0.3F + 2) * 0.05F) + ((headPitch * 0.017453292F) / 2);
        this.base.zRot = (Mth.sin(ageInTicks * 0.6F + 2) * 0.05F);
        this.base.yRot = ((netHeadYaw * 0.017453292F) / 2);
    }
}
