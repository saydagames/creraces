package mc.sayda.creraces.client.model;

import net.minecraft.world.entity.Entity;
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

/**
 * Tornado Model - ported from legacy Modelwind_wall2.
 */
@SuppressWarnings("null")
public class TornadoModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("creraces", "tornado"), "main");

    private final ModelPart layer1;
    private final ModelPart layer2;
    private final ModelPart layer3;
    private final ModelPart layer4;
    private final ModelPart layer5;

    public TornadoModel(ModelPart root) {
        this.layer1 = root.getChild("layer1");
        this.layer2 = root.getChild("layer2");
        this.layer3 = root.getChild("layer3");
        this.layer4 = root.getChild("layer4");
        this.layer5 = root.getChild("layer5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition layer1 = partdefinition.addOrReplaceChild("layer1", CubeListBuilder.create().texOffs(84, 48)
                .addBox(16.0F, -16.0F, -7.0F, 0.0F, 16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        layer1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(84, 16).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.3562F, 0.0F));
        layer1.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(84, 32).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        layer1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(28, 80).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        layer1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(84, 64).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        layer1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(56, 80).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        layer1.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(84, 0).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        layer1.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(84, 80).addBox(16.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition layer2 = partdefinition.addOrReplaceChild("layer2", CubeListBuilder.create().texOffs(156, 48)
                .addBox(20.0F, -24.0F, -4.0F, 0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        layer2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(156, 64).addBox(20.0F, -16.0F, -4.0F, 0.0F,
                16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        layer2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(120, 148).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 0.3927F, 0.0F));
        layer2.addOrReplaceChild("cube_r10",
                CubeListBuilder.create().texOffs(48, 150).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        layer2.addOrReplaceChild("cube_r11",
                CubeListBuilder.create().texOffs(32, 150).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        layer2.addOrReplaceChild("cube_r12",
                CubeListBuilder.create().texOffs(156, 32).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        layer2.addOrReplaceChild("cube_r13",
                CubeListBuilder.create().texOffs(16, 150).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        layer2.addOrReplaceChild("cube_r14",
                CubeListBuilder.create().texOffs(156, 16).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        layer2.addOrReplaceChild("cube_r15",
                CubeListBuilder.create().texOffs(0, 150).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        layer2.addOrReplaceChild("cube_r16",
                CubeListBuilder.create().texOffs(156, 0).addBox(20.0F, -16.0F, -4.0F, 0.0F, 16.0F, 8.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        layer2.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(140, 146).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.1781F, 0.0F));
        layer2.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(96, 150).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        layer2.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(140, 130).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.9635F, 0.0F));
        layer2.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(80, 150).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        layer2.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(140, 114).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 2.7489F, 0.0F));
        layer2.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(64, 150).addBox(20.0F, -16.0F, -4.0F,
                0.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 2.3562F, 0.0F));

        PartDefinition layer3 = partdefinition.addOrReplaceChild("layer3", CubeListBuilder.create().texOffs(136, 64)
                .addBox(24.0F, -32.0F, -5.0F, 0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        layer3.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(60, 132).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.7489F, 0.0F));
        layer3.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(140, 96).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.3562F, 0.0F));
        layer3.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(40, 132).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        layer3.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(136, 80).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        layer3.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(120, 114).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.3927F, 0.0F));
        layer3.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(20, 132).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        layer3.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(136, 48).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        layer3.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(0, 132).addBox(24.0F, -32.0F, -5.0F, 0.0F,
                16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        layer3.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(136, 32).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        layer3.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(120, 130).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        layer3.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(136, 16).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        layer3.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(120, 98).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.1781F, 0.0F));
        layer3.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(136, 0).addBox(24.0F, -32.0F, -5.0F, 0.0F,
                16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        layer3.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(80, 132).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.9635F, 0.0F));
        layer3.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(100, 132).addBox(24.0F, -32.0F, -5.0F,
                0.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition layer4 = partdefinition.addOrReplaceChild("layer4", CubeListBuilder.create().texOffs(112, 80)
                .addBox(28.0F, -8.0F, -6.0F, 0.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        layer4.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(96, 114).addBox(28.0F, -32.0F, -6.0F,
                0.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 2.7489F, 0.0F));
        layer4.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(72, 114).addBox(28.0F, -32.0F, -6.0F,
                0.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 2.3562F, 0.0F));
        layer4.addOrReplaceChild("cube_r40",
                CubeListBuilder.create().texOffs(48, 114).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        layer4.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(24, 114).addBox(28.0F, -32.0F, -6.0F,
                0.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        layer4.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(0, 114).addBox(28.0F, -32.0F, -6.0F, 0.0F,
                16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.3927F, 0.0F));
        layer4.addOrReplaceChild("cube_r43",
                CubeListBuilder.create().texOffs(112, 64).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        layer4.addOrReplaceChild("cube_r44",
                CubeListBuilder.create().texOffs(112, 48).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        layer4.addOrReplaceChild("cube_r45",
                CubeListBuilder.create().texOffs(112, 32).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        layer4.addOrReplaceChild("cube_r46",
                CubeListBuilder.create().texOffs(112, 16).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        layer4.addOrReplaceChild("cube_r47",
                CubeListBuilder.create().texOffs(112, 0).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        layer4.addOrReplaceChild("cube_r48",
                CubeListBuilder.create().texOffs(96, 98).addBox(28.0F, -32.0F, -6.0F, 0.0F, 16.0F, 12.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        layer4.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(72, 98).addBox(28.0F, -32.0F, -6.0F, 0.0F,
                16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 1.1781F, 0.0F));
        layer4.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(48, 98).addBox(28.0F, -32.0F, -6.0F, 0.0F,
                16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        layer4.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(24, 98).addBox(28.0F, -32.0F, -6.0F, 0.0F,
                16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 1.9635F, 0.0F));
        layer4.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(0, 98).addBox(28.0F, -32.0F, -6.0F, 0.0F,
                16.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition layer5 = partdefinition.addOrReplaceChild("layer5", CubeListBuilder.create().texOffs(28, 64)
                .addBox(32.0F, -16.0F, -7.0F, 0.0F, 16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        layer5.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(28, 48).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.9635F, 0.0F));
        layer5.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(0, 48).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.7489F, 0.0F));
        layer5.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(0, 80).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.3562F, 0.0F));
        layer5.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(28, 32).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        layer5.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(56, 64).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        layer5.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(0, 16).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.3927F, 0.0F));
        layer5.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(0, 32).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        layer5.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(0, 64).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        layer5.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(28, 16).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        layer5.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(56, 48).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        layer5.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(28, 0).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        layer5.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(56, 32).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        layer5.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(0, 0).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.1781F, 0.0F));
        layer5.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(56, 16).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        layer5.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(56, 0).addBox(32.0F, -16.0F, -7.0F, 0.0F,
                16.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.layer3.yRot = ageInTicks * 0.5f;
        this.layer4.yRot = ageInTicks * 0.6f;
        this.layer1.yRot = ageInTicks * 0.7f;
        this.layer2.yRot = ageInTicks * 0.8f;
        this.layer5.yRot = ageInTicks * 0.9f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        layer1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        layer2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        layer3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        layer4.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        layer5.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
