package mc.sayda.creraces.item;

import mc.sayda.creraces.client.ClientAccess;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.quest.QuestSessionRegistry;
import mc.sayda.creraces.registry.ModDataComponents;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * A quest taken from a Quest Board. Tracks its own progress, owner, tier, and expiration
 * window directly on the ItemStack's NBT, matching ScrollItem's per-stack storage approach.
 */
public class QuestScrollItem extends Item {
    public enum State {
        ACTIVE, COMPLETED
    }

    private static final String TAG_QUEST_ID = "QuestId";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_DAY_OBTAINED = "DayObtained";
    private static final String TAG_TARGET_DAY = "TargetDay";
    private static final String TAG_DURATION_DAYS = "DurationDays";
    private static final String TAG_STATE = "State";
    private static final String TAG_COLLECT_PEAK = "CollectPeak";

    public QuestScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getState(stack) == State.COMPLETED;
    }

    public static ItemStack create(Quest quest, ServerPlayer owner) {
        // A re-taken quest must not be immediately self-removed by a stale abandoned flag
        // left over from a previous instance of the same quest.
        QuestSessionRegistry.clearAbandoned(owner.getUUID(), quest.id());

        ItemStack stack = new ItemStack(ModItems.QUEST_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        long dayObtained = WorldState.currentDay(owner.level());
        tag.putString(TAG_QUEST_ID, quest.id().toString());
        tag.putInt(TAG_TIER, quest.tier());
        tag.putUUID(TAG_OWNER_UUID, owner.getUUID());
        tag.putString(TAG_OWNER_NAME, owner.getName().getString());
        tag.putInt(TAG_PROGRESS, 0);
        tag.putLong(TAG_DAY_OBTAINED, dayObtained);
        tag.putLong(TAG_TARGET_DAY, dayObtained + quest.durationDays());
        tag.putInt(TAG_DURATION_DAYS, quest.durationDays());
        tag.putString(TAG_STATE, State.ACTIVE.name());
        // Collect-item quests track progress by the player's highest-ever held count of the
        // target since accepting the quest, not by raw pickup events - otherwise dropping an
        // already-owned item and picking it back up would farm free progress. Seeding this to
        // whatever they already own means only genuinely new items above that count, and above
        // any later peak, ever grant progress. See QuestTracker.onItemPickup.
        if (quest.objective() instanceof Quest.CollectItemObjective objective) {
            tag.putInt(TAG_COLLECT_PEAK, countMatching(owner, objective));
        }
        ItemNbt.set(stack, tag);
        return stack;
    }

    /** Sums stack sizes across the player's inventory for every stack the objective matches. */
    public static int countMatching(Player player, Quest.CollectItemObjective objective) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (objective.matches(stack)) total += stack.getCount();
        }
        return total;
    }

    /** Shrinks matching stacks across the player's inventory until `amount` total count is removed. */
    public static void removeMatching(Player player, Quest.CollectItemObjective objective, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) break;
            if (stack.isEmpty() || !objective.matches(stack)) continue;
            int take = Math.min(stack.getCount(), amount);
            stack.shrink(take);
            amount -= take;
        }
    }

    /** A minimal stack carrying only Tier/State tags, used as a villager trade cost template. */
    public static ItemStack createTemplate(int tier, State state) {
        ItemStack stack = new ItemStack(ModItems.QUEST_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_TIER, tier);
        tag.putString(TAG_STATE, state.name());
        ItemNbt.set(stack, tag);
        if (state == State.COMPLETED) {
            // Villager trade costs match on this component; see GuildReceptionistTrades.
            stack.set(ModDataComponents.QUEST_GRADE.get(), tier);
        }
        return stack;
    }

    public static Optional<Quest> getQuest(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_QUEST_ID)) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_QUEST_ID));
        return id == null ? Optional.empty() : Optional.ofNullable(QuestRegistry.get(id));
    }

    public static int getProgress(ItemStack stack) {
        return ItemNbt.get(stack).getInt(TAG_PROGRESS);
    }

    /** The highest total count of the target item this scroll's owner has held since accepting it. */
    public static int getCollectPeak(ItemStack stack) {
        return ItemNbt.get(stack).getInt(TAG_COLLECT_PEAK);
    }

    public static void setCollectPeak(ItemStack stack, int value) {
        ItemNbt.mutate(stack, t -> t.putInt(TAG_COLLECT_PEAK, value));
    }

    public static State getState(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_STATE)) {
            // A villager trade cost reaches the client, and comes back out of villager save data,
            // with no CUSTOM_DATA at all: ItemCost only stores its item, count and component
            // predicate, and rebuilds the displayed stack from those. QUEST_GRADE is only ever
            // stamped on a completed scroll, so it is what identifies one of those rebuilt costs.
            return stack.has(ModDataComponents.QUEST_GRADE.get())
                    ? State.COMPLETED
                    : State.ACTIVE;
        }
        try {
            return State.valueOf(tag.getString(TAG_STATE));
        } catch (IllegalArgumentException e) {
            return State.ACTIVE;
        }
    }

    /**
     * Tier from the cached NBT tag if present (trade templates), then from QUEST_GRADE for a
     * trade cost that lost its CUSTOM_DATA on the way through ItemCost, otherwise from the quest
     * definition. GuiGraphicsMixin draws this number onto the item slot, so the component
     * fallback is what puts the tier back on a receptionist's trade costs.
     */
    public static int getTier(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.contains(TAG_TIER)) return tag.getInt(TAG_TIER);
        Integer grade = stack.get(ModDataComponents.QUEST_GRADE.get());
        if (grade != null) return grade;
        return getQuest(stack).map(Quest::tier).orElse(0);
    }

    public static boolean hasActiveScroll(Player player, ResourceLocation questId) {
        for (ItemStack stack : player.getInventory().items) {
            if (matchesActive(stack, questId)) return true;
        }
        return false;
    }

    public static boolean removeActiveScroll(ServerPlayer player, ResourceLocation questId) {
        if (!hasActiveScroll(player, questId)) return false;
        abandonQuest(player, questId);
        return true;
    }

    private static boolean matchesActive(ItemStack stack, ResourceLocation questId) {
        if (!(stack.getItem() instanceof QuestScrollItem)) return false;
        if (getState(stack) != State.ACTIVE) return false;
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_QUEST_ID)) return false;
        return questId.toString().equals(tag.getString(TAG_QUEST_ID));
    }

    /** Writes the new progress back to the stack. Returns true if this increment completed the quest. */
    public static boolean incrementProgress(ItemStack stack, int amount) {
        Optional<Quest> questOpt = getQuest(stack);
        if (questOpt.isEmpty()) return false;
        int newProgress = ItemNbt.get(stack).getInt(TAG_PROGRESS) + amount;
        boolean completed = newProgress >= questOpt.get().objective().count();
        ItemNbt.mutate(stack, t -> {
            t.putInt(TAG_PROGRESS, newProgress);
            if (completed) {
                t.putString(TAG_STATE, State.COMPLETED.name());
            }
        });
        if (completed) {
            // Villager trade costs match on this component, not on CUSTOM_DATA, which also carries
            // per-quest data that differs on every scroll. See GuildReceptionistTrades.
            stack.set(ModDataComponents.QUEST_GRADE.get(), getTier(stack));
        }
        return completed;
    }

    /**
     * The single abandon path for both the board's Abandon button and expiry. Marks the
     * (player, quest) pair abandoned in the session registry and sweeps every matching ACTIVE
     * scroll out of the inventory now; the periodic sweep (see sweepAbandoned) catches copies
     * that were elsewhere at the time, e.g. in a chest. Also starts a re-take cooldown as long
     * as the quest's own duration.
     */
    public static void abandonQuest(ServerPlayer player, ResourceLocation questId) {
        Quest quest = QuestRegistry.get(questId);
        String name = quest != null ? quest.name().getString() : "?";
        player.sendSystemMessage(Component.translatable("msg.creraces.quest_abandoned", name)
                .withStyle(ChatFormatting.YELLOW));

        QuestSessionRegistry.markAbandoned(player.getUUID(), questId);
        if (quest != null) {
            long currentDay = WorldState.currentDay(player.level());
            QuestSessionRegistry.startCooldown(player.getUUID(), questId, currentDay + quest.durationDays());
        }
        sweepAbandoned(player);
    }

    /**
     * Removes every ACTIVE scroll in the player's inventory whose quest is flagged abandoned.
     * Silent: abandonQuest sends the message once, not once per removed copy.
     */
    public static void sweepAbandoned(ServerPlayer player) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!(stack.getItem() instanceof QuestScrollItem) || getState(stack) != State.ACTIVE) continue;
            Optional<Quest> quest = getQuest(stack);
            if (quest.isPresent() && QuestSessionRegistry.isAbandoned(player.getUUID(), quest.get().id())) {
                items.set(i, ItemStack.EMPTY);
            }
        }
    }

    /** Checks tamper-corrected expiration for a single active scroll stack; abandons it if expired. */
    public static void tick(ServerPlayer player, ItemStack stack) {
        if (getState(stack) != State.ACTIVE) return;
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_TARGET_DAY) || !tag.contains(TAG_QUEST_ID)) return;

        long currentDay = WorldState.currentDay(player.level());
        long targetDay = tag.getLong(TAG_TARGET_DAY);
        int durationDays = tag.getInt(TAG_DURATION_DAYS);

        // Tamper check: an admin rewinding time (e.g. /time set 0) must not grant free time.
        if (targetDay - currentDay > durationDays) {
            long correctedDay = currentDay + durationDays;
            ItemNbt.mutate(stack, t -> t.putLong(TAG_TARGET_DAY, correctedDay));
            targetDay = correctedDay;
        }

        if (currentDay >= targetDay) {
            ResourceLocation questId = ResourceLocation.tryParse(tag.getString(TAG_QUEST_ID));
            if (questId != null) abandonQuest(player, questId);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<Quest> questOpt = getQuest(stack);
        if (questOpt.isPresent()) {
            Quest quest = questOpt.get();
            tooltip.add(Component.translatable("gui.creraces.quest_board.objective",
                    quest.objective().verb(), quest.objective().count(), quest.objective().targetName())
                    .withStyle(ChatFormatting.GRAY));

            int progress = getProgress(stack);
            int count = quest.objective().count();
            tooltip.add(Component.translatable("tooltip.creraces.quest_progress", progress, count)
                    .withStyle(ChatFormatting.GRAY));

            CompoundTag tag = ItemNbt.get(stack);
            if (tag.contains(TAG_OWNER_NAME)) {
                tooltip.add(Component.translatable("tooltip.creraces.quest_owner", tag.getString(TAG_OWNER_NAME))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            // TooltipContext carries no level, so the day count comes from the client level.
            Level clientLevel = ClientAccess.getLevel();
            if (getState(stack) == State.COMPLETED) {
                tooltip.add(Component.translatable("tooltip.creraces.quest_completed")
                        .withStyle(ChatFormatting.GREEN));
            } else if (clientLevel != null) {
                long currentDay = WorldState.currentDay(clientLevel);
                long targetDay = tag.getLong(TAG_TARGET_DAY);
                long daysLeft = Math.max(0, targetDay - currentDay);
                ChatFormatting color = daysLeft <= 1 ? ChatFormatting.RED : ChatFormatting.YELLOW;
                tooltip.add(Component.translatable("tooltip.creraces.quest_days_left", daysLeft).withStyle(color));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
