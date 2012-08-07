package org.teamflawless.dust;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.mozilla.javascript.*;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

@Slf4j
public class DustEngine {
    private static final String DUST_JS_FILENAME = "/js/dust-full-1.0.0.js";
    
    @Setter private List<String> dustTemplates;
    
    private Scriptable globalScope;

    public DustEngine() {
        try {
            Context jsCtx = Context.enter();
            jsCtx.setOptimizationLevel(9);

            globalScope = jsCtx.initStandardObjects();

            @Cleanup InputStream dustStream = DustEngineTest.class.getResourceAsStream(DUST_JS_FILENAME);
            if (dustStream != null) {
                Reader dustReader = new InputStreamReader(dustStream, "UTF-8");
                jsCtx.evaluateReader(globalScope, dustReader, DUST_JS_FILENAME, 0, null);
            }
                
            Context.exit();
        } catch (IOException ex) {
            throw new RuntimeException(" ERROR : Unable to load dust engine resource: ", ex);
        }
    }

    public void init() {
        if (dustTemplates == null) {
            return;
        }
        
        for (String resourcePath : dustTemplates) {
            try {
                Pattern p = Pattern.compile(resourcePath.replaceAll("\\*", ".*"));
                Collection<String> resources = ResourceList.getResources(p);

                for (String rsrc : resources) {
                    URL url = this.getClass().getResource(rsrc);
                    String fullPath = url.getFile();
                    int lastSlash = fullPath.lastIndexOf("/");
                    String file = fullPath.substring(lastSlash+1);
                    loadTemplate(file, Resources.toString(url, Charsets.UTF_8));
                }
            } catch (IOException e) {
                log.warn("Exception while loading resources", e);
            }
        }        
        
    }

    public void close() {
        // no longer needed
    }
    
    public Object execJs(Scriptable scope, String rawJs) {
        Context jsCtx = Context.enter();

        try {
            return jsCtx.evaluateString(scope, rawJs, "DustEngine", 0, null);
        } catch (JavaScriptException e) {
            // Fail hard on any render time error for dust templates
            throw new RuntimeException(e);
        } finally {
            Context.exit();
        }
    }

    /** Reset the dust.js cache and reinitialize */
    public void reset() {
        execJs(globalScope, "dust.reset()");
        init();
    }
    
    public String compileTemplate(String name, String rawSource) {
        Context jsCtx = Context.enter();
        Scriptable compileScope = jsCtx.newObject(globalScope);
        compileScope.setParentScope(globalScope);
        compileScope.put("rawSource", compileScope, rawSource);
        compileScope.put("name", compileScope, name);
        
        return (String) execJs(compileScope, "(dust.compile(rawSource, name))");
    }

    public void loadTemplate(String name, String rawSource) {
        Context jsCtx = Context.enter();
        
        log.debug("Compiling " + name + " as dust.js template");
        
        Scriptable compileScope = jsCtx.newObject(globalScope);
        compileScope.setParentScope(globalScope);
        compileScope.put("rawSource", compileScope, rawSource);
        compileScope.put("name", compileScope, name);

        execJs(compileScope, "(dust.loadSource(dust.compile(rawSource, name)))");
    }
    
    public boolean exists(String name) {
        Object result = execJs(globalScope, "dust.exists('" + name + "')");
        return (result.toString() == "true");
    }
    
    public void render(String name, String json, Writer writer) {
        Context jsCtx = Context.enter();
        
        Scriptable renderScope = jsCtx.newObject(globalScope);
        renderScope.setParentScope(globalScope);
        renderScope.put("writer", renderScope, writer);
        renderScope.put("json", renderScope, json);
        renderScope.put("name", renderScope, name);

        String renderScript = ("{   dust.render( name,  JSON.parse(json) , function( err, data) { if(err) { writer.write(err);} else { writer.write( data );}  } );   }");

        execJs(renderScope, renderScript);
    }

}
