package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.client.ClientAccess;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.util.ItemNbt;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ScrollItem extends Item {
    // Category markers an ability can list in allowedRaces instead of a concrete race id.
    private static final String CATEGORY_SPIRIT = "creraces:spirit";
    private static final String CATEGORY_TINY = "creraces:tiny";
    private static final String CATEGORY_AQUATIC = "creraces:aquatic";
    private static final String CATEGORY_UNDEAD = "creraces:undead";

    public ScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Objects.requireNonNull(hand);
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = ItemNbt.get(stack);
            ResourceLocation abilityId = tag.contains("Ability")
                    ? ResourceLocation.tryParse(Objects.requireNonNull(tag.getString("Ability")))
                    : null;
            Ability ability = abilityId != null ? AbilityRegistry.get(abilityId) : null;

            if (ability != null) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                int scrollLevel = tag.contains("Level") ? tag.getInt("Level") : 1;
                DataUtils.getVariables(serverPlayer).ifPresent(
                        vars -> tryLearn(serverPlayer, vars, stack, ability, abilityId, scrollLevel));
            } else {
                player.sendSystemMessage(
                        Component.translatable("msg.creraces.ability_invalid").withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void tryLearn(ServerPlayer player, IPlayerVariables vars, ItemStack stack, Ability ability,
            ResourceLocation abilityId, int scrollLevel) {
        List<ResourceLocation> allowed = ability.allowedRaces();
        if (!allowed.isEmpty()) {
            ResourceLocation playerRaceId = vars.getRace();
            Race playerRace = RaceRegistry.get(playerRaceId);
            boolean isAllowed = allowed.stream().anyMatch(rId -> raceMatches(rId, playerRaceId, playerRace));
            if (!isAllowed) {
                StringBuilder racesStr = new StringBuilder();
                for (int i = 0; i < allowed.size(); i++) {
                    racesStr.append(raceDisplayName(allowed.get(i)));
                    if (i < allowed.size() - 1)
                        racesStr.append(", ");
                }
                player.sendSystemMessage(Component.translatable("msg.creraces.race_restricted",
                        racesStr.toString()).withStyle(ChatFormatting.RED));
                return;
            }
        }

        boolean wasAlreadyLearned = vars.isAbilityUnlocked(abilityId);
        if (wasAlreadyLearned && vars.getAbilityLevel(abilityId) >= scrollLevel) {
            player.sendSystemMessage(
                    Component.translatable("msg.creraces.ability_already_learned").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        vars.unlockAbility(abilityId);
        vars.setAbilityLevel(abilityId, scrollLevel);

        MutableComponent learnedMsg = wasAlreadyLearned
                ? Component.translatable("msg.creraces.ability_upgraded", ability.name().getString(), scrollLevel)
                : Component.translatable("msg.creraces.ability_learned", ability.name().getString());
        player.sendSystemMessage(learnedMsg.withStyle(ChatFormatting.GREEN));
        playLearnEffects(player);
    }

    private static void playLearnEffects(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    50, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    20, 0.3, 0.3, 0.3, 0.1);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag isAdvanced) {
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.contains("Ability")) {
            ResourceLocation abilityId = ResourceLocation.tryParse(tag.getString("Ability"));
            Ability ability = abilityId != null ? AbilityRegistry.get(abilityId) : null;
            if (ability != null) {
                tooltipComponents.add(Component.translatable("tooltip.creraces.scroll_ability", ability.name())
                        .withStyle(ChatFormatting.GRAY));
                // TooltipContext carries no level; ClientAccess hands back null anywhere but the client.
                Player localPlayer = ClientAccess.getPlayer();
                addRaceRestrictionLine(ability, localPlayer, tooltipComponents);
            } else {
                tooltipComponents.add(Component.translatable("tooltip.creraces.scroll_unknown_ability",
                        String.valueOf(abilityId)).withStyle(ChatFormatting.RED));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }

    /** Lists the allowed races, green for any the local player matches and red otherwise. */
    private static void addRaceRestrictionLine(Ability ability, @Nullable Player localPlayer, List<Component> tooltip) {
        List<ResourceLocation> allowed = ability.allowedRaces();
        if (allowed.isEmpty()) {
            return;
        }

        ResourceLocation playerRaceId = localPlayer != null
                ? DataUtils.getVariables(localPlayer).map(IPlayerVariables::getRace).orElse(null)
                : null;
        Race playerRace = playerRaceId != null ? RaceRegistry.get(playerRaceId) : null;

        MutableComponent raceList = Component.literal("(").withStyle(ChatFormatting.DARK_GRAY);
        for (int i = 0; i < allowed.size(); i++) {
            ResourceLocation rId = allowed.get(i);
            ChatFormatting color = raceMatches(rId, playerRaceId, playerRace) ? ChatFormatting.GREEN : ChatFormatting.RED;
            raceList.append(Objects.requireNonNull(Component.literal(raceDisplayName(rId)).withStyle(color)));
            if (i < allowed.size() - 1) {
                raceList.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        raceList.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(raceList);
    }

    private static String raceDisplayName(ResourceLocation rId) {
        return switch (rId.toString()) {
            case CATEGORY_SPIRIT -> "Spirit";
            case CATEGORY_TINY -> "Tiny";
            case CATEGORY_AQUATIC -> "Aquatic";
            case CATEGORY_UNDEAD -> "Undead";
            default -> {
                Race race = RaceRegistry.get(rId);
                yield race != null ? race.name().getString() : rId.getPath();
            }
        };
    }

    public static ItemStack create(ResourceLocation abilityId, int level) {
        ItemStack stack = new ItemStack(ModItems.ABILITY_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Ability", abilityId.toString());
        tag.putInt("Level", level);
        ItemNbt.set(stack, tag);
        return stack;
    }

    public static ItemStack create(ResourceLocation abilityId) {
        return create(abilityId, 1);
    }

    public static int getLevel(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        return tag.contains("Level") ? tag.getInt("Level") : 0;
    }

    /**
     * Returns true if the player's race satisfies race requirement {@code rId}, either by
     * direct id match, race ancestry, or one of the category markers (spirit / tiny / aquatic / undead).
     */
    private static boolean raceMatches(ResourceLocation rId, @Nullable ResourceLocation playerRaceId,
            @Nullable Race playerRace) {
        if (RaceUtils.isRaceOrDescendant(playerRaceId, rId)) {
            return true;
        }
        if (playerRace == null) {
            return false;
        }
        return switch (rId.toString()) {
            case CATEGORY_SPIRIT -> playerRace.isSpirit();
            case CATEGORY_TINY -> playerRace.isTiny();
            case CATEGORY_AQUATIC -> playerRace.isAquatic();
            case CATEGORY_UNDEAD -> playerRace.isUndead();
            default -> false;
        };
    }
}
