package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScrollItem extends Item {
    public ScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Ability")) {
                ResourceLocation abilityId = new ResourceLocation(tag.getString("Ability"));
                Ability ability = AbilityRegistry.get(abilityId);

                if (ability != null) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
                        // Check race restriction
                        List<ResourceLocation> allowed = ability.allowedRaces();
                        if (!allowed.isEmpty() && !allowed.contains(vars.getRace())) {
                            StringBuilder racesStr = new StringBuilder();
                            for (int i = 0; i < allowed.size(); i++) {
                                ResourceLocation rId = allowed.get(i);
                                Race r = RaceRegistry.get(rId);
                                racesStr.append(r != null ? r.name().getString() : rId.getPath());
                                if (i < allowed.size() - 1)
                                    racesStr.append(", ");
                            }
                            serverPlayer.sendSystemMessage(Component.translatable("creraces.message.race_restricted",
                                    racesStr.toString()).withStyle(ChatFormatting.RED));
                            return;
                        }

                        if (vars.isAbilityUnlocked(abilityId)) {
                            serverPlayer.sendSystemMessage(
                                    Component.translatable("creraces.message.ability_already_learned")
                                            .withStyle(ChatFormatting.YELLOW));
                        } else {
                            // Race matches or no restriction - Consume Scroll
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                            }
                            vars.unlockAbility(abilityId);
                            serverPlayer.sendSystemMessage(Component
                                    .translatable("creraces.message.ability_learned", ability.name().getString())
                                    .withStyle(ChatFormatting.GREEN));

                            // Fun Effects: Knowledge flowing into the player
                            serverPlayer.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                    serverPlayer.getX(), serverPlayer.getY() + 1.5, serverPlayer.getZ(),
                                    50, 0.5, 0.5, 0.5, 0.1);
                            serverPlayer.serverLevel().sendParticles(
                                    net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                                    serverPlayer.getX(), serverPlayer.getY() + 1.2, serverPlayer.getZ(),
                                    20, 0.3, 0.3, 0.3, 0.1);

                            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                                    net.minecraft.sounds.SoundSource.PLAYERS,
                                    1.0f, 1.2f);
                        }
                    });
                } else {
                    player.sendSystemMessage(
                            Component.translatable("creraces.message.ability_invalid").withStyle(ChatFormatting.RED));
                }
            } else {
                player.sendSystemMessage(
                        Component.translatable("creraces.message.ability_invalid").withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Ability")) {
            ResourceLocation abilityId = new ResourceLocation(tag.getString("Ability"));
            Ability ability = AbilityRegistry.get(abilityId);
            if (ability != null) {
                tooltipComponents.add(Component.translatable("creraces.tooltip.scroll_ability", ability.name())
                        .withStyle(ChatFormatting.GRAY));

                // Race Restriction Tooltip (3rd line)
                List<ResourceLocation> allowed = ability.allowedRaces();
                if (!allowed.isEmpty()) {
                    ResourceLocation playerRace = null;
                    if (level != null && level.isClientSide) {
                        playerRace = DataUtils.getVariables(mc.sayda.creraces.client.ClientAccess.getPlayer())
                                .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                                .orElse(null);
                    }

                    MutableComponent raceList = Component.literal("(").withStyle(ChatFormatting.DARK_GRAY);
                    for (int i = 0; i < allowed.size(); i++) {
                        ResourceLocation rId = allowed.get(i);
                        Race r = RaceRegistry.get(rId);
                        String name = (r != null) ? r.name().getString() : rId.getPath();

                        ChatFormatting color = (playerRace != null && playerRace.equals(rId)) ? ChatFormatting.GREEN
                                : ChatFormatting.RED;

                        raceList.append(Component.literal(name).withStyle(color));

                        if (i < allowed.size() - 1) {
                            raceList.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }
                    raceList.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
                    tooltipComponents.add(raceList);
                }
            } else {
                tooltipComponents.add(Component.literal("Unknown Ability: " + abilityId).withStyle(ChatFormatting.RED));
            }
        } else {
            tooltipComponents
                    .add(Component.translatable("creraces.tooltip.scroll_empty").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    // Helper to create a scroll stack for a specific ability
    public static ItemStack create(ResourceLocation abilityId) {
        ItemStack stack = new ItemStack(mc.sayda.creraces.registry.ModItems.ABILITY_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Ability", abilityId.toString());
        stack.setTag(tag);
        return stack;
    }
}
