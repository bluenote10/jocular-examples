package org.saintandreas.vr;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.saintandreas.ExampleResource;
import org.saintandreas.Programs;
import org.saintandreas.gl.FrameBuffer;
import org.saintandreas.gl.IndexedGeometry;
import org.saintandreas.gl.MatrixStack;
import org.saintandreas.gl.OpenGL;
import org.saintandreas.gl.SceneHelpers;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Vector2f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.resources.BasicResource;
import org.saintandreas.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.player.AudioDevice;
import uk.co.caprica.vlcj.player.AudioOutput;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import com.oculusvr.capi.OvrLibrary;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class VideoDemo extends RiftApp implements BufferFormatCallback, RenderCallback {
  // private static final String MEDIA_URL = "http://192.168.0.4/sc2a.mp4";
  // private static final String MEDIA_URL =
  // "http://192.168.0.4/Videos/South.Park.S17E09.HDTV.x264-ASAP.%5bVTV%5d.mp4";
   // private static final String MEDIA_URL =
   // "http://192.168.0.4/Videos/3D/Gravity.2013.1080p%203D.HDTV.x264.DTS-RARBG.mkv";
  private static final String MEDIA_URL =   "http://192.168.0.4/Videos/3D/TRON%20LEGACY%203D.mkv";
  //private static final String MEDIA_URL = "http://192.168.0.4/Videos/3D/Man.Of.Steel.3D.2013.1080p.BluRay.Half-OU.DTS.x264-PublicHD.mkv";
  private static final Logger LOG = LoggerFactory.getLogger(VideoDemo.class);
  private static final LibVlc LIB_VLC;
  static {
//  System.setProperty("jna.library.path", "/Program Files/VideoLAN/VLC/plugins");
    NativeLibrary.addSearchPath(
        RuntimeUtil.getLibVlcLibraryName(), "/Program Files/VideoLan/VLC"
    );
    Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
    LIB_VLC = LibVlcFactory.factory().log().create();
  }

  IndexedGeometry cubeGeometry;
  IndexedGeometry eyeMeshes[];
  IndexedGeometry screenQuad;
  Program coloredProgram;
  Program textureProgram;
  FrameBuffer frameBuffer;
  Texture videoTexture;
  ByteBuffer videoData;
  MediaPlayer player;
//  SwingGl swingPane;
  private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
  volatile boolean newFrame = false;
  int videoWidth, videoHeight;
  private float videoAspect;
  private boolean swap = false;

  enum StereoscopicMode {
    SIDE_BY_SIDE,
    TOP_AND_BOTTOM,
  }
  public static IndexedGeometry[] get3dEyeMeshes(float aspect, StereoscopicMode mode) {
    Vector2f min = new Vector2f(-0.5f, -0.5f / aspect);
    Vector2f max = new Vector2f(0.5f, 0.5f / aspect);
    Vector2f texMin = Vector2f.UNIT_Y;
    Vector2f texMax = Vector2f.UNIT_X;
    Vector2f leftMax, rightMin;

    switch (mode) {
    case SIDE_BY_SIDE:
      leftMax = new Vector2f(0.5f, 0);
      rightMin = new Vector2f(0.5f, 1);
      break;
      
    case TOP_AND_BOTTOM:
      leftMax = new Vector2f(1, 0.5f);
      rightMin = new Vector2f(0, 0.5f);
      break;

    default:
      throw new IllegalStateException("Unknown stereoscopic format");
    }

    return new IndexedGeometry[] {
      OpenGL.makeTexturedQuad(min, max, texMin, leftMax),
      OpenGL.makeTexturedQuad(min, max, rightMin, texMax)
    };
  }

  public VideoDemo() {
    // Rift applications should have no window decoration
    System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
    MediaPlayerFactory playerFactory = new MediaPlayerFactory(LIB_VLC);
    for (AudioOutput output : playerFactory.getAudioOutputs()) {
      LOG.warn("-----------");
      LOG.warn(output.getName());
      LOG.warn(output.getDescription());
      for (AudioDevice device : output.getDevices()) {
        LOG.warn("\t" + device.getDeviceId());
        LOG.warn("\t" + device.getLongName());
        LOG.warn("\t*********");
      }
    }

    player = playerFactory.newDirectMediaPlayer(this, this);
    player.playMedia(MEDIA_URL);
  }

  
  private static final Resource SHADER_INCLUDES[] = new Resource[]{
      new BasicResource("shaders/noise/cellular2.glsl"),
      new BasicResource("shaders/noise/cellular2x2.glsl"),
      new BasicResource("shaders/noise/cellular2x2x2.glsl"),
      new BasicResource("shaders/noise/cellular3.glsl"),
      new BasicResource("shaders/noise/cnoise2.glsl"),
      new BasicResource("shaders/noise/cnoise3.glsl"),
      new BasicResource("shaders/noise/cnoise4.glsl"),
      new BasicResource("shaders/noise/snoise2.glsl"),
      new BasicResource("shaders/noise/snoise3.glsl"),
      new BasicResource("shaders/noise/snoise4.glsl"),
      new BasicResource("shaders/noise/srdnoise2.glsl"),
  };

  
  @Override
  protected void initGl() {
    super.initGl();
    Programs.compileIncludes(SHADER_INCLUDES);
    hmd.enableHswDisplay(false);
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glEnable(GL_PRIMITIVE_RESTART);
    videoTexture = new Texture(GL_TEXTURE_2D);
    videoTexture.bind();
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    videoTexture.unbind();

    glPrimitiveRestartIndex(Short.MAX_VALUE);
    MatrixStack.MODELVIEW.lookat(new Vector3f(0, 0, 3), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    
//    swingPane = new SwingGl(640, 480);
//    JFrame frame = swingPane.getFrame();
//    frame.add(new JTable(new DefaultTableModel() {
//      @Override
//      public int getColumnCount() {
//          return 10;
//      }
//      @Override
//      public int getRowCount() {
//          return 10;
//      }
//      @Override
//      public Object getValueAt(int row , int column) {
//          return row + " " + column;
//      }
//    }));
//    frame.setSize(800, 600);

    frameBuffer = new FrameBuffer(width / 2, height);
    frameBuffer.getTexture().bind();
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
    frameBuffer.getTexture().unbind();
    OpenGL.checkError();

    // glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
    coloredProgram = new Program(ExampleResource.SHADERS_COLORED_VS, ExampleResource.SHADERS_COLORED_FS);
    coloredProgram.link();

    textureProgram = new Program(ExampleResource.SHADERS_TEXTURED_VS, ExampleResource.SHADERS_TEXTURED_FS);
    textureProgram.link();
    screenQuad = OpenGL.makeTexturedQuad();
  }

  @Override
  protected void onResize(int width, int height) {
    super.onResize(width, height);
    MatrixStack.PROJECTION.perspective(80f, aspect / 2.0f, 0.01f, 1000.0f);
  }
  
  protected void setTimeDelta(float time) {
    player.setTime(player.getTime() + (int)(time * 1000));
  }

  @Override
  protected boolean onKeyboardEvent() {
    if (super.onKeyboardEvent()) {
      return true;
    }
    if (!Keyboard.getEventKeyState()) {
      return false;
    }
    switch (Keyboard.getEventKey()) {
    case Keyboard.KEY_R:
      hmd.recenterPose();
      return true;
    case Keyboard.KEY_S:
      swap = !swap;
      return true;
    case Keyboard.KEY_LEFT:
      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
        setTimeDelta(-5);
      } else {
        setTimeDelta(-30);
      }
      return true;
    case Keyboard.KEY_RIGHT:
      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
        setTimeDelta(5);
      } else {
        setTimeDelta(30);
      }
      return true;

    case Keyboard.KEY_UP:
      setTimeDelta(5 * 60);
      return true;

    case Keyboard.KEY_DOWN:
      setTimeDelta(-5 * 60);
      return true;

    case Keyboard.KEY_SPACE:
      player.pause();
      return true;

    }
    return false;
  }

  @Override
  protected void update() {
    super.update();
    Runnable task;
    while (null != (task = taskQueue.poll())) {
      task.run();
    }

//    swingPane.render();
    MatrixStack.MODELVIEW.lookat(
        new Vector3f(0, OvrLibrary.OVR_DEFAULT_EYE_HEIGHT, 0.8f), 
        new Vector3f(0, OvrLibrary.OVR_DEFAULT_EYE_HEIGHT, 0), 
        new Vector3f(0, 1, 0));
    if (newFrame) {
      onNewFrame();
    }
  }

  protected void onNewFrame() {
    synchronized (videoData) {
      videoData.position(0);
      videoTexture.bind();
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, videoWidth, videoHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, videoData);
      glGenerateMipmap(GL_TEXTURE_2D);
      videoTexture.unbind();
      newFrame = false;
    }
  }

  @Override
  protected void onDestroy() {
    player.stop();
    player.release();
  }

  public static void main(String[] args) {
    new VideoDemo().run();
  }

  // This callback is executed by the VLCJ library whenever new
  // frame data is available. We cannot transfer it to the OpenGL
  // texture here, because the GL calls need to be confined to the
  // main render thread.
  @Override
  public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
    synchronized (videoData) {
      Memory m = nativeBuffers[0];
      videoData.position(0);
      videoData.put(m.getByteBuffer(0, videoData.capacity()));
      videoData.position(0);
      newFrame = true;
    }
  }

  @Override
  public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
    videoData = BufferUtils.createByteBuffer(sourceWidth * sourceHeight * 4);
    videoWidth = sourceWidth;
    videoHeight = sourceHeight;
    videoAspect = (float) videoWidth / (float) videoHeight;
    taskQueue.add(()->{
      eyeMeshes = get3dEyeMeshes(videoAspect, StereoscopicMode.SIDE_BY_SIDE);
    });

    return new BufferFormat("RGBA", sourceWidth, sourceHeight, //
        new int[] { sourceWidth * 4 },//
        new int[] { sourceHeight });
  }

  @Override
  protected void renderScene() {
    // glViewport(1, 1, width / 2 - 1, height - 1);
    glClear(GL_DEPTH_BUFFER_BIT);
    glEnable(GL_TEXTURE_2D);
    glDisable(GL_BLEND);
    SceneHelpers.renderProceduralSkybox();

    MatrixStack mv = MatrixStack.MODELVIEW;
    if (null != eyeMeshes) {
      mv.withPush(()->{
        mv.translate(new Vector3f(0, 0, -8.0f));
        mv.scale(16);
        mv.translate(new Vector3f(0, 0.3f, 0));
        textureProgram.use();
        MatrixStack.bindAll(textureProgram);
        videoTexture.bind();
        int currentEye = getCurrentEye();
        if (swap) {
          ++currentEye;
          currentEye %= 2;
        }
        IndexedGeometry eyeMesh = eyeMeshes[currentEye]; 
        eyeMesh.bindVertexArray();
        eyeMesh.draw();
      });
    }
  }
}



//@Override
//public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media, String mrl) {
//  LOG.warn("Media Changed");
//}
//
//@Override
//public void opening(MediaPlayer mediaPlayer) {
//  LOG.warn("Opening");
//}
//
//@Override
//public void buffering(MediaPlayer mediaPlayer, float newCache) {
//  LOG.warn("Buffering");
//}
//
//@Override
//public void playing(MediaPlayer mediaPlayer) {
//  LOG.warn("Playing");
//}
//
//@Override
//public void paused(MediaPlayer mediaPlayer) {
//  LOG.warn("Paused");
//}
//
//@Override
//public void stopped(MediaPlayer mediaPlayer) {
//  LOG.warn("Stopped");
//}
//
//@Override
//public void forward(MediaPlayer mediaPlayer) {
//  LOG.warn("Foreward");
//}
//
//@Override
//public void backward(MediaPlayer mediaPlayer) {
//  LOG.warn("backward");
//
//}
//
//@Override
//public void finished(MediaPlayer mediaPlayer) {
//  LOG.warn("finished");
//  mediaPlayer.playMedia(MEDIA_URL);
//}
//
//@Override
//public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
//  // LOG.warn("timeChanged");
//}
//
//@Override
//public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
//  // LOG.warn("positionChanged");
//}
//
//@Override
//public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {
//  LOG.warn("seekableChanged");
//}
//
//@Override
//public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {
//  LOG.warn("pausableChanged");
//}
//
//@Override
//public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {
//  LOG.warn("titleChanged");
//}
//
//@Override
//public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {
//  LOG.warn("snapshotTaken");
//}
//
//@Override
//public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
//  LOG.warn("lengthChanged");
//}
//
//@Override
//public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
//  LOG.warn("videoOutput");
//}
//
//@Override
//public void error(MediaPlayer mediaPlayer) {
//  LOG.warn("error");
//}
//
//@Override
//public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
//  LOG.warn("mediaMetaChanged");
//}
//
//@Override
//public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
//  LOG.warn("mediaSubItemAdded");
//}
//
//@Override
//public void mediaDurationChanged(MediaPlayer mediaPlayer, long newDuration) {
//  LOG.warn("mediaDurationChanged");
//}
//
//@Override
//public void mediaParsedChanged(MediaPlayer mediaPlayer, int newStatus) {
//  LOG.warn("mediaParsedChanged");
//}
//
//@Override
//public void mediaFreed(MediaPlayer mediaPlayer) {
//  LOG.warn("mediaFreed");
//}
//
//@Override
//public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
//  LOG.warn("mediaStateChanged");
//}
//
//@Override
//public void newMedia(MediaPlayer mediaPlayer) {
//  LOG.warn("newMedia");
//}
//
//@Override
//public void subItemPlayed(MediaPlayer mediaPlayer, int subItemIndex) {
//  LOG.warn("subItemPlayed");
//}
//
//@Override
//public void subItemFinished(MediaPlayer mediaPlayer, int subItemIndex) {
//  LOG.warn("subItemFinished");
//}
//
//@Override
//public void endOfSubItems(MediaPlayer mediaPlayer) {
//  LOG.warn("endOfSubItems");
//}
