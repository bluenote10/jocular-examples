package org.saintandreas.gl;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.lwjgl.opengl.GL11;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;

public class SwingGl {
  private final BufferedImage image;
  private final Texture texture;
  private final IndexedGeometry geometry;
  private final Program program;
  private final JFrame frame;

  public SwingGl(int width, int height) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    texture = new Texture();
    texture.bind();
    texture.image2d(GL_RGBA8, width, height, 0, GL_RGBA, null);
    texture.parameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    texture.parameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    texture.unbind();
    geometry = OpenGL.makeTexturedQuad((float)width / (float)height);
    program = new Program(
        GlamourResources.SHADERS_TEXTURED_VS, 
        GlamourResources.SHADERS_TEXTURED_FS);
    program.link();
    frame = new JFrame("SwingGl");
    frame.setSize(width, height);
    //frame.setVisible(true);
  }

  public void render() {
    Graphics g = image.getGraphics();
//    g.translate(-100, -100);
    frame.paintComponents(g);
    g.dispose();
    texture.bind();
//     texture.loadImageData(image);
    texture.unbind();
  }

  public void draw() {
    program.use();
    MatrixStack.bindAll(program);
    texture.bind();
    geometry.bindVertexArray();
    geometry.draw();
    texture.unbind();
    VertexArray.unbind();
    Program.clear();
  }
  
  public JFrame getFrame() {
    return frame;
  }
}

