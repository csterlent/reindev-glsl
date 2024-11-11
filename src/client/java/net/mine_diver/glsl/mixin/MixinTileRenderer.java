package net.mine_diver.glsl.mixin;

import net.minecraft.src.client.renderer.RenderBlocks;
import net.minecraft.src.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// RenderBlocks
@Mixin({RenderBlocks.class})
public class MixinTileRenderer {
  @Inject(method = {"renderBottomFace"}, at = {@At("HEAD")})
  private void onRenderBottomFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(0.0F, -1.0F, 0.0F);
  }
  
  @Inject(method = {"renderTopFace"}, at = {@At("HEAD")})
  private void onRenderTopFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(0.0F, 1.0F, 0.0F);
  }
  
  @Inject(method = {"renderEastFace"}, at = {@At("HEAD")})
  private void onRenderEastFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(0.0F, 0.0F, -1.0F);
  }
  
  @Inject(method = {"renderWestFace"}, at = {@At("HEAD")})
  private void onRenderWestFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(0.0F, 0.0F, 1.0F);
  }
  
  @Inject(method = {"renderNorthFace"}, at = {@At("HEAD")})
  private void onRenderNorthFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(-1.0F, 0.0F, 0.0F);
  }
  
  @Inject(method = {"renderSouthFace"}, at = {@At("HEAD")})
  private void onRenderSouthFace(CallbackInfo ci) {
    Tessellator.instance.setNormal(1.0F, 0.0F, 0.0F);
  }
}
