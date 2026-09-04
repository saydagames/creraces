package mc.sayda.creraces.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class VeilMistParticle extends TextureSheetParticle {

    protected VeilMistParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);
        this.quadSize = 0.6f + level.random.nextFloat() * 0.8f;
        this.lifetime = 140 + level.random.nextInt(80);
        this.hasPhysics = false;
        this.xd = (level.random.nextDouble() - 0.5) * 0.006;
        this.yd = 0;
        this.zd = (level.random.nextDouble() - 0.5) * 0.006;
        this.alpha = 0f;
        this.pickSprite(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        float progress = (float) this.age / this.lifetime;
        this.alpha = progress < 0.15f
                ? progress / 0.15f * 0.55f
                : (progress > 0.7f ? (1.0f - progress) / 0.3f * 0.55f : 0.55f);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new VeilMistParticle(level, x, y, z, spriteSet);
        }
    }
}
