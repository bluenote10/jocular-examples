package org.saintandreas;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.*;
import static org.saintandreas.gl.GlamourResources.*;

import java.util.HashMap;
import java.util.Map;

import org.saintandreas.gl.shaders.Program;
import org.saintandreas.resources.Resource;
import org.saintandreas.resources.ResourceManager;



public class Programs {
  private static Map<String, ProgramInfo> LOADED_PROGRAMS = new HashMap<>();
  
  private static class ProgramInfo {
    Program program;
    Resource vs;
    Resource fs;
    long vsModified;
    long fsModified;

    public void update(Resource vs, Resource fs) {
      this.vs = vs;
      this.fs = fs;
      long vsModifiedNew = ResourceManager.getLastModified(vs);
      long fsModifiedNew = Long.MAX_VALUE; //ResourceManager.getLastModified(fs);
      if (null == program || (vsModifiedNew > vsModified) || (fsModifiedNew > fsModified)) {
        program = new Program(vs, fs);
        program.link();
        this.vsModified = vsModifiedNew;
        this.fsModified = fsModifiedNew;
      }
    }
  }

  private static String getKey(Resource vs, Resource fs) {
    return vs.getPath() + ":" + fs.getPath();
  }

  public static Program getProgram(Resource vs, Resource fs) {
    String key = getKey(vs, fs);
    
    ProgramInfo result;
    if (!LOADED_PROGRAMS.containsKey(key)) {
      LOADED_PROGRAMS.put(key, result = new ProgramInfo());
    } else {
      result = LOADED_PROGRAMS.get(key);
    }
    result.update(vs, fs);
    return result.program;
  }

  private static Resource SHADER_INCLUDES[] = new Resource[]{
    SHADERS_NOISE_CELLULAR2_GLSL, 
    SHADERS_NOISE_CELLULAR2X2_GLSL, 
    SHADERS_NOISE_CELLULAR2X2X2_GLSL, 
    SHADERS_NOISE_CELLULAR3_GLSL, 
    SHADERS_NOISE_CNOISE2_GLSL, 
    SHADERS_NOISE_CNOISE3_GLSL, 
    SHADERS_NOISE_CNOISE4_GLSL, 
    SHADERS_NOISE_SNOISE2_GLSL, 
    SHADERS_NOISE_SNOISE3_GLSL, 
    SHADERS_NOISE_SNOISE4_GLSL, 
    SHADERS_NOISE_SRDNOISE2_GLSL, 
  };
  
  public static void compileIncludes(Resource rs[]) {
    for (Resource r : rs) {
      String path = "/" + r.getPath();
      String shader = ResourceManager.getProvider().getAsString(r);
      glNamedStringARB(GL_SHADER_INCLUDE_ARB, path, shader); 
    }
  }
}
