package mc.sayda.creraces.race;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ResourceType {
    NONE(0xFFFFFF, "None"),
    MANA(0x3366CC, "Mana"),
    RAGE(0xCC3333, "Rage"),
    ENERGY(0xFFCC33, "Energy"),
    GRIT(0x666666, "Grit"),
    SOULS(0x9933CC, "Souls"),
    STACKS(0x580000, "Stacks");

    private final int color;
    private final String displayName;

    ResourceType(int color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.literal(displayName).withStyle(style -> style.withColor(color));
    }
}
