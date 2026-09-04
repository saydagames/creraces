package mc.sayda.creraces.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Loads quest JSONs from data/creraces/quests/
 */
public class QuestManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "quests";
    private static volatile Map<ResourceLocation, JsonElement> lastRawData = new java.util.HashMap<>();

    public static mc.sayda.creraces.network.SyncQuestsPacket createSyncPacket() {
        Map<ResourceLocation, String> data = new java.util.HashMap<>();
        lastRawData.forEach((id, element) -> data.put(id, element.toString()));
        return new mc.sayda.creraces.network.SyncQuestsPacket(data);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("QuestManager: Preparing data reload...");
        Map<?, ?> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? (Map<ResourceLocation, JsonElement>) files : new java.util.HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("QuestManager: Applying data reload ({} files found)", data.size());
        lastRawData = data;
        syncFromServer(data);

        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            var pkt = createSyncPacket();
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                mc.sayda.creraces.network.BoundaryHandler.syncQuestsToPlayer(player, pkt);
            }
        }
    }

    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        QuestRegistry.clear();
        final int[] count = {0};

        data.forEach((id, element) -> {
            if (!element.isJsonObject()) return;
            try {
                JsonObject json = element.getAsJsonObject();

                int tier = GsonHelper.getAsInt(json, "tier", 1);
                String nameStr = GsonHelper.getAsString(json, "name", id.getPath());
                String descStr = GsonHelper.getAsString(json, "description", "");
                int durationDays = GsonHelper.getAsInt(json, "duration_days", 1);

                Quest.Objective objective = parseObjective(id, json.getAsJsonObject("objective"));
                if (objective == null) {
                    CreRaces.LOGGER.error("Quest {} has an invalid or missing objective; skipping.", id);
                    return;
                }

                Quest quest = new Quest.Builder(id)
                        .tier(tier)
                        .name(Component.translatable(nameStr))
                        .description(Component.translatable(descStr))
                        .durationDays(durationDays)
                        .objective(objective)
                        .build();

                QuestRegistry.register(quest);
                count[0]++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load quest {}: ", id, e);
            }
        });

        CreRaces.LOGGER.info("Loaded {} quests.", count[0]);
    }

    private static Quest.Objective parseObjective(ResourceLocation questId, JsonObject obj) {
        if (obj == null) return null;
        String type = GsonHelper.getAsString(obj, "type", "");
        String targetStr = GsonHelper.getAsString(obj, "target", "");
        int count = GsonHelper.getAsInt(obj, "count", 1);
        Quest.TargetRef target = Quest.TargetRef.parse(targetStr);
        if (target.id == null) {
            CreRaces.LOGGER.error("Quest {} objective has an invalid target: {}", questId, targetStr);
            return null;
        }

        return switch (type) {
            case "kill_entity" -> new Quest.KillEntityObjective(
                    target.isTag ? null : target.id,
                    target.isTag ? Quest.entityTag(target.id) : null,
                    count);
            case "mine_block" -> new Quest.MineBlockObjective(
                    target.isTag ? null : target.id,
                    target.isTag ? Quest.blockTag(target.id) : null,
                    count);
            case "collect_item" -> new Quest.CollectItemObjective(
                    target.isTag ? null : target.id,
                    target.isTag ? Quest.itemTag(target.id) : null,
                    count);
            default -> {
                CreRaces.LOGGER.error("Quest {} has unknown objective type: {}", questId, type);
                yield null;
            }
        };
    }
}
