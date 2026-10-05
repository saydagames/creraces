package mc.sayda.creraces.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** The drifting, breathing Veil ember mote, tinted with an essence type's colour. */
public class EssenceParticle extends VeilEmberParticle {

    protected EssenceParticle(ClientLevel level, double x, double y, double z,
            float r, float g, float b, SpriteSet spriteSet) {
        super(level, x, y, z, spriteSet);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            // xSpeed/ySpeed/zSpeed are repurposed as RGB color here, not velocity.
            return new EssenceParticle(level, x, y, z,
                    (float) xSpeed, (float) ySpeed, (float) zSpeed, spriteSet);
        }
    }
}
