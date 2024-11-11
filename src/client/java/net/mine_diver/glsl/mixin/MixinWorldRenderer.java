package net.mine_diver.glsl.mixin;

import net.mine_diver.glsl.Shaders;
import net.minecraft.src.game.level.World;
import net.minecraft.src.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RenderGlobal.class})
public class MixinWorldRenderer {
  @Redirect(method = {"renderSky(F)V"}, at = @At(value = "INVOKE", target = "getStarBrightness"))
  private float onGetStarBrightness(World world, float f) {
    float ret = world.getStarBrightness(f);
    Shaders.setCelestialPosition();
    return ret;
  }
  
  @Redirect(method = {"*"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
  private void onGlEnable(int i) {
    Shaders.glEnableWrapper(i);
  }
  
  @Redirect(method = {"*"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
  private void onGlDisable(int i) {
    Shaders.glDisableWrapper(i);
  }
}
