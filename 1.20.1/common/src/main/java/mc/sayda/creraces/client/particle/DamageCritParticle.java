package mc.sayda.creraces.client.particle;
 
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
 
public class DamageCritParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
 
    protected DamageCritParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        float colorRand = (float)(Math.random() * 0.3D + 0.6D);
        this.rCol = colorRand;
        this.gCol = colorRand;
        this.bCol = colorRand;

        this.quadSize *= 0.75F;
        this.lifetime = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
        this.hasPhysics = false;
        this.setSpriteFromAge(spriteSet);
    }
 
    @Override
    public float getQuadSize(float partialTicks) {
        float f = ((float)this.age + partialTicks) / (float)this.lifetime;
        return this.quadSize * (1.0F - f * f * 0.5F);
    }
 
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.spriteSet);
        }
    }
 
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
 
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
 
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
 
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz) {
            return new DamageCritParticle(world, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}
