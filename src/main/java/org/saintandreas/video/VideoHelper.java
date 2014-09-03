package org.saintandreas.video;

import org.saintandreas.gl.IndexedGeometry;
import org.saintandreas.gl.OpenGL;
import org.saintandreas.math.Vector2f;

public class VideoHelper {

  public static IndexedGeometry[] get3dEyeMeshes(float aspect, StereoscopicMode mode) {
    Vector2f min = new Vector2f(-0.5f, -0.5f / aspect);
    Vector2f max = new Vector2f(0.5f, 0.5f / aspect);
    Vector2f texMin = Vector2f.UNIT_Y;
    Vector2f texMax = Vector2f.UNIT_X;
    Vector2f leftMax, rightMin;
  
    switch (mode) {
    case NONE: 
      leftMax = texMax;
      rightMin = texMin;
      break;
      
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

}
