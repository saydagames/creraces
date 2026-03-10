package mc.sayda.creraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.core.Direction;
import mc.sayda.creraces.registry.ModBlocks;

@SuppressWarnings("null")
public class ToriBellRenderer extends BellRenderer {
    private static final String MODID = "creraces";

    public static final ResourceLocation TOP = new ResourceLocation(MODID, "textures/block/torii_bell_top.png");
    public static final ResourceLocation SIDE = new ResourceLocation(MODID, "textures/block/torii_bell_side.png");
    public static final ResourceLocation BOTTOM = new ResourceLocation(MODID, "textures/block/torii_bell_bottom.png");

    public static final ResourceLocation W_TOP = new ResourceLocation(MODID,
            "textures/block/weathered_torii_bell_top.png");
    public static final ResourceLocation W_SIDE = new ResourceLocation(MODID,
            "textures/block/weathered_torii_bell_side.png");
    public static final ResourceLocation W_BOTTOM = new ResourceLocation(MODID,
            "textures/block/weathered_torii_bell_bottom.png");

    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER_LOCATION = new net.minecraft.client.model.geom.ModelLayerLocation(
            new ResourceLocation(MODID, "tori_bell"), "main");

    private final ModelPart bellBody;
    private final ModelPart body;
    private final ModelPart waist;
    private final ModelPart cap;

    public ToriBellRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER_LOCATION);
        this.bellBody = root.getChild("bell_body");
        this.body = this.bellBody.getChild("body");
        this.waist = this.bellBody.getChild("waist");
        this.cap = this.bellBody.getChild("cap");
    }

    public static net.minecraft.client.model.geom.builders.LayerDefinition createBodyLayer() {
        net.minecraft.client.model.geom.builders.MeshDefinition meshdefinition = new net.minecraft.client.model.geom.builders.MeshDefinition();
        net.minecraft.client.model.geom.builders.PartDefinition partdefinition = meshdefinition.getRoot();

        net.minecraft.client.model.geom.builders.PartDefinition bell_body = partdefinition.addOrReplaceChild(
                "bell_body",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create(),
                net.minecraft.client.model.geom.PartPose.offset(0.0F, 12.0F, 0.0F));

        // Note: UVs are somewhat arbitrary as we use separate textures,
        // but we'll try to match the logic from the JSON model.
        // Waist (Bottom)
        bell_body.addOrReplaceChild("waist",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F,
                        -5.0F,
                        10.0F, 2.0F, 10.0F),
                net.minecraft.client.model.geom.PartPose.offset(0.0F, 0.0F, 0.0F));

        // Body (Middle)
        bell_body.addOrReplaceChild("body",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 2.0F,
                        -4.0F,
                        8.0F, 8.0F, 8.0F),
                net.minecraft.client.model.geom.PartPose.offset(0.0F, 0.0F, 0.0F));

        // Cap (Top)
        bell_body.addOrReplaceChild("cap",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 10.0F,
                        -2.0F, 4.0F, 2.0F, 4.0F),
                net.minecraft.client.model.geom.PartPose.offset(0.0F, 0.0F, 0.0F));

        return net.minecraft.client.model.geom.builders.LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void render(BellBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        float f = (float) blockEntity.ticks + partialTick;
        float g = 0.0F;
        float h = 0.0F;
        if (blockEntity.shaking) {
            float i = (float) Math.sin((double) (f / (float) Math.PI)) / (4.0F + f / 3.0F);
            if (blockEntity.clickDirection == Direction.NORTH) {
                g = -i;
            } else if (blockEntity.clickDirection == Direction.SOUTH) {
                g = i;
            } else if (blockEntity.clickDirection == Direction.EAST) {
                h = -i;
            } else if (blockEntity.clickDirection == Direction.WEST) {
                h = i;
            }
        }

        this.bellBody.xRot = g;
        this.bellBody.zRot = h;

        boolean weathered = blockEntity.getBlockState().is(ModBlocks.WEATHERED_TORI_BELL.get());
        ResourceLocation topTex = weathered ? W_TOP : TOP;
        ResourceLocation sideTex = weathered ? W_SIDE : SIDE;
        ResourceLocation bottomTex = weathered ? W_BOTTOM : BOTTOM;

        // Render each part with its corresponding texture
        // Note: In vanilla BellModel, 'cap' is the top, 'waist' is the bottom, and
        // 'body' is the middle.
        // We render side texture for body, top for cap, and bottom for waist.

        renderPart(poseStack, bufferSource, combinedLight, combinedOverlay, this.cap, topTex);
        renderPart(poseStack, bufferSource, combinedLight, combinedOverlay, this.body, sideTex);
        renderPart(poseStack, bufferSource, combinedLight, combinedOverlay, this.waist, bottomTex);
    }

    private void renderPart(PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay,
            ModelPart part, ResourceLocation texture) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
        part.render(poseStack, vertexConsumer, combinedLight, combinedOverlay);
    }
}
