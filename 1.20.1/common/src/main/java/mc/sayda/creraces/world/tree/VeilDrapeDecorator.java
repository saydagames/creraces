package mc.sayda.creraces.world.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public class VeilDrapeDecorator extends TreeDecorator {

    public static final Codec<VeilDrapeDecorator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("probability").forGetter(d -> d.probability),
            Codec.intRange(1, 16).fieldOf("min_length").forGetter(d -> d.minLength),
            Codec.intRange(1, 16).fieldOf("max_length").forGetter(d -> d.maxLength)
    ).apply(instance, VeilDrapeDecorator::new));

    public static TreeDecoratorType<VeilDrapeDecorator> TYPE;

    private final float probability;
    private final int minLength;
    private final int maxLength;

    public VeilDrapeDecorator(float probability, int minLength, int maxLength) {
        this.probability = probability;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TYPE;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        List<BlockPos> leaves = context.leaves();
        if (leaves.isEmpty()) return;

        RandomSource random = context.random();
        BlockState drapeState = ModBlocks.VEIL_WILLOW_DRAPE.get().defaultBlockState();
        int range = Math.max(1, maxLength - minLength + 1);

        // Build a map from each (x, z) column to the lowest leaf Y in that column.
        // Packing x and z into a long avoids boxing overhead.
        Map<Long, Integer> columnLowest = new HashMap<>();
        for (BlockPos leaf : leaves) {
            long key = pack(leaf.getX(), leaf.getZ());
            Integer prev = columnLowest.get(key);
            if (prev == null || leaf.getY() < prev) {
                columnLowest.put(key, leaf.getY());
            }
        }

        // A column is on the outer perimeter when at least one of its four orthogonal
        // neighbours has no leaves at all. This is the exact silhouette of the canopy
        // footprint. No radius maths, no centre coordinates - works for any canopy shape.
        for (Map.Entry<Long, Integer> entry : columnLowest.entrySet()) {
            long key = entry.getKey();
            int x = xOf(key);
            int z = zOf(key);

            boolean outer = !columnLowest.containsKey(pack(x - 1, z))
                         || !columnLowest.containsKey(pack(x + 1, z))
                         || !columnLowest.containsKey(pack(x, z - 1))
                         || !columnLowest.containsKey(pack(x, z + 1));
            if (!outer) continue;
            if (random.nextFloat() >= probability) continue;

            // Hang drapes straight down from below the lowest leaf in this column.
            int lowestY = entry.getValue();
            int len = minLength + random.nextInt(range);

            for (int i = 1; i <= len; i++) {
                BlockPos drapePos = new BlockPos(x, lowestY - i, z);
                if (!context.isAir(drapePos)) break;
                context.setBlock(drapePos, drapeState);
            }
        }
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | ((long) z & 0xFFFFFFFFL);
    }

    private static int xOf(long key) {
        return (int) (key >> 32);
    }

    private static int zOf(long key) {
        return (int) key;
    }
}
