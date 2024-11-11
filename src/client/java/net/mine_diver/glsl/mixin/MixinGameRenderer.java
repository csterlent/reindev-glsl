package net.mine_diver.glsl.mixin;

import net.mine_diver.glsl.Shaders;
import net.minecraft.src.game.entity.EntityLiving;
import net.minecraft.src.client.renderer.RenderGlobal;
import net.minecraft.src.client.renderer.EntityRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public abstract class MixinGameRenderer {
  @Shadow
  private Minecraft mc;

  @Shadow
  float fogColorRed;

  @Shadow
  float fogColorGreen;

  @Shadow
  float fogColorBlue;

  @Shadow
  protected abstract void renderHand(float paramFloat);

  @Shadow
  protected abstract void renderRainSnow(float paramFloat);

  @Shadow
  protected abstract void setupCameraTransform(float paramFloat);

  @Inject(method = {"renderWorld(FJ)V"}, at = {@At("HEAD")})
  private void beginRender(float var1, long var2, CallbackInfo ci) {
    Shaders.beginRender(this.mc, var1, var2);
  }

  @Inject(method = {"renderWorld(FJ)V"}, at = {@At("RETURN")})
  private void endRender(CallbackInfo ci) {
    Shaders.endRender();
  }

  @Redirect(method = {"renderWorld(FJ)V"}, at = @At(value = "INVOKE", target = "setupCameraTransform"))
  private void setClearColor(EntityRenderer gameRenderer, float var1) {
    Shaders.setClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue);
    setupCameraTransform(var1);
    Shaders.setCamera(var1);
  }

  @Redirect(method = {"renderWorld(FJ)V"}, at = @At(value = "INVOKE", target = "sortAndRender"))
  private int beginTerrain(RenderGlobal worldRenderer, EntityLiving var4, int i, double var1) {
    int ret;
    if (i == 0) {
      Shaders.beginTerrain();
      ret = worldRenderer.sortAndRender(var4, i, var1);
      Shaders.endTerrain();
    } else if (i == 1) {
      Shaders.beginWater();
      ret = worldRenderer.sortAndRender(var4, i, var1);
      Shaders.endWater();
    } else {
      ret = worldRenderer.sortAndRender(var4, i, var1);
    }
    return ret;
  }

  // renderWorld => missing renderAllRenderLists
  // @Redirect(method = {"renderWorld(FJ)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;method_1540(ID)V"))
  // private void beginWater(RenderGlobal worldRenderer, int i, double var1) {
  //   Shaders.beginWater();
  //   worldRenderer.method_1540(i, var1);
  //   Shaders.endWater();
  // }

  @Redirect(method = {"renderWorld(FJ)V"}, at = @At(value = "INVOKE", target = "renderRainSnow"))
  private void beginWeather(EntityRenderer gameRenderer, float var1) {
    Shaders.beginWeather();
    renderRainSnow(var1);
    Shaders.endWeather();
  }

  @Redirect(method = {"renderWorld(FJ)V"}, at = @At(value = "INVOKE", target = "renderHand"))
  private void beginHand(EntityRenderer gameRenderer, float var1) {
    Shaders.beginHand();
    renderHand(var1);
    Shaders.endHand();
  }
}
