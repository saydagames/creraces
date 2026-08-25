package mc.sayda.creraces.ability;

import net.minecraft.util.StringRepresentable;

public enum EssenceType implements StringRepresentable {
    FIRE   ("fire",   0xFF1A00),
    WATER  ("water",  0x2255FF),
    EARTH  ("earth",  0xAA7744),
    AIR    ("air",    0xDDEEFF),
    LIGHT  ("light",  0xFFDD44),
    DARK   ("dark",   0x4D1A88),
    LIFE   ("life",   0x00CC66),
    DEATH  ("death",  0x444444),
    VOID   ("void",   0x220033),
    ARCANE ("arcane", 0x8822CC),
    SOUL   ("soul",   0x00D4C8);

    private final String id;
    private final int color;

    EssenceType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static EssenceType byId(String id) {
        for (EssenceType t : values()) {
            if (t.id.equals(id)) return t;
        }
        throw new IllegalArgumentException("Unknown essence type: " + id);
    }
}
