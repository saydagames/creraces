package mc.sayda.creraces.quest;

import mc.sayda.creraces.item.QuestScrollItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives quest objective progress and expiration. Each hook scans the player's inventory
 * for active quest scrolls whose objective matches, rather than tracking state elsewhere -
 * the scroll ItemStack is the single source of truth for a quest's progress.
 */
public class QuestTracker {

    public static void onPlayerKill(ServerPlayer killer, LivingEntity victim) {
        forEachActiveScroll(killer, stack -> {
            Quest quest = QuestScrollItem.getQuest(stack).orElse(null);
            if (quest == null || !(quest.objective() instanceof Quest.KillEntityObjective objective)) return;
            if (!objective.matches(victim)) return;
            progress(killer, stack, quest, 1);
        });
    }

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        forEachActiveScroll(player, stack -> {
            Quest quest = QuestScrollItem.getQuest(stack).orElse(null);
            if (quest == null || !(quest.objective() instanceof Quest.MineBlockObjective objective)) return;
            if (!objective.matches(state)) return;
            progress(player, stack, quest, 1);
        });
    }

    /** Credits progress off the player's peak held count since accepting, not raw pickups - stops drop/re-pickup farming. */
    public static void onItemPickup(ServerPlayer player, ItemStack picked) {
        forEachActiveScroll(player, stack -> {
            Quest quest = QuestScrollItem.getQuest(stack).orElse(null);
            if (quest == null || !(quest.objective() instanceof Quest.CollectItemObjective objective)) return;
            if (!objective.matches(picked)) return;
            creditCollectProgress(player, stack, quest, objective);
        });
    }

    /** Periodic fallback for pickups that don't fire PICKUP_ITEM_POST (furnace output, chests, trades). */
    public static void recheckCollectProgress(ServerPlayer player) {
        forEachActiveScroll(player, stack -> {
            Quest quest = QuestScrollItem.getQuest(stack).orElse(null);
            if (quest == null || !(quest.objective() instanceof Quest.CollectItemObjective objective)) return;
            creditCollectProgress(player, stack, quest, objective);
        });
    }

    private static void creditCollectProgress(ServerPlayer player, ItemStack stack, Quest quest,
            Quest.CollectItemObjective objective) {
        int total = QuestScrollItem.countMatching(player, objective);
        int peak = QuestScrollItem.getCollectPeak(stack);
        if (total <= peak) return;

        int remaining = objective.count() - QuestScrollItem.getProgress(stack);
        int delta = Math.min(total - peak, remaining);
        QuestScrollItem.setCollectPeak(stack, peak + delta);
        if (delta <= 0) return;

        progress(player, stack, quest, delta);
        if (!mc.sayda.creraces.config.CreRacesConfig.KEEP_QUEST_ITEMS.get()) {
            // The items just credited are consumed instead of kept, so the peak must be
            // rebaselined to the post-removal total - otherwise it would sit above the
            // player's actual held count and silently block all future progress.
            QuestScrollItem.removeMatching(player, objective, delta);
            QuestScrollItem.setCollectPeak(stack, QuestScrollItem.countMatching(player, objective));
        }
    }

    public static void tickQuests(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof mc.sayda.creraces.item.QuestScrollItem) {
                QuestScrollItem.tick(player, stack);
            }
        }
        // Safety net: catches any scroll matching an already-abandoned quest that wasn't in
        // the inventory at the moment abandonQuest() ran its immediate sweep (e.g. pulled out
        // of storage afterward).
        QuestScrollItem.sweepAbandoned(player);
    }

    private static void progress(ServerPlayer player, ItemStack stack, Quest quest, int amount) {
        boolean completed = QuestScrollItem.incrementProgress(stack, amount);
        int progress = QuestScrollItem.getProgress(stack);
        int count = quest.objective().count();
        if (completed) {
            player.sendSystemMessage(Component.translatable("msg.creraces.quest_completed", quest.name())
                    .withStyle(ChatFormatting.GREEN));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else {
            player.sendSystemMessage(Component.translatable("msg.creraces.quest_progress",
                    quest.name(), progress, count).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void forEachActiveScroll(ServerPlayer player, java.util.function.Consumer<ItemStack> action) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof mc.sayda.creraces.item.QuestScrollItem
                    && QuestScrollItem.getState(stack) == QuestScrollItem.State.ACTIVE) {
                action.accept(stack);
            }
        }
    }
}
