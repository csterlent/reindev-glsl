package net.mine_diver.glsl.mixin;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import net.mine_diver.glsl.Shaders;
import net.mine_diver.glsl.util.TessellatorAccessor;
import net.minecraft.src.client.renderer.GLAllocation;
import net.minecraft.src.client.renderer.Tessellator;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Tessellator.class})
public class MixinTessellator implements TessellatorAccessor {
  @Shadow
  private int drawMode;

  // @Shadow // convertQuadsToTriangles, missing in Rind. Presumably true though
  // private static boolean field_2055;

  @Shadow
  private int addedVertices;

  // @Shadow
  // private boolean hasNormals; missing in Rind 2.8 presumably the second true

  // since hasNormals is unavailable, I might use (isDrawing && !renderingChunk)
  @Shadow
  private boolean renderingChunk;
  @Shadow
  private boolean isDrawing;

  @Shadow // rawBuffer
  private int[] rawBuffer;

  @Shadow
  private int rawBufferIndex;

  public ByteBuffer shadersBuffer;

  public ShortBuffer shadersShortBuffer;

  public short[] shadersData;

  @Inject(method = {"<init>(I)V"}, at = {@At("RETURN")})
  private void onCor(int var1, CallbackInfo ci) {
    this.shadersData = new short[] { -1, 0 };
    this.shadersBuffer = GLAllocation.createDirectByteBuffer(var1 / 8 * 4);
    this.shadersShortBuffer = this.shadersBuffer.asShortBuffer();
  }

  @Redirect(method = {"draw()V"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDrawArrays(III)V"))
  private void onDraw(int mode, int first, int vertexCount) {
    if (Shaders.entityAttrib >= 0) {
      ARBVertexProgram.glEnableVertexAttribArrayARB(Shaders.entityAttrib);
      ARBVertexProgram.glVertexAttribPointerARB(Shaders.entityAttrib, 2, false, false, 4, (ShortBuffer)this.shadersShortBuffer.position(0));
    }
    GL11.glDrawArrays(mode, first, vertexCount);
    if (Shaders.entityAttrib >= 0)
      ARBVertexProgram.glDisableVertexAttribArrayARB(Shaders.entityAttrib);
  }

  @Inject(method = {"reset()V"}, at = {@At("RETURN")})
  private void onReset(CallbackInfo ci) {
    this.shadersBuffer.clear();
  }

  @Inject(method = {"addVertex(DDD)V"}, at = {@At("HEAD")})
  private void onAddVertex(CallbackInfo ci) {
    // if (this.drawMode == 7 && true && (this.addedVertices + 1) % 4 == 0 && (isDrawing)) {
    //   this.rawBuffer[this.rawBufferIndex + 6] = this.rawBuffer[this.rawBufferIndex - 24 + 6];
    //   this.shadersBuffer.putShort(this.shadersData[0]).putShort(this.shadersData[1]);
    //   this.rawBuffer[this.rawBufferIndex + 8 + 6] = this.rawBuffer[this.rawBufferIndex + 8 - 16 + 6];
    //   this.shadersBuffer.putShort(this.shadersData[0]).putShort(this.shadersData[1]);
    // }
    this.shadersBuffer.putShort(this.shadersData[0]).putShort(this.shadersData[1]);
  }

  // allow for setting normal when drawing chunks
  public void setNormal(float x, float y, float z) {
    if (!this.isDrawing)
      System.out.println("Error: Not drawing !!!"); 
    byte xb = (byte)(int)(x * 128.0F);
    byte yb = (byte)(int)(y * 127.0F);
    byte zb = (byte)(int)(z * 127.0F);
    GL11.glNormal3b(xb, yb, zb);
  }

  public void setEntity(int id) {
    this.shadersData[0] = (short)id;
  }
}
