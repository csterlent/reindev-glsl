package net.mine_diver.glsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.src.game.entity.EntityLiving;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.GLU;

public class Shaders {
  public static void init() {
    int maxDrawBuffers = GL11.glGetInteger(34852);
    System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);
    colorAttachments = 4;
    int i;
    for (i = 0; i < 10; i++) {
      if (programNames[i] == "") {
        programs[i] = 0;
      } else {
        programs[i] = setupProgram("shaders/" + programNames[i] + ".vsh", "shaders/" + programNames[i] + ".fsh");
      }
    }
    if (colorAttachments > maxDrawBuffers)
      System.out.println("Not enough draw buffers!");
    for (i = 0; i < 10; i++) {
      for (int n = i; programs[i] == 0 &&
        n != programBackups[n]; n = programBackups[n])
        programs[i] = programs[programBackups[n]];
    }
    dfbDrawBuffers = BufferUtils.createIntBuffer(colorAttachments);
    for (i = 0; i < colorAttachments; i++)
      dfbDrawBuffers.put(i, 36064 + i);
    dfbTextures = BufferUtils.createIntBuffer(colorAttachments);
    dfbRenderBuffers = BufferUtils.createIntBuffer(colorAttachments);
    resize();
    setupShadowMap();
    isInitialized = true;
  }

  public static void destroy() {
    for (int i = 0; i < 10; i++) {
      if (programs[i] != 0) {
        ARBShaderObjects.glDeleteObjectARB(programs[i]);
        programs[i] = 0;
      }
    }
  }

  public static void glEnableWrapper(int cap) {
    GL11.glEnable(cap);
    if (cap == 3553) {
      if (activeProgram == 1)
        useProgram(lightmapEnabled ? 3 : 2);
    } else if (cap == 2912) {
      fogEnabled = true;
      setProgramUniform1i("fogMode", GL11.glGetInteger(2917));
    }
  }

  public static void glDisableWrapper(int cap) {
    GL11.glDisable(cap);
    if (cap == 3553) {
      if (activeProgram == 2 || activeProgram == 3)
        useProgram(1);
    } else if (cap == 2912) {
      fogEnabled = false;
      setProgramUniform1i("fogMode", 0);
    }
  }

  public static void enableLightmap() {
    lightmapEnabled = true;
    if (activeProgram == 2)
      useProgram(3);
  }

  public static void disableLightmap() {
    lightmapEnabled = false;
    if (activeProgram == 3)
      useProgram(2);
  }

  public static void setClearColor(float red, float green, float blue) {
    clearColor[0] = red;
    clearColor[1] = green;
    clearColor[2] = blue;
    if (isShadowPass) {
      GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0F);
      GL11.glClear(16640);
      return;
    }
    GL20.glDrawBuffers(dfbDrawBuffers);
    GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
    GL11.glClear(16640);
    GL20.glDrawBuffers(36064);
    GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0F);
    GL11.glClear(16640);
    GL20.glDrawBuffers(36065);
    GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glClear(16640);
    GL20.glDrawBuffers(dfbDrawBuffers);
  }

  public static void setCamera(float f) {
    EntityLiving viewEntity = mc.renderViewEntity;
    double x = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * f;
    double y = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * f;
    double z = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * f;
    if (isShadowPass) {
      GL11.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      if (shadowMapIsOrtho) {
        GL11.glOrtho(-shadowMapHalfPlane, shadowMapHalfPlane, -shadowMapHalfPlane, shadowMapHalfPlane, 0.05000000074505806D, 256.0D);
      } else {
        GLU.gluPerspective(shadowMapFOV, shadowMapWidth / shadowMapHeight, 0.05F, 256.0F);
      }
      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glTranslatef(0.0F, 0.0F, -100.0F);
      GL11.glRotatef(90.0F, 0.0F, 0.0F, -1.0F);
      float angle = mc.theWorld.getCelestialAngle(f) * 360.0F;
      if (angle < 90.0D || angle > 270.0D) {
        GL11.glRotatef(angle - 90.0F, -1.0F, 0.0F, 0.0F);
      } else {
        GL11.glRotatef(angle + 90.0F, -1.0F, 0.0F, 0.0F);
      }
      if (shadowMapIsOrtho)
        GL11.glTranslatef((float)x % 10.0F - 5.0F, (float)y % 10.0F - 5.0F, (float)z % 10.0F - 5.0F);
      shadowProjection = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(2983, shadowProjection);
      shadowProjectionInverse = invertMat4x(shadowProjection);
      shadowModelView = BufferUtils.createFloatBuffer(16);
      GL11.glGetFloat(2982, shadowModelView);
      shadowModelViewInverse = invertMat4x(shadowModelView);
      return;
    }
    previousProjection = projection;
    projection = BufferUtils.createFloatBuffer(16);
    GL11.glGetFloat(2983, projection);
    projectionInverse = invertMat4x(projection);
    previousModelView = modelView;
    modelView = BufferUtils.createFloatBuffer(16);
    GL11.glGetFloat(2982, modelView);
    modelViewInverse = invertMat4x(modelView);
    previousCameraPosition[0] = cameraPosition[0];
    previousCameraPosition[1] = cameraPosition[1];
    previousCameraPosition[2] = cameraPosition[2];
    cameraPosition[0] = x;
    cameraPosition[1] = y;
    cameraPosition[2] = z;
  }

  public static void beginRender(Minecraft minecraft, float f, long l) {
    rainStrength = minecraft.theWorld.visualRainingStrength(f);
    if (isShadowPass)
      return;
    mc = minecraft;
    if (!isInitialized)
      init();
    if (mc.displayWidth != renderWidth || mc.displayHeight != renderHeight)
      resize();
    if (shadowPassInterval > 0 && --shadowPassCounter <= 0) {
      preShadowPassThirdPersonView = mc.gameSettings.thirdPersonView;
      mc.gameSettings.thirdPersonView = 0; // guessed value for false
      isShadowPass = true;
      shadowPassCounter = shadowPassInterval;
      EXTFramebufferObject.glBindFramebufferEXT(36160, sfb);
      useProgram(0);
      mc.entityRenderer.renderWorld(f, l);
      GL11.glFlush();
      isShadowPass = false;
      mc.gameSettings.thirdPersonView = preShadowPassThirdPersonView;
    }
    EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
    useProgram(lightmapEnabled ? 3 : 2);
  }

  public static void endRender() {
    if (isShadowPass)
      return;
    GL11.glPushMatrix();
    GL11.glMatrixMode(5889);
    GL11.glLoadIdentity();
    GL11.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, 0.0D, 1.0D);
    GL11.glMatrixMode(5888);
    GL11.glLoadIdentity();
    GL11.glDisable(2929);
    GL11.glEnable(3553);
    GL11.glDisable(3042);
    useProgram(8);
    GL20.glDrawBuffers(dfbDrawBuffers);
    GL11.glBindTexture(3553, dfbTextures.get(0));
    GL13.glActiveTexture(33985);
    GL11.glBindTexture(3553, dfbTextures.get(1));
    GL13.glActiveTexture(33986);
    GL11.glBindTexture(3553, dfbTextures.get(2));
    GL13.glActiveTexture(33987);
    GL11.glBindTexture(3553, dfbTextures.get(3));
    if (colorAttachments >= 5) {
      GL13.glActiveTexture(33988);
      GL11.glBindTexture(3553, dfbTextures.get(4));
      if (colorAttachments >= 6) {
        GL13.glActiveTexture(33989);
        GL11.glBindTexture(3553, dfbTextures.get(5));
        if (colorAttachments >= 7) {
          GL13.glActiveTexture(33990);
          GL11.glBindTexture(3553, dfbTextures.get(6));
        }
      }
    }
    if (shadowPassInterval > 0) {
      GL13.glActiveTexture(33991);
      GL11.glBindTexture(3553, sfbDepthTexture);
    }
    GL13.glActiveTexture(33984);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glBegin(7);
    GL11.glTexCoord2f(0.0F, 0.0F);
    GL11.glVertex3f(0.0F, 0.0F, 0.0F);
    GL11.glTexCoord2f(1.0F, 0.0F);
    GL11.glVertex3f(1.0F, 0.0F, 0.0F);
    GL11.glTexCoord2f(1.0F, 1.0F);
    GL11.glVertex3f(1.0F, 1.0F, 0.0F);
    GL11.glTexCoord2f(0.0F, 1.0F);
    GL11.glVertex3f(0.0F, 1.0F, 0.0F);
    GL11.glEnd();
    EXTFramebufferObject.glBindFramebufferEXT(36160, 0);
    useProgram(9);
    GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0F);
    GL11.glClear(16640);
    GL11.glBindTexture(3553, dfbTextures.get(0));
    GL13.glActiveTexture(33985);
    GL11.glBindTexture(3553, dfbTextures.get(1));
    GL13.glActiveTexture(33986);
    GL11.glBindTexture(3553, dfbTextures.get(2));
    GL13.glActiveTexture(33987);
    GL11.glBindTexture(3553, dfbTextures.get(3));
    if (colorAttachments >= 5) {
      GL13.glActiveTexture(33988);
      GL11.glBindTexture(3553, dfbTextures.get(4));
      if (colorAttachments >= 6) {
        GL13.glActiveTexture(33989);
        GL11.glBindTexture(3553, dfbTextures.get(5));
        if (colorAttachments >= 7) {
          GL13.glActiveTexture(33990);
          GL11.glBindTexture(3553, dfbTextures.get(6));
        }
      }
    }
    if (shadowPassInterval > 0) {
      GL13.glActiveTexture(33991);
      GL11.glBindTexture(3553, sfbDepthTexture);
    }
    GL13.glActiveTexture(33984);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    GL11.glBegin(7);
    GL11.glTexCoord2f(0.0F, 0.0F);
    GL11.glVertex3f(0.0F, 0.0F, 0.0F);
    GL11.glTexCoord2f(1.0F, 0.0F);
    GL11.glVertex3f(1.0F, 0.0F, 0.0F);
    GL11.glTexCoord2f(1.0F, 1.0F);
    GL11.glVertex3f(1.0F, 1.0F, 0.0F);
    GL11.glTexCoord2f(0.0F, 1.0F);
    GL11.glVertex3f(0.0F, 1.0F, 0.0F);
    GL11.glEnd();
    GL11.glEnable(3042);
    GL11.glPopMatrix();
    useProgram(0);
  }

  public static void beginTerrain() {
    useProgram(4);
    GL13.glActiveTexture(33986);
    GL11.glBindTexture(3553, mc.renderEngine.getTexture("/terrain_nh.png"));
    GL13.glActiveTexture(33987);
    GL11.glBindTexture(3553, mc.renderEngine.getTexture("/terrain_s.png"));
    GL13.glActiveTexture(33984);
    FloatBuffer projection = BufferUtils.createFloatBuffer(16);
  }

  public static void endTerrain() {
    useProgram(lightmapEnabled ? 3 : 2);
  }

  public static void beginWater() {
    useProgram(5);
    GL13.glActiveTexture(33986);
    GL11.glBindTexture(3553, mc.renderEngine.getTexture("/terrain_nh.png"));
    GL13.glActiveTexture(33987);
    GL11.glBindTexture(3553, mc.renderEngine.getTexture("/terrain_s.png"));
    GL13.glActiveTexture(33984);
  }

  public static void endWater() {
    useProgram(lightmapEnabled ? 3 : 2);
  }

  public static void beginHand() {
    GL11.glEnable(3042);
    useProgram(6);
  }

  public static void endHand() {
    GL11.glDisable(3042);
    useProgram(lightmapEnabled ? 3 : 2);
    if (isShadowPass)
      EXTFramebufferObject.glBindFramebufferEXT(36160, sfb);
  }

  public static void beginWeather() {
    GL11.glEnable(3042);
    useProgram(7);
    if (isShadowPass)
      EXTFramebufferObject.glBindFramebufferEXT(36160, 0);
  }

  public static void endWeather() {
    GL11.glDisable(3042);
    useProgram(lightmapEnabled ? 3 : 2);
  }

  private static void resize() {
    renderWidth = mc.displayWidth;
    renderHeight = mc.displayHeight;
    setupFrameBuffer();
  }

  private static void setupShadowMap() {
    setupShadowFrameBuffer();
  }

  private static int setupProgram(String vShaderPath, String fShaderPath) {
    int program = ARBShaderObjects.glCreateProgramObjectARB();
    int vShader = 0;
    int fShader = 0;
    if (program != 0) {
      vShader = createVertShader(vShaderPath);
      fShader = createFragShader(fShaderPath);
    }
    if (vShader != 0 || fShader != 0) {
      if (vShader != 0)
        ARBShaderObjects.glAttachObjectARB(program, vShader);
      if (fShader != 0)
        ARBShaderObjects.glAttachObjectARB(program, fShader);
      if (entityAttrib >= 0)
        ARBVertexShader.glBindAttribLocationARB(program, entityAttrib, "mc_Entity");
      ARBShaderObjects.glLinkProgramARB(program);
      ARBShaderObjects.glValidateProgramARB(program);
      printLogInfo(program);
    } else if (program != 0) {
      ARBShaderObjects.glDeleteObjectARB(program);
      program = 0;
    }
    return program;
  }

  public static void useProgram(int program) {
    if (activeProgram == program)
      return;
    if (isShadowPass) {
      activeProgram = 0;
      ARBShaderObjects.glUseProgramObjectARB(programs[0]);
      return;
    }
    activeProgram = program;
    ARBShaderObjects.glUseProgramObjectARB(programs[program]);
    if (programs[program] == 0)
      return;
    if (program == 2) {
      setProgramUniform1i("texture", 0);
    } else if (program == 3 || program == 6 || program == 7) {
      setProgramUniform1i("texture", 0);
      setProgramUniform1i("lightmap", 1);
    } else if (program == 4 || program == 5) {
      setProgramUniform1i("texture", 0);
      setProgramUniform1i("lightmap", 1);
      setProgramUniform1i("normals", 2);
      setProgramUniform1i("specular", 3);
    } else if (program == 8 || program == 9) {
      setProgramUniform1i("gcolor", 0);
      setProgramUniform1i("gdepth", 1);
      setProgramUniform1i("gnormal", 2);
      setProgramUniform1i("composite", 3);
      setProgramUniform1i("gaux1", 4);
      setProgramUniform1i("gaux2", 5);
      setProgramUniform1i("gaux3", 6);
      setProgramUniform1i("shadow", 7);
      setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, previousProjection);
      setProgramUniformMatrix4ARB("gbufferProjection", false, projection);
      setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, projectionInverse);
      setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, previousModelView);
      if (shadowPassInterval > 0) {
        setProgramUniformMatrix4ARB("shadowProjection", false, shadowProjection);
        setProgramUniformMatrix4ARB("shadowProjectionInverse", false, shadowProjectionInverse);
        setProgramUniformMatrix4ARB("shadowModelView", false, shadowModelView);
        setProgramUniformMatrix4ARB("shadowModelViewInverse", false, shadowModelViewInverse);
      }
    }
    ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
    setProgramUniform1i("heldItemId", (stack == null) ? -1 : stack.itemID);
    setProgramUniform1i("heldBlockLightValue", (stack == null || stack.itemID >= Block.blocksList.length) ? 0 : Block.lightValue[stack.itemID]);
    setProgramUniform1i("fogMode", fogEnabled ? GL11.glGetInteger(2917) : 0);
    setProgramUniform1f("rainStrength", rainStrength);
    setProgramUniform1i("worldTime", (int)(mc.theWorld.getWorldTime() % 24000L));
    setProgramUniform1f("aspectRatio", renderWidth / renderHeight);
    setProgramUniform1f("viewWidth", renderWidth);
    setProgramUniform1f("viewHeight", renderHeight);
    setProgramUniform1f("near", 0.05F);
    setProgramUniform1f("far", (256 >> mc.gameSettings.renderDistance));
    setProgramUniform3f("sunPosition", sunPosition[0], sunPosition[1], sunPosition[2]);
    setProgramUniform3f("moonPosition", moonPosition[0], moonPosition[1], moonPosition[2]);
    setProgramUniform3f("previousCameraPosition", (float)previousCameraPosition[0], (float)previousCameraPosition[1], (float)previousCameraPosition[2]);
    setProgramUniform3f("cameraPosition", (float)cameraPosition[0], (float)cameraPosition[1], (float)cameraPosition[2]);
    setProgramUniformMatrix4ARB("gbufferModelView", false, modelView);
    setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, modelViewInverse);

    setProgramUniform1i("systemTime", (int) (System.currentTimeMillis() - start));
  }

  private static long start = System.currentTimeMillis();

  public static void setProgramUniform1i(String name, int x) {
    if (activeProgram == 0)
      return;
    int uniform = ARBShaderObjects.glGetUniformLocationARB(programs[activeProgram], name);
    ARBShaderObjects.glUniform1iARB(uniform, x);
  }

  public static void setProgramUniform1f(String name, float x) {
    if (activeProgram == 0)
      return;
    int uniform = ARBShaderObjects.glGetUniformLocationARB(programs[activeProgram], name);
    ARBShaderObjects.glUniform1fARB(uniform, x);
  }

  public static void setProgramUniform3f(String name, float x, float y, float z) {
    if (activeProgram == 0)
      return;
    int uniform = ARBShaderObjects.glGetUniformLocationARB(programs[activeProgram], name);
    ARBShaderObjects.glUniform3fARB(uniform, x, y, z);
  }

  public static void setProgramUniformMatrix4ARB(String name, boolean transpose, FloatBuffer matrix) {
    if (activeProgram == 0 || matrix == null)
      return;
    int uniform = GL20.glGetUniformLocation(programs[activeProgram], name);
    ARBShaderObjects.glUniformMatrix4ARB(uniform, transpose, matrix);
  }

  public static void setCelestialPosition() {
    FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    GL11.glGetFloat(2982, modelView);
    float[] mv = new float[16];
    modelView.get(mv, 0, 16);
    float[] sunPos = multiplyMat4xVec4(mv, new float[] { 0.0F, 100.0F, 0.0F, 0.0F });
    sunPosition = sunPos;
    float[] moonPos = multiplyMat4xVec4(mv, new float[] { 0.0F, -100.0F, 0.0F, 0.0F });
    moonPosition = moonPos;
  }

  private static float[] multiplyMat4xVec4(float[] ta, float[] tb) {
    float[] mout = new float[4];
    mout[0] = ta[0] * tb[0] + ta[4] * tb[1] + ta[8] * tb[2] + ta[12] * tb[3];
    mout[1] = ta[1] * tb[0] + ta[5] * tb[1] + ta[9] * tb[2] + ta[13] * tb[3];
    mout[2] = ta[2] * tb[0] + ta[6] * tb[1] + ta[10] * tb[2] + ta[14] * tb[3];
    mout[3] = ta[3] * tb[0] + ta[7] * tb[1] + ta[11] * tb[2] + ta[15] * tb[3];
    return mout;
  }

  private static FloatBuffer invertMat4x(FloatBuffer matin) {
    float[] m = new float[16];
    float[] inv = new float[16];
    int i;
    for (i = 0; i < 16; i++)
      m[i] = matin.get(i);
    inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
    inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
    inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
    inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
    inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
    inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
    inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
    inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
    inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
    inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
    inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
    inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
    inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
    inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
    inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
    inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
    float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
    FloatBuffer invout = BufferUtils.createFloatBuffer(16);
    if (det == 0.0D)
      return invout;
    for (i = 0; i < 16; i++)
      invout.put(i, inv[i] / det);
    return invout;
  }

  private static int createVertShader(String filename) {
    BufferedReader reader;
    int vertShader = ARBShaderObjects.glCreateShaderObjectARB(35633);
    if (vertShader == 0)
      return 0;
    String vertexCode = "";
    try {
      reader = new BufferedReader(new InputStreamReader(Shaders.class.getResourceAsStream(filename)));
    } catch (Exception e) {
      try {
        reader = new BufferedReader(new FileReader(new File(filename)));
      } catch (Exception e2) {
        System.out.println("Couldn't open " + filename + "!");
        ARBShaderObjects.glDeleteObjectARB(vertShader);
        return 0;
      }
    }
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        vertexCode = vertexCode + line + "\n";
        if (line.matches("attribute [_a-zA-Z0-9]+ mc_Entity.*"))
        entityAttrib = 10;
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    ARBShaderObjects.glShaderSourceARB(vertShader, vertexCode);
    ARBShaderObjects.glCompileShaderARB(vertShader);
    printLogInfo(vertShader);
    return vertShader;
  }

  private static int createFragShader(String filename) {
    BufferedReader reader;
    int fragShader = ARBShaderObjects.glCreateShaderObjectARB(35632);
    if (fragShader == 0)
      return 0;
    String fragCode = "";
    try {
      reader = new BufferedReader(new InputStreamReader(Shaders.class.getResourceAsStream(filename)));
    } catch (Exception e) {
      try {
        reader = new BufferedReader(new FileReader(new File(filename)));
      } catch (Exception e2) {
        System.out.println("Couldn't open " + filename + "!");
        ARBShaderObjects.glDeleteObjectARB(fragShader);
        return 0;
      }
    }
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        fragCode = fragCode + line + "\n";
        if (colorAttachments < 5 && line.matches("uniform [ _a-zA-Z0-9]+ gaux1;.*")) {
          colorAttachments = 5;
          continue;
        }
        if (colorAttachments < 6 && line.matches("uniform [ _a-zA-Z0-9]+ gaux2;.*")) {
          colorAttachments = 6;
          continue;
        }
        if (colorAttachments < 7 && line.matches("uniform [ _a-zA-Z0-9]+ gaux3;.*")) {
          colorAttachments = 7;
          continue;
        }
        if (colorAttachments < 8 && line.matches("uniform [ _a-zA-Z0-9]+ shadow;.*")) {
          shadowPassInterval = 1;
          colorAttachments = 8;
          continue;
        }
        if (line.matches("/\\* SHADOWRES:[0-9]+ \\*/.*")) {
          String[] parts = line.split("(:| )", 4);
          System.out.println("Shadow map resolution: " + parts[2]);
          shadowMapWidth = shadowMapHeight = Integer.parseInt(parts[2]);
          continue;
        }
        if (line.matches("/\\* SHADOWFOV:[0-9\\.]+ \\*/.*")) {
          String[] parts = line.split("(:| )", 4);
          System.out.println("Shadow map field of view: " + parts[2]);
          shadowMapFOV = Float.parseFloat(parts[2]);
          shadowMapIsOrtho = false;
          continue;
        }
        if (line.matches("/\\* SHADOWHPL:[0-9\\.]+ \\*/.*")) {
          String[] parts = line.split("(:| )", 4);
          System.out.println("Shadow map half-plane: " + parts[2]);
          shadowMapHalfPlane = Float.parseFloat(parts[2]);
          shadowMapIsOrtho = true;
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    ARBShaderObjects.glShaderSourceARB(fragShader, fragCode);
    ARBShaderObjects.glCompileShaderARB(fragShader);
    printLogInfo(fragShader);
    return fragShader;
  }

  private static boolean printLogInfo(int obj) {
    IntBuffer iVal = BufferUtils.createIntBuffer(1);
    ARBShaderObjects.glGetObjectParameterARB(obj, 35716, iVal);
    int length = iVal.get();
    if (length > 1) {
      ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
      iVal.flip();
      ARBShaderObjects.glGetInfoLogARB(obj, iVal, infoLog);
      byte[] infoBytes = new byte[length];
      infoLog.get(infoBytes);
      String out = new String(infoBytes);
      System.out.println("Info log:\n" + out);
      return false;
    }
    return true;
  }

  private static void setupFrameBuffer() {
    setupRenderTextures();
    if (dfb != 0) {
      EXTFramebufferObject.glDeleteFramebuffersEXT(dfb);
      EXTFramebufferObject.glDeleteRenderbuffersEXT(dfbRenderBuffers);
    }
    dfb = EXTFramebufferObject.glGenFramebuffersEXT();
    EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
    EXTFramebufferObject.glGenRenderbuffersEXT(dfbRenderBuffers);
    for (int i = 0; i < colorAttachments; i++) {
      EXTFramebufferObject.glBindRenderbufferEXT(36161, dfbRenderBuffers.get(i));
      if (i == 1) {
        EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34837, renderWidth, renderHeight);
      } else {
        EXTFramebufferObject.glRenderbufferStorageEXT(36161, 6408, renderWidth, renderHeight);
      }
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, dfbDrawBuffers.get(i), 36161, dfbRenderBuffers.get(i));
      EXTFramebufferObject.glFramebufferTexture2DEXT(36160, dfbDrawBuffers.get(i), 3553, dfbTextures.get(i), 0);
    }
    EXTFramebufferObject.glDeleteRenderbuffersEXT(dfbDepthBuffer);
    dfbDepthBuffer = EXTFramebufferObject.glGenRenderbuffersEXT();
    EXTFramebufferObject.glBindRenderbufferEXT(36161, dfbDepthBuffer);
    EXTFramebufferObject.glRenderbufferStorageEXT(36161, 6402, renderWidth, renderHeight);
    EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, dfbDepthBuffer);
    int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
    if (status != 36053)
      System.out.println("Failed creating framebuffer! (Status " + status + ")");
  }

  private static void setupShadowFrameBuffer() {
    if (shadowPassInterval <= 0)
      return;
    setupShadowRenderTexture();
    EXTFramebufferObject.glDeleteFramebuffersEXT(sfb);
    sfb = EXTFramebufferObject.glGenFramebuffersEXT();
    EXTFramebufferObject.glBindFramebufferEXT(36160, sfb);
    GL11.glDrawBuffer(0);
    GL11.glReadBuffer(0);
    EXTFramebufferObject.glDeleteRenderbuffersEXT(sfbDepthBuffer);
    sfbDepthBuffer = EXTFramebufferObject.glGenRenderbuffersEXT();
    EXTFramebufferObject.glBindRenderbufferEXT(36161, sfbDepthBuffer);
    EXTFramebufferObject.glRenderbufferStorageEXT(36161, 6402, shadowMapWidth, shadowMapHeight);
    EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, sfbDepthBuffer);
    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, sfbDepthTexture, 0);
    int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
    if (status != 36053)
      System.out.println("Failed creating shadow framebuffer! (Status " + status + ")");
  }

  private static void setupRenderTextures() {
    GL11.glDeleteTextures(dfbTextures);
    GL11.glGenTextures(dfbTextures);
    for (int i = 0; i < colorAttachments; i++) {
      GL11.glBindTexture(3553, dfbTextures.get(i));
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      if (i == 1) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4 * 4);
        GL11.glTexImage2D(3553, 0, 34837, renderWidth, renderHeight, 0, 6408, 5126, buffer);
      } else {
        ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4);
        if (i == 0 || i == 3) {
          for (int ii = 0; ii < renderWidth * renderHeight; ii++) {
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 26); // related to glAlphaFunc(GL_GREATER, 0.1f)
          }
          buffer.flip();  // Prepare the buffer for use in glTexImage2D
        }

        GL11.glTexImage2D(3553, 0, 6408, renderWidth, renderHeight, 0, 6408, 5121, buffer);
      }
    }
  }

  private static void setupShadowRenderTexture() {
    if (shadowPassInterval <= 0)
      return;
    GL11.glDeleteTextures(sfbDepthTexture);
    sfbDepthTexture = GL11.glGenTextures();
    GL11.glBindTexture(3553, sfbDepthTexture);
    GL11.glTexParameterf(3553, 10242, 10496.0F);
    GL11.glTexParameterf(3553, 10243, 10496.0F);
    GL11.glTexParameteri(3553, 10241, 9729);
    GL11.glTexParameteri(3553, 10240, 9729);
    ByteBuffer buffer = ByteBuffer.allocateDirect(shadowMapWidth * shadowMapHeight * 4);
    GL11.glTexImage2D(3553, 0, 6402, shadowMapWidth, shadowMapHeight, 0, 6402, 5126, buffer);
  }

  private static boolean isInitialized = false;

  private static int renderWidth = 0;

  private static int renderHeight = 0;

  private static Minecraft mc = null;

  private static float[] sunPosition = new float[3];

  private static float[] moonPosition = new float[3];

  private static float[] clearColor = new float[3];

  private static float rainStrength = 0.0F;

  private static boolean lightmapEnabled = false;

  private static boolean fogEnabled = true;

  public static int entityAttrib = -1;

  private static FloatBuffer previousProjection = null;

  private static FloatBuffer projection = null;

  private static FloatBuffer projectionInverse = null;

  private static FloatBuffer previousModelView = null;

  private static FloatBuffer modelView = null;

  private static FloatBuffer modelViewInverse = null;

  private static double[] previousCameraPosition = new double[3];

  private static double[] cameraPosition = new double[3];

  private static int shadowPassInterval = 0;

  private static int shadowMapWidth = 1024;

  private static int shadowMapHeight = 1024;

  private static float shadowMapFOV = 25.0F;

  private static float shadowMapHalfPlane = 30.0F;

  private static boolean shadowMapIsOrtho = true;

  private static int shadowPassCounter = 0;

  private static int preShadowPassThirdPersonView;

  private static boolean isShadowPass = false;

  private static int sfb = 0;

  private static int sfbColorTexture = 0;

  private static int sfbDepthTexture = 0;

  private static int sfbRenderBuffer = 0;

  private static int sfbDepthBuffer = 0;

  private static FloatBuffer shadowProjection = null;

  private static FloatBuffer shadowProjectionInverse = null;

  private static FloatBuffer shadowModelView = null;

  private static FloatBuffer shadowModelViewInverse = null;

  private static int colorAttachments = 0;

  private static IntBuffer dfbDrawBuffers = null;

  private static IntBuffer dfbTextures = null;

  private static IntBuffer dfbRenderBuffers = null;

  private static int dfb = 0;

  private static int dfbDepthBuffer = 0;

  public static int activeProgram = 0;

  public static final int ProgramNone = 0;

  public static final int ProgramBasic = 1;

  public static final int ProgramTextured = 2;

  public static final int ProgramTexturedLit = 3;

  public static final int ProgramTerrain = 4;

  public static final int ProgramWater = 5;

  public static final int ProgramHand = 6;

  public static final int ProgramWeather = 7;

  public static final int ProgramComposite = 8;

  public static final int ProgramFinal = 9;

  public static final int ProgramCount = 10;

  private static String[] programNames = new String[] { "", "gbuffers_basic", "gbuffers_textured", "gbuffers_textured_lit", "gbuffers_terrain", "gbuffers_water", "gbuffers_hand", "gbuffers_weather", "composite", "final" };

  private static int[] programBackups = new int[] { 0, 0, 1, 2, 3, 4, 3, 3, 0, 0 };

  private static int[] programs = new int[10];
}
