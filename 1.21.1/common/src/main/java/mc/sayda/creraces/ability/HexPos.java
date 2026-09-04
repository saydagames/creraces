package mc.sayda.creraces.ability;

import net.minecraft.network.FriendlyByteBuf;

public record HexPos(int q, int r) {
    public static HexPos decode(FriendlyByteBuf buf) {
        return new HexPos(buf.readByte(), buf.readByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(q);
        buf.writeByte(r);
    }
}
