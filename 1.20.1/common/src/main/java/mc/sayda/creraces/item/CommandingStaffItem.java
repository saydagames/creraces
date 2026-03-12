package mc.sayda.creraces.item;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.race.SocialPassivesHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CommandingStaffItem extends Item {

    public CommandingStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public InteractionResult interactLivingEntity(@Nonnull ItemStack stack, @Nonnull Player player,
            @Nonnull LivingEntity interactionTarget, @Nonnull InteractionHand hand) {
        if (!canCommandSocials(player)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.creraces.staff_fail").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (interactionTarget instanceof Mob mob) {
            if (SocialPassivesHelper.isRespectedBy(player, mob)) {
                CompoundTag nbt = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
                if (!nbt.contains("creraces:servant_of")) {
                    if (!player.level().isClientSide) {
                        nbt.putString("creraces:servant_of", player.getUUID().toString());
                        player.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                    mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
                        }
                        player.displayClientMessage(Component.translatable("message.creraces.servant_claimed")
                                .withStyle(ChatFormatting.GREEN), true);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    if (!player.level().isClientSide) {
                        player.displayClientMessage(
                                Component.literal("Entity is already a servant!").withStyle(ChatFormatting.YELLOW),
                                true);
                    }
                }
            } else {
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.literal("This creature does not respect your authority.")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
        }

        return super.interactLivingEntity(stack, player, interactionTarget, hand);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
            @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!canCommandSocials(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.creraces.staff_fail").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            String currentMode = tag.contains("PendingMode") ? tag.getString("PendingMode") : 
                                (tag.contains("CommandMode") ? tag.getString("CommandMode") : "follow");

            String nextMode = switch (currentMode) {
                case "follow" -> "move";
                case "move" -> "attack";
                case "attack" -> "free";
                default -> "follow";
            };

            tag.putString("PendingMode", nextMode);

            if (!level.isClientSide) {
                MutableComponent modeComp = switch (nextMode) {
                    case "follow" -> Component.translatable("message.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
                    case "move" -> Component.translatable("message.creraces.mode_move").withStyle(ChatFormatting.AQUA);
                    case "attack" -> Component.translatable("message.creraces.mode_attack").withStyle(ChatFormatting.RED);
                    case "free" -> Component.translatable("message.creraces.mode_free").withStyle(ChatFormatting.YELLOW);
                    default -> Component.translatable("message.creraces.mode_unknown");
                };

                player.displayClientMessage(Component.translatable("message.creraces.staff_selecting", modeComp), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            return InteractionResultHolder.success(stack);
        }

        double range = 32.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(viewVec.scale(range));

        String commandMode = stack.getOrCreateTag().contains("CommandMode")
                ? stack.getOrCreateTag().getString("CommandMode")
                : "follow";

        if (commandMode.equals("attack")) {
            EntityHitResult entityHit = getEntityHitResult(player, eyePos, endPos, range);
            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                if (!level.isClientSide) {
                    stack.getOrCreateTag().putUUID("CommandTarget", target.getUUID());
                    stack.getOrCreateTag().remove("CommandPos");
                    player.displayClientMessage(
                            Component.translatable("message.creraces.command_attack", target.getDisplayName())
                                    .withStyle(ChatFormatting.RED),
                            true);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(mc.sayda.creraces.registry.ModParticles.MARKER_ATTACK.get(),
                                target.getX(), target.getEyeY(), target.getZ(), 1, 0, 0, 0, 0);
                    }
                }
                return InteractionResultHolder.success(stack);
            }
        }

        if (commandMode.equals("move") || commandMode.equals("attack")) {
            BlockHitResult blockHit = level
                    .clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                Vec3 pos = blockHit.getLocation();
                if (!level.isClientSide) {
                    CompoundTag posTag = new CompoundTag();
                    posTag.putDouble("x", pos.x);
                    posTag.putDouble("y", pos.y);
                    posTag.putDouble("z", pos.z);
                    stack.getOrCreateTag().put("CommandPos", posTag);
                    stack.getOrCreateTag().remove("CommandTarget");

                    MutableComponent msg = Component.translatable("message.creraces.command_move").withStyle(ChatFormatting.AQUA);
                    player.displayClientMessage(msg, true);

                    if (level instanceof ServerLevel serverLevel) {
                        net.minecraft.core.particles.SimpleParticleType pt = commandMode.equals("attack")
                                ? mc.sayda.creraces.registry.ModParticles.MARKER_ATTACK.get()
                                : mc.sayda.creraces.registry.ModParticles.MARKER_MOVE.get();
                        serverLevel.sendParticles(pt, pos.x, pos.y + 0.1, pos.z, 1, 0, 0, 0, 0);
                    }
                }
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private EntityHitResult getEntityHitResult(Player player, Vec3 eyePos, Vec3 endPos, double range) {
        return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eyePos, endPos,
                player.getBoundingBox().expandTowards(player.getViewVector(1.0f).scale(range)).inflate(1.0),
                (entity) -> entity instanceof LivingEntity && entity != player);
    }

    private boolean canCommandSocials(Player player) {
        return DataUtils.getVariables(player)
                .map(vars -> {
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                    return race != null && race.passives() != null && race.passives().canCommandSocials();
                })
                .orElse(false);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip,
            @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.creraces.commanding_staff.desc").withStyle(ChatFormatting.GRAY));
        String commandMode = stack.getOrCreateTag().contains("CommandMode")
                ? stack.getOrCreateTag().getString("CommandMode")
                : "follow";

        MutableComponent modeComp = switch (commandMode) {
            case "follow" -> Component.translatable("message.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
            case "move" -> Component.translatable("message.creraces.mode_move").withStyle(ChatFormatting.AQUA);
            case "attack" -> Component.translatable("message.creraces.mode_attack").withStyle(ChatFormatting.RED);
            case "free" -> Component.translatable("message.creraces.mode_free").withStyle(ChatFormatting.YELLOW);
            default -> Component.translatable("message.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
        };

        tooltip.add(
                Component.translatable("item.creraces.commanding_staff.mode", modeComp).withStyle(ChatFormatting.GOLD));
    }
}
