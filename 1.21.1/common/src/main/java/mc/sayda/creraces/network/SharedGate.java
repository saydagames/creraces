package mc.sayda.creraces.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * One gate travelling between two kitsune: where it is and what its sender calls it. Icon and
 * colour are the recipient's own to choose, so they are not sent.
 */
public record SharedGate(String dimension, BlockPos pos, String name) {
    public static final int DIMENSION_MAX_LEN = 64;
    public static final int NAME_MAX_LEN = 48;
    /** Hard cap on gates in a single packet; the sender splits larger shares across packets. */
    public static final int MAX_PER_PACKET = 64;

    public static SharedGate read(FriendlyByteBuf buf) {
        return new SharedGate(buf.readUtf(DIMENSION_MAX_LEN), buf.readBlockPos(), buf.readUtf(NAME_MAX_LEN));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(dimension, DIMENSION_MAX_LEN);
        buf.writeBlockPos(pos);
        buf.writeUtf(name.length() > NAME_MAX_LEN ? name.substring(0, NAME_MAX_LEN) : name, NAME_MAX_LEN);
    }

    public static List<SharedGate> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_PER_PACKET) {
            throw new IllegalStateException("Oversized gate share: " + size);
        }
        List<SharedGate> gates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            gates.add(read(buf));
        }
        return gates;
    }

    public static void writeList(FriendlyByteBuf buf, List<SharedGate> gates) {
        buf.writeVarInt(gates.size());
        for (SharedGate gate : gates) {
            gate.write(buf);
        }
    }
}
