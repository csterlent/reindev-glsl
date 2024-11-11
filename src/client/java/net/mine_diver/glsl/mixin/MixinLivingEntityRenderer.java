package net.mine_diver.glsl.mixin;

import net.mine_diver.glsl.Shaders;
import net.minecraft.src.client.renderer.entity.RenderLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// RenderLiving
@Mixin({RenderLiving.class})
public class MixinLivingEntityRenderer {
  @Redirect(method = {"*"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
  private void onGlEnable(int i) {
    Shaders.glEnableWrapper(i);
  }
  
  @Redirect(method = {"*"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
  private void onGlDisable(int i) {
    Shaders.glDisableWrapper(i);
  }
}
