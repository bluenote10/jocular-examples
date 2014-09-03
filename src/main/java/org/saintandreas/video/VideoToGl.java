package org.saintandreas.video;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.saintandreas.gl.textures.Texture;

import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;

import com.sun.jna.Memory;

public class VideoToGl implements BufferFormatCallback, RenderCallback {
  private int videoHeight, videoWidth;
  private int bufferSize;
  private ByteBuffer videoData;
  private volatile boolean newFrame = false;
  private VideoSizeChangedCallback videoChangedCallback;

  public VideoToGl() {
  }

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
    videoChangedCallback.onVideoChanged(sourceWidth, sourceHeight);
      bufferSize = sourceWidth * sourceHeight * 4;
      videoData = BufferUtils.createByteBuffer(bufferSize);
      videoWidth = sourceWidth;
      videoHeight = sourceHeight;
      return new BufferFormat("RGBA", // 
          sourceWidth, sourceHeight, //
          new int[] { sourceWidth * 4 },//
          new int[] { sourceHeight });
  }

  public void updateTexture(Texture videoTexture) {
    if (newFrame) {
      synchronized (videoData) {
        videoData.position(0);
        videoTexture.bind();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, videoWidth, videoHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, videoData);
        //glGenerateMipmap(GL_TEXTURE_2D);
        videoTexture.unbind();
        newFrame = false;
      }
    }
  }

  public void setVideoChangedCallback(VideoSizeChangedCallback videoChangedCallback) {
    this.videoChangedCallback = videoChangedCallback;
  }
}
