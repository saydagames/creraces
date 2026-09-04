package mc.sayda.creraces.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry for all loaded quests.
 * Populated by QuestManager during resource reload.
 */
public class QuestRegistry {
    private static final Map<ResourceLocation, Quest> QUESTS = new ConcurrentHashMap<>();

    public static void register(Quest quest) {
        QUESTS.put(quest.id(), quest);
    }

    public static Quest get(ResourceLocation id) {
        return QUESTS.get(id);
    }

    public static Collection<Quest> getAll() {
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static boolean exists(ResourceLocation id) {
        return QUESTS.containsKey(id);
    }

    public static void clear() {
        QUESTS.clear();
    }
}
