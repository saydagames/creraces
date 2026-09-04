package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.registry.ModGameRules;

@SuppressWarnings("null")
public class BreakBlocksAction implements ActionRegistry.RaceAction {

    private final ScalingValue radius;
    private final boolean dropItems;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public BreakBlocksAction(ScalingValue radius, boolean dropItems, ScalingValue offsetX, ScalingValue offsetY,
            ScalingValue offsetZ, boolean useTarget, boolean useTargetBlock, boolean absolute,
            ScalingValue.MathOp coordinateMath) {
        this.radius = radius;
        this.dropItems = dropItems;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player == null || player.level() == null) return false;
        if (player.level().isClientSide()) return true;

        if (!player.level().getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos basePos;
        if (absolute) {
            basePos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interact_pos != null) {
            basePos = interact_pos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int bx = (int) Math.floor(tx);
            int by = (int) Math.floor(ty);
            int bz = (int) Math.floor(tz);

            switch (coordinateMath) {
                case ROUND:
                    bx = (int) Math.round(tx);
                    by = (int) Math.round(ty);
                    bz = (int) Math.round(tz);
                    break;
                case CEIL:
                    bx = (int) Math.ceil(tx);
                    by = (int) Math.ceil(ty);
                    bz = (int) Math.ceil(tz);
                    break;
                default:
                    break;
            }
            basePos = new BlockPos(bx, by, bz);
        }

        int ox = (int) offsetX.evaluate(player, target, slot);
        int oy = (int) offsetY.evaluate(player, target, slot);
        int oz = (int) offsetZ.evaluate(player, target, slot);
        BlockPos center = basePos.offset(ox, oy, oz);

        double r = radius.evaluate(player, target, slot);
        int maxRadius = mc.sayda.creraces.config.CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get();
        if (maxRadius > 0)
            r = Math.min(r, maxRadius);
        for (int x = -(int) r; x <= r; x++) {
            for (int y = -(int) r; y <= r; y++) {
                for (int z = -(int) r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    float speed = player.level().getBlockState(pos).getDestroySpeed(player.level(), pos);
                    // destroySpeed == -1 means the block is unbreakable (bedrock, command blocks,
                    // etc.)
                    if (speed >= 0) {
                        player.level().destroyBlock(pos, dropItems, player);
                    }
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "break_blocks"), json -> {
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 1.0);
            boolean dropItems = GsonHelper.getAsBoolean(json, "drop_items", true);
            ScalingValue offsetX = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue offsetZ = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    String mode = json.get("math").getAsString().toUpperCase();
                    for (ScalingValue.MathOp op : ScalingValue.MathOp.values()) {
                        if (op.name().equals(mode)) {
                            coordinateMath = op;
                            break;
                        }
                    }
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("Invalid math mode in BreakBlocksAction: {}",
                            json.get("math").getAsString());
                }
            }

            return new BreakBlocksAction(radius, dropItems, offsetX, offsetY, offsetZ, useTarget, useTargetBlock,
                    absolute, coordinateMath);
        });
    }
}
