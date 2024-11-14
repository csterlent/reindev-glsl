package net.mine_diver.glsl.mixin;

import net.mine_diver.glsl.Shaders;
import net.mine_diver.glsl.util.TessellatorAccessor;
import net.minecraft.src.client.renderer.RenderBlocks;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.client.renderer.WorldRenderer;
import net.minecraft.src.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class Mixinclass_66 {
  @Redirect(method = {"updateRenderer"}, at = @At(value = "INVOKE", target = "renderBlockByRenderType"))
  private boolean onRenderBlockByRenderType(RenderBlocks tileRenderer, Block var24, int var17, int var15, int var16) {
    if (Shaders.entityAttrib >= 0)
      ((TessellatorAccessor)Tessellator.instance).setEntity(var24.blockID);
    return tileRenderer.renderBlockByRenderType(var24, var17, var15, var16);
  }

  @Inject(method = {"updateRenderer"}, at = {@At("RETURN")})
  private void onUpdateRenderer(CallbackInfo ci) {
    if (Shaders.entityAttrib >= 0)
      ((TessellatorAccessor)Tessellator.instance).setEntity(-1);
  }
}
