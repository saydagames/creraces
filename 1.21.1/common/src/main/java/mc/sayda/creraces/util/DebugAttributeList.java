package mc.sayda.creraces.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class DebugAttributeList {
    public static void run() {
        System.out.println("--- Listing Registered Attributes ---");
        BuiltInRegistries.ATTRIBUTE.keySet().forEach(id -> {
            System.out.println("Found attribute: " + id.toString());
        });
        System.out.println("--- End of Attribute List ---");
    }
}
