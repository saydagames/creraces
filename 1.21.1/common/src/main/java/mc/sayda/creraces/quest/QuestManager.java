package mc.sayda.creraces.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.utils.GameInstance;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncQuestsPacket;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads quest JSONs from data/creraces/quests/
 */
public class QuestManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "quests";
    private static volatile Map<ResourceLocation, JsonElement> lastRawData = new HashMap<>();

    public static SyncQuestsPacket createSyncPacket() {
        Map<ResourceLocation, String> data = new HashMap<>();
        lastRawData.forEach((id, element) -> data.put(id, element.toString()));
        return new SyncQuestsPacket(data);
    }

    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("QuestManager: Preparing data reload...");
        return GsonHelper.getJsonFiles(resourceManager, FOLDER);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("QuestManager: Applying data reload ({} files found)", data.size());
        lastRawData = data;
        syncFromServer(data);

        MinecraftServer server = GameInstance.getServer();
        if (server != null) {
            SyncQuestsPacket pkt = createSyncPacket();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BoundaryHandler.syncQuestsToPlayer(player, pkt);
            }
        }
    }

    /** Rebuilds the quest registry from raw JSON; runs on the server after a reload and on clients from the sync packet. */
    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        QuestRegistry.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;
            try {
                JsonObject json = entry.getValue().getAsJsonObject();

                int tier = GsonHelper.getAsInt(json, "tier", 1);
                String nameStr = GsonHelper.getAsString(json, "name", id.getPath());
                String descStr = GsonHelper.getAsString(json, "description", "");
                int durationDays = GsonHelper.getAsInt(json, "duration_days", 1);

                Quest.Objective objective = parseObjective(id, json.getAsJsonObject("objective"));
                if (objective == null) {
                    CreRaces.LOGGER.error("Quest {} has an invalid or missing objective; skipping.", id);
                    continue;
                }

                Quest quest = new Quest.Builder(id)
                        .tier(tier)
                        .name(Component.translatable(nameStr))
                        .description(Component.translatable(descStr))
                        .durationDays(durationDays)
                        .objective(objective)
                        .build();

                QuestRegistry.register(quest);
                count++;
            } catch (Exception e) {
                // Malformed JSON can throw a range of Gson/runtime exceptions; one bad quest must not stop the rest loading.
                CreRaces.LOGGER.error("Failed to load quest {}: ", id, e);
            }
        }

        CreRaces.LOGGER.info("Loaded {} quests.", count);
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
