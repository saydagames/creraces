package mc.sayda.creraces.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Wire format shared by the race, ability and quest syncs: an int count, then id/JSON pairs. */
final class JsonDefinitions {
    private static final int MAX_JSON_LENGTH = 262144;

    private JsonDefinitions() {
    }

    static Map<ResourceLocation, String> read(FriendlyByteBuf buf) {
        Map<ResourceLocation, String> definitions = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            definitions.put(buf.readResourceLocation(), buf.readUtf(MAX_JSON_LENGTH));
        }
        return definitions;
    }

    static void write(FriendlyByteBuf buf, Map<ResourceLocation, String> definitions) {
        buf.writeInt(definitions.size());
        definitions.forEach((id, json) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(json, MAX_JSON_LENGTH);
        });
    }
}
