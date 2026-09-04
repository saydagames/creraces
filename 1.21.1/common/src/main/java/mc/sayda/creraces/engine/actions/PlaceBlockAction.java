package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModGameRules;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;

public class PlaceBlockAction implements ActionRegistry.RaceAction {
    private final ResourceLocation block;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean useRaycast;
    private final ScalingValue rayRange;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean overwrite;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final List<DataModification> dataModifications;
    private final String particle;
    private final int particleCount;
    private final String sound;

    private static class DataModification {
        final String key;
        final ScalingValue value;
        final String mode;

        DataModification(String key, ScalingValue value, String mode) {
            this.key = key;
            this.value = value;
            this.mode = mode != null ? mode.toUpperCase() : "SET";
        }
    }

    public PlaceBlockAction(ResourceLocation block, boolean useTarget, boolean useTargetBlock, boolean useRaycast,
            ScalingValue rayRange, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean overwrite, boolean absolute, ScalingValue.MathOp coordinateMath,
            List<DataModification> dataModifications, String particle, int particleCount, String sound) {
        this.block = block;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.overwrite = overwrite;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
        this.dataModifications = dataModifications;
        this.particle = particle;
        this.particleCount = particleCount;
        this.sound = sound;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        if (player == null || player.level() == null)
            return false;

        if (!player.level().getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos targetPos;
        if (absolute) {
            targetPos = BlockPos.ZERO;
        } else if (useRaycast) {
            double range = rayRange.evaluate(player, target, slot);
            net.minecraft.world.phys.BlockHitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                    player.getEyePosition(1f),
                    player.getEyePosition(1f).add(player.getViewVector(1f).scale(range)),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return false;
            targetPos = hit.getBlockPos().relative(hit.getDirection());
        } else if (useTarget && target != null) {
            targetPos = target.blockPosition();
        } else if (useTargetBlock && interact_pos != null) {
            targetPos = interact_pos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            // Apply coordinate rounding mode
            int x = (int) Math.floor(tx);
            int y = (int) Math.floor(ty);
            int z = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                x = (int) Math.round(tx);
                y = (int) Math.round(ty);
                z = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                x = (int) Math.ceil(tx);
                y = (int) Math.ceil(ty);
                z = (int) Math.ceil(tz);
            }
            targetPos = new BlockPos(x, y, z);
        }

        int ox = (int) offsetX.evaluate(player, target, slot);
        int oy = (int) offsetY.evaluate(player, target, slot);
        int oz = (int) offsetZ.evaluate(player, target, slot);
        BlockPos finalPos = targetPos.offset(ox, oy, oz);

        net.minecraft.world.level.block.Block resolvedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(block);

        if (resolvedBlock == null) {
            CreRaces.LOGGER.error("PlaceBlockAction: block '{}' not found in registry", block);
            return false;
        }

        // If the block is already there and we're not overwriting, abort so the
        // ability doesn't progress its state or consume costs when no change occurred.
        if (!overwrite && player.level().getBlockState(finalPos).is(resolvedBlock)) {
            return false;
        }

        // Check if block is replaceable
        boolean canPlace = overwrite || player.level().getBlockState(finalPos).isAir()
                || player.level().getBlockState(finalPos).canBeReplaced();

        // Check for entity obstruction (vanilla behavior)
        if (canPlace && !overwrite) {
            if (!player.level()
                    .getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(finalPos), e -> e != player)
                    .isEmpty()) {
                canPlace = false;
            }
        }

        if (canPlace) {
            // Protection for RootBlock: only owner (or creative) can replace it.
            net.minecraft.world.level.block.state.BlockState existingState = player.level().getBlockState(finalPos);
            if (existingState.getBlock() instanceof mc.sayda.creraces.block.RootBlock) {
                if (!player.isCreative() && !mc.sayda.creraces.block.RootBlock.isOwner(player, finalPos)) {
                    return false;
                }
            }

            player.level().setBlockAndUpdate(finalPos, resolvedBlock.defaultBlockState());

            // Apply Data Modifications if BlockEntity exists
            if (!dataModifications.isEmpty()) {
                BlockEntity be = player.level().getBlockEntity(finalPos);
                if (be != null) {
                    CompoundTag tag = be.saveWithFullMetadata(player.level().registryAccess());
                    boolean changed = false;
                    for (DataModification mod : dataModifications) {
                        // "owner" is a reserved key: auto-filled with the caster's UUID instead of evaluated normally.
                        if ("owner".equalsIgnoreCase(mod.key)) {
                            tag.putUUID("owner", player.getUUID());
                            changed = true;
                        } else if ("REMOVE".equals(mod.mode)) {
                            if (tag.contains(mod.key)) {
                                tag.remove(mod.key);
                                changed = true;
                            }
                        } else {
                            double val = mod.value.evaluate(player, target, slot, interact_pos);
                            if (val == (long) val) {
                                tag.putInt(mod.key, (int) val);
                            } else {
                                tag.putDouble(mod.key, val);
                            }
                            changed = true;
                        }
                    }
                    if (changed) {
                        be.loadWithComponents(tag, player.level().registryAccess());
                        be.setChanged();
                        player.level().sendBlockUpdated(finalPos, player.level().getBlockState(finalPos),
                                player.level().getBlockState(finalPos), 3);
                    }
                }
            }

            // 4. Particles + 5. Sound (Resilient)
            BlockActionEffects.spawnResilientEffects(player, finalPos, particle, sound, particleCount);
            return true;
        } else {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.place_block_failed"), true);
        }

        return false;
    }

    @SuppressWarnings("null")
    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "place_block"), json -> {
            String blockId = GsonHelper.getAsString(json, "block", "minecraft:air");
            ResourceLocation block = ResourceLocation.parse(blockId);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean useRaycast = GsonHelper.getAsBoolean(json, "use_raycast", false);
            ScalingValue rayRange = ScalingValue.fromJson(json, "ray_range", 10.0);
            ScalingValue offsetX = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue offsetZ = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean overwrite = GsonHelper.getAsBoolean(json, "overwrite", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            String particle = GsonHelper.getAsString(json, "particle", "");
            int particleCount = GsonHelper.getAsInt(json, "particle_count", 10);
            String sound = GsonHelper.getAsString(json, "sound", "");

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    coordinateMath = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid math mode in PlaceBlockAction: {}", json.get("math").getAsString());
                }
            }

            List<DataModification> mods = new ArrayList<>();
            if (json.has("data") && json.get("data").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("data");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        String key = GsonHelper.getAsString(obj, "key", "");
                        ScalingValue val = ScalingValue.fromJson(obj, "value", 0.0);
                        String mode = GsonHelper.getAsString(obj, "mode", "SET");
                        if (!key.isEmpty()) {
                            mods.add(new DataModification(key, val, mode));
                        }
                    }
                }
            }

            return new PlaceBlockAction(block, useTarget, useTargetBlock, useRaycast, rayRange,
                    offsetX, offsetY, offsetZ, overwrite, absolute, coordinateMath, mods, particle, particleCount, sound);
        });
    }
}
