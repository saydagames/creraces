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
        java.util.Objects.requireNonNull(hand);
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Ability")) {
                String abilityName = java.util.Objects.requireNonNull(tag.getString("Ability"));
                ResourceLocation abilityId = ResourceLocation.tryParse(abilityName);
                Ability ability = abilityId != null ? AbilityRegistry.get(abilityId) : null;

                if (ability != null) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
                        // Check race restriction
                        List<ResourceLocation> allowed = ability.allowedRaces();
                        boolean isAllowed = false;
                        if (allowed.isEmpty()) {
                            isAllowed = true;
                        } else {
                            ResourceLocation playerRaceId = vars.getRace();
                            Race playerRace = RaceRegistry.get(playerRaceId);

                            for (ResourceLocation rId : allowed) {
                                if (rId.equals(playerRaceId)) {
                                    isAllowed = true;
                                    break;
                                }
                                if (isDescendantOf(playerRaceId, rId)) {
                                    isAllowed = true;
                                    break;
                                }
                                if (playerRace != null) {
                                    if (rId.toString().equals("creraces:spirit") && playerRace.isSpirit()) {
                                        isAllowed = true;
                                        break;
                                    }
                                    if (rId.toString().equals("creraces:tiny") && playerRace.isTiny()) {
                                        isAllowed = true;
                                        break;
                                    }
                                    if (rId.toString().equals("creraces:aquatic") && playerRace.isAquatic()) {
                                        isAllowed = true;
                                        break;
                                    }
                                    if (rId.toString().equals("creraces:undead") && playerRace.isUndead()) {
                                        isAllowed = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!isAllowed) {
                            StringBuilder racesStr = new StringBuilder();
                            for (int i = 0; i < allowed.size(); i++) {
                                ResourceLocation rId = allowed.get(i);
                                String name;
                                if (rId.toString().equals("creraces:spirit")) {
                                    name = "Spirit";
                                } else if (rId.toString().equals("creraces:tiny")) {
                                    name = "Tiny";
                                } else if (rId.toString().equals("creraces:aquatic")) {
                                    name = "Aquatic";
                                } else if (rId.toString().equals("creraces:undead")) {
                                    name = "Undead";
                                } else {
                                    Race r = RaceRegistry.get(rId);
                                    name = (r != null) ? r.name().getString() : rId.getPath();
                                }
                                racesStr.append(name);
                                if (i < allowed.size() - 1)
                                    racesStr.append(", ");
                            }
                            serverPlayer.sendSystemMessage(Component.translatable("creraces.message.race_restricted",
                                    racesStr.toString()).withStyle(ChatFormatting.RED));
                            return;
                        }

                        int scrollLevel = tag.contains("Level") ? tag.getInt("Level") : 1;
                        int currentLevel = vars.getAbilityLevel(abilityId);

                        if (vars.isAbilityUnlocked(abilityId) && currentLevel >= scrollLevel) {
                            serverPlayer.sendSystemMessage(
                                    Component.translatable("creraces.message.ability_already_learned")
                                            .withStyle(ChatFormatting.YELLOW));
                        } else {
                            // Race matches or no restriction - Consume Scroll
                            if (!player.getAbilities().instabuild) {
                                stack.shrink(1);
                            }
                            boolean wasAlreadyLearned = vars.isAbilityUnlocked(abilityId);
                            vars.unlockAbility(abilityId);
                            vars.setAbilityLevel(abilityId, scrollLevel);

                            Component learnedMsg = wasAlreadyLearned
                                    ? Component.translatable("creraces.message.ability_upgraded", ability.name().getString(), scrollLevel)
                                    : Component.translatable("creraces.message.ability_learned", ability.name().getString());

                            serverPlayer.sendSystemMessage(((MutableComponent)learnedMsg).withStyle(ChatFormatting.GREEN));

                            // Fun Effects: Knowledge flowing into the player
                            if (serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                        serverPlayer.getX(), serverPlayer.getY() + 1.5, serverPlayer.getZ(),
                                        50, 0.5, 0.5, 0.5, 0.1);
                                serverLevel.sendParticles(
                                        net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                                        serverPlayer.getX(), serverPlayer.getY() + 1.2, serverPlayer.getZ(),
                                        20, 0.3, 0.3, 0.3, 0.1);
                            }

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
            ResourceLocation abilityId = ResourceLocation.tryParse(tag.getString("Ability"));
            Ability ability = abilityId != null ? AbilityRegistry.get(abilityId) : null;
            if (ability != null) {
                tooltipComponents.add(Component.translatable("creraces.tooltip.scroll_ability", ability.name())
                        .withStyle(ChatFormatting.GRAY));

                // Race Restriction Tooltip (3rd line)
                List<ResourceLocation> allowed = ability.allowedRaces();
                if (!allowed.isEmpty()) {
                    MutableComponent raceList = Component.literal("(").withStyle(ChatFormatting.DARK_GRAY);
                    for (int i = 0; i < allowed.size(); i++) {
                        ResourceLocation rId = allowed.get(i);
                        String name;
                        boolean matches = false;

                        ResourceLocation playerRaceId = null;
                        Race playerRace = null;
                        if (level != null && level.isClientSide) {
                            playerRaceId = DataUtils.getVariables(mc.sayda.creraces.client.ClientAccess.getPlayer())
                                    .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                                    .orElse(null);
                            if (playerRaceId != null) {
                                playerRace = RaceRegistry.get(playerRaceId);
                            }
                        }

                        if (rId.toString().equals("creraces:spirit")) {
                            name = "Spirit";
                            matches = (playerRace != null && playerRace.isSpirit());
                        } else if (rId.toString().equals("creraces:tiny")) {
                            name = "Tiny";
                            matches = (playerRace != null && playerRace.isTiny());
                        } else if (rId.toString().equals("creraces:aquatic")) {
                            name = "Aquatic";
                            matches = (playerRace != null && playerRace.isAquatic());
                        } else if (rId.toString().equals("creraces:undead")) {
                            name = "Undead";
                            matches = (playerRace != null && playerRace.isUndead());
                        } else {
                            Race r = RaceRegistry.get(rId);
                            name = (r != null) ? r.name().getString() : rId.getPath();
                            matches = (playerRaceId != null &&
                                    (playerRaceId.equals(rId) || isDescendantOf(playerRaceId, rId)));
                        }

                        ChatFormatting color = matches ? ChatFormatting.GREEN : ChatFormatting.RED;

                        raceList.append(java.util.Objects.requireNonNull(Component.literal(name).withStyle(color)));

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
        }
        if (level != null)
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    // Helper to create a scroll stack for a specific ability
    public static ItemStack create(ResourceLocation abilityId, int level) {
        ItemStack stack = new ItemStack(mc.sayda.creraces.registry.ModItems.ABILITY_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Ability", abilityId.toString());
        tag.putInt("Level", level);
        stack.setTag(tag);
        return stack;
    }

    public static ItemStack create(ResourceLocation abilityId) {
        return create(abilityId, 1);
    }

    public static int getLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("Level")) ? tag.getInt("Level") : 0;
    }

    /** Returns true if {@code raceId}'s ancestry chain contains {@code ancestorId}. */
    static boolean isDescendantOf(ResourceLocation raceId, ResourceLocation ancestorId) {
        if (raceId == null) return false;
        Race race = RaceRegistry.get(raceId);
        if (race == null) return false;
        for (ResourceLocation parentId : race.parentRaces()) {
            if (parentId.equals(ancestorId)) return true;
            if (isDescendantOf(parentId, ancestorId)) return true;
        }
        return false;
    }
}
