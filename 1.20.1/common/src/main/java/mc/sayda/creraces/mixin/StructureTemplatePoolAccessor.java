package mc.sayda.creraces.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Exposes StructureTemplatePool's private template lists so a village pool can be mutated directly instead of overriding the whole datapack file. */
@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {
    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> creraces$getRawTemplates();

    @Accessor("rawTemplates")
    @Mutable
    void creraces$setRawTemplates(List<Pair<StructurePoolElement, Integer>> rawTemplates);

    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> creraces$getTemplates();
}
