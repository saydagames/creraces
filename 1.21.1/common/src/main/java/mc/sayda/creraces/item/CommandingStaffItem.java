package mc.sayda.creraces.item;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.entity.RemainsEntity;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.race.SocialPassivesHelper;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModParticles;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
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

/**
 * Lets a race that can command socials claim mobs that respect it as servants, then direct
 * them. Sneak-use cycles the pending mode (follow, move, attack, free); a normal use commits it
 * and, in move/attack mode, marks the spot or target that the servant goals read back off the
 * owner's held staff.
 */
public class CommandingStaffItem extends Item {

    /** Persistent-data key on a claimed mob holding its commander's UUID. */
    public static final String SERVANT_OF = "creraces:servant_of";
    public static final String TAG_COMMAND_MODE = "CommandMode";
    public static final String TAG_COMMAND_TARGET = "CommandTarget";
    public static final String TAG_COMMAND_POS = "CommandPos";
    private static final String TAG_PENDING_MODE = "PendingMode";

    private static final double COMMAND_RANGE = 32.0;

    public CommandingStaffItem(Properties properties) {
        super(properties);
    }

    /** The staff in the player's main hand, else their off hand, or null if they hold neither. */
    @Nullable
    public static ItemStack findHeldStaff(Player player) {
        if (player.getMainHandItem().is(ModItems.COMMANDING_STAFF.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ModItems.COMMANDING_STAFF.get())) return player.getOffhandItem();
        return null;
    }

    /** The staff's command data for reading; an empty tag if the staff has never been used. */
    public static CompoundTag commandData(ItemStack staff) {
        return ItemNbt.get(staff);
    }

    @Override
    @Nonnull
    public InteractionResult interactLivingEntity(@Nonnull ItemStack stack, @Nonnull Player player,
            @Nonnull LivingEntity interactionTarget, @Nonnull InteractionHand hand) {
        boolean serverSide = !player.level().isClientSide;
        if (!canCommandSocials(player)) {
            if (serverSide) {
                player.displayClientMessage(
                        Component.translatable("msg.creraces.staff_fail").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (interactionTarget instanceof Mob mob && !(mob instanceof RemainsEntity)) {
            if (!SocialPassivesHelper.isRespectedBy(player, mob)) {
                if (serverSide) {
                    player.displayClientMessage(Component.translatable("msg.creraces.staff_no_authority"), true);
                }
            } else if (((IPersistentDataAccessor) mob).creraces$getPersistentData().contains(SERVANT_OF)) {
                if (serverSide) {
                    player.displayClientMessage(Component.translatable("msg.creraces.staff_already_servant"), true);
                }
            } else {
                if (serverSide) {
                    claimServant(player, mob);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.interactLivingEntity(stack, player, interactionTarget, hand);
    }

    private static void claimServant(Player player, Mob mob) {
        ((IPersistentDataAccessor) mob).creraces$getPersistentData().putUUID(SERVANT_OF, player.getUUID());
        player.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }
        player.displayClientMessage(Component.translatable("msg.creraces.servant_claimed")
                .withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
            @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!canCommandSocials(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.creraces.staff_fail").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            selectNextMode(level, player, stack);
            return InteractionResultHolder.success(stack);
        }

        String commandMode = commitPendingMode(stack);
        Vec3 eyePos = player.getEyePosition();
        Vec3 endPos = eyePos.add(player.getViewVector(1.0f).scale(COMMAND_RANGE));

        if (commandMode.equals("attack")) {
            EntityHitResult entityHit = getEntityHitResult(player, eyePos, endPos);
            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                if (!level.isClientSide) {
                    markAttackTarget(level, player, stack, target);
                }
                return InteractionResultHolder.success(stack);
            }
        }

        if (commandMode.equals("move") || commandMode.equals("attack")) {
            BlockHitResult blockHit = level
                    .clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                if (!level.isClientSide) {
                    markCommandPos(level, player, stack, blockHit.getLocation(), commandMode.equals("attack"));
                }
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /** Sneak-use: advances the pending mode, which only takes effect on the next normal use. */
    private static void selectNextMode(Level level, Player player, ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        String currentMode = tag.contains(TAG_PENDING_MODE) ? tag.getString(TAG_PENDING_MODE)
                : tag.contains(TAG_COMMAND_MODE) ? tag.getString(TAG_COMMAND_MODE) : "follow";
        String nextMode = switch (currentMode) {
            case "follow" -> "move";
            case "move" -> "attack";
            case "attack" -> "free";
            default -> "follow";
        };
        ItemNbt.mutate(stack, t -> t.putString(TAG_PENDING_MODE, nextMode));

        if (!level.isClientSide) {
            Component modeComp = modeComponent(nextMode, Component.translatable("msg.creraces.mode_unknown"));
            player.displayClientMessage(Component.translatable("msg.creraces.staff_selecting", modeComp), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }

    /** Promotes the pending mode, if any, to the active one and returns the mode now in effect. */
    private static String commitPendingMode(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.contains(TAG_PENDING_MODE)) {
            String pending = tag.getString(TAG_PENDING_MODE);
            ItemNbt.mutate(stack, t -> {
                t.putString(TAG_COMMAND_MODE, pending);
                t.remove(TAG_PENDING_MODE);
            });
            return pending;
        }
        return tag.contains(TAG_COMMAND_MODE) ? tag.getString(TAG_COMMAND_MODE) : "follow";
    }

    private static void markAttackTarget(Level level, Player player, ItemStack stack, LivingEntity target) {
        ItemNbt.mutate(stack, t -> {
            t.putUUID(TAG_COMMAND_TARGET, target.getUUID());
            t.remove(TAG_COMMAND_POS);
        });

        player.displayClientMessage(
                Component.translatable("msg.creraces.command_attack", target.getDisplayName())
                        .withStyle(ChatFormatting.RED),
                true);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.MARKER_ATTACK.get(),
                    target.getX(), target.getEyeY(), target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private static void markCommandPos(Level level, Player player, ItemStack stack, Vec3 pos, boolean attackMarker) {
        CompoundTag posTag = new CompoundTag();
        posTag.putDouble("x", pos.x);
        posTag.putDouble("y", pos.y);
        posTag.putDouble("z", pos.z);
        ItemNbt.mutate(stack, t -> {
            t.put(TAG_COMMAND_POS, posTag);
            t.remove(TAG_COMMAND_TARGET);
        });

        player.displayClientMessage(
                Component.translatable("msg.creraces.command_move").withStyle(ChatFormatting.AQUA), true);
        if (level instanceof ServerLevel serverLevel) {
            SimpleParticleType marker = attackMarker
                    ? ModParticles.MARKER_ATTACK.get()
                    : ModParticles.MARKER_MOVE.get();
            serverLevel.sendParticles(marker, pos.x, pos.y + 0.1, pos.z, 1, 0, 0, 0, 0);
        }
    }

    @Nullable
    private static EntityHitResult getEntityHitResult(Player player, Vec3 eyePos, Vec3 endPos) {
        return ProjectileUtil.getEntityHitResult(
                player.level(), player, eyePos, endPos,
                player.getBoundingBox().expandTowards(player.getViewVector(1.0f).scale(COMMAND_RANGE)).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity != player
                        && !(entity instanceof RemainsEntity)
                        && !isOwnServant(entity, player));
    }

    /** Only the player's own servants are off-limits as attack targets; another player's are fair game. */
    private static boolean isOwnServant(Entity entity, Player player) {
        if (!(entity instanceof IPersistentDataAccessor accessor))
            return false;
        CompoundTag nbt = accessor.creraces$getPersistentData();
        return nbt.contains(SERVANT_OF) && player.getUUID().equals(DataUtils.loadUUID(nbt, SERVANT_OF));
    }

    private static boolean canCommandSocials(Player player) {
        return DataUtils.getVariables(player)
                .map(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    return race != null && race.passives() != null && race.passives().canCommandSocials();
                })
                .orElse(false);
    }

    private static Component modeComponent(String mode, Component fallback) {
        return switch (mode) {
            case "follow" -> Component.translatable("msg.creraces.mode_follow").withStyle(ChatFormatting.GREEN);
            case "move" -> Component.translatable("msg.creraces.mode_move").withStyle(ChatFormatting.AQUA);
            case "attack" -> Component.translatable("msg.creraces.mode_attack").withStyle(ChatFormatting.RED);
            case "free" -> Component.translatable("msg.creraces.mode_free").withStyle(ChatFormatting.YELLOW);
            default -> fallback;
        };
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, @Nonnull List<Component> tooltip,
            @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.creraces.commanding_staff.desc").withStyle(ChatFormatting.GRAY));
        CompoundTag tag = commandData(stack);
        String commandMode = tag.contains(TAG_COMMAND_MODE) ? tag.getString(TAG_COMMAND_MODE) : "follow";

        Component modeComp = modeComponent(commandMode,
                Component.translatable("msg.creraces.mode_follow").withStyle(ChatFormatting.GREEN));

        tooltip.add(
                Component.translatable("item.creraces.commanding_staff.mode", modeComp).withStyle(ChatFormatting.GOLD));
    }
}
