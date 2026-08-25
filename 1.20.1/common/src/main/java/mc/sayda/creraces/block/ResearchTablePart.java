package mc.sayda.creraces.block;

import net.minecraft.util.StringRepresentable;

public enum ResearchTablePart implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    ResearchTablePart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
