package mc.sayda.creraces.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    private static final String CATEGORY = "category.creraces.general";
    private static final int UNBOUND = InputConstants.UNKNOWN.getValue();

    public static final KeyMapping SKILL_WHEEL = key("skill_wheel", GLFW.GLFW_KEY_X);
    public static final KeyMapping ABILITY_A1 = key("ability_a1", GLFW.GLFW_KEY_R);
    public static final KeyMapping ABILITY_A2 = key("ability_a2", GLFW.GLFW_KEY_G);
    public static final KeyMapping ABILITY_A3 = key("ability_a3", UNBOUND);
    public static final KeyMapping ABILITY_A4 = key("ability_a4", UNBOUND);
    public static final KeyMapping ABILITY_A5 = key("ability_a5", UNBOUND);
    public static final KeyMapping MENU_GUI = key("menu_gui", GLFW.GLFW_KEY_I);
    public static final KeyMapping ESSENCE_BELT = key("essence_belt", UNBOUND);
    public static final KeyMapping WAYPOINT_TOGGLE = key("waypoint_toggle", UNBOUND);

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.creraces." + name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    public static void register() {
        KeyMappingRegistry.register(SKILL_WHEEL);
        KeyMappingRegistry.register(ABILITY_A1);
        KeyMappingRegistry.register(ABILITY_A2);
        KeyMappingRegistry.register(ABILITY_A3);
        KeyMappingRegistry.register(ABILITY_A4);
        KeyMappingRegistry.register(ABILITY_A5);
        KeyMappingRegistry.register(MENU_GUI);
        KeyMappingRegistry.register(ESSENCE_BELT);
        KeyMappingRegistry.register(WAYPOINT_TOGGLE);
    }
}
