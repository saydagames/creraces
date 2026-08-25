package mc.sayda.creraces.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
        public static final KeyMapping SKILL_WHEEL = new KeyMapping(
                        "key.creraces.skill_wheel",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_X,
                        "category.creraces.general");

        public static final KeyMapping ABILITY_A1 = new KeyMapping(
                        "key.creraces.ability_a1",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.creraces.general");

        public static final KeyMapping ABILITY_A2 = new KeyMapping(
                        "key.creraces.ability_a2",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_G,
                        "category.creraces.general");

        public static final KeyMapping ABILITY_A3 = new KeyMapping(
                        "key.creraces.ability_a3",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        "category.creraces.general");

        public static final KeyMapping ABILITY_A4 = new KeyMapping(
                        "key.creraces.ability_a4",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        "category.creraces.general");

        public static final KeyMapping ABILITY_A5 = new KeyMapping(
                        "key.creraces.ability_a5",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        "category.creraces.general");

        public static final KeyMapping MENU_GUI = new KeyMapping(
                        "key.creraces.menu_gui",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_I,
                        "category.creraces.general");

        public static final KeyMapping ESSENCE_BELT = new KeyMapping(
                        "key.creraces.essence_belt",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        "category.creraces.general");

        public static void register() {
                KeyMappingRegistry.register(SKILL_WHEEL);
                KeyMappingRegistry.register(ABILITY_A1);
                KeyMappingRegistry.register(ABILITY_A2);
                KeyMappingRegistry.register(ABILITY_A3);
                KeyMappingRegistry.register(ABILITY_A4);
                KeyMappingRegistry.register(ABILITY_A5);
                KeyMappingRegistry.register(MENU_GUI);
                KeyMappingRegistry.register(ESSENCE_BELT);
        }
}
