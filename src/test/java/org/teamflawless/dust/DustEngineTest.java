package org.teamflawless.dust;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import lombok.Cleanup;

import org.junit.Test;

public class DustEngineTest {

    private static final String RESOURCE_PATH = "/org/teamflawless/dust/";
    
    private static final String TEMPLATE_NAME = "testResource.dust";
    private static final String TEMPLATE_NAME_SECOND = "anotherResource.dust";
    
    @Test
    public void testBasics() throws Exception {
        
        @Cleanup DustEngine engine = new DustEngine();
        
        List<String> templates = new ArrayList<String>();
        templates.add(RESOURCE_PATH + TEMPLATE_NAME);
        engine.setDustTemplates(templates);
        engine.init();
        
        @Cleanup StringWriter writer = new StringWriter(); 
        
//        engine.loadTemplate("test", "Hello {name}! You have {count} new messages.\n");
        
        Assert.assertTrue(engine.exists(TEMPLATE_NAME));
        Assert.assertFalse(engine.exists("invalidTemplate"));
        
        engine.render(TEMPLATE_NAME, "{ \"name\": \"Mick\", \"count\": 30}", writer);
        writer.flush();
        
        String expected = "Hello Mick! You have 30 new messages.\n";
        String actual = writer.getBuffer().toString();
        
        Assert.assertEquals(expected, actual);
        
    }
    
    @Test
    public void testWildcards() throws Exception {
        @Cleanup DustEngine engine = new DustEngine();
        
        List<String> templates = new ArrayList<String>();
        templates.add(RESOURCE_PATH + "*.dust");
        engine.setDustTemplates(templates);
        engine.init();
        
        Assert.assertTrue(engine.exists(TEMPLATE_NAME));
        Assert.assertTrue(engine.exists(TEMPLATE_NAME_SECOND));
        Assert.assertFalse(engine.exists("invalidTemplate"));
        
    }
    
    @Test
    public void testReset() throws Exception {
        
        @Cleanup DustEngine engine = new DustEngine();
        
        List<String> templates = new ArrayList<String>();
        templates.add(RESOURCE_PATH + TEMPLATE_NAME);
        engine.setDustTemplates(templates);
        engine.init();
        
        @Cleanup StringWriter writer = new StringWriter(); 
        
        engine.render(TEMPLATE_NAME, "{ \"name\": \"Mick\", \"count\": 30}", writer);
        writer.flush();
        
        String expected = "Hello Mick! You have 30 new messages.\n";
        String actual = writer.getBuffer().toString();
        
        Assert.assertEquals(expected, actual);

        engine.setDustTemplates(null);
        engine.reset();
        
        Assert.assertFalse(engine.exists(TEMPLATE_NAME));
        
        engine.loadTemplate(TEMPLATE_NAME, "{name}, your count is {count}...");
        
        Assert.assertTrue(engine.exists(TEMPLATE_NAME));

        @Cleanup StringWriter writer2 = new StringWriter();
        engine.render(TEMPLATE_NAME, "{ \"name\": \"Mick\", \"count\": 30}", writer2);
        writer2.flush();
        
        String expected2nd = "Mick, your count is 30...";
        String actual2nd = writer2.getBuffer().toString();
        
        Assert.assertEquals(expected2nd, actual2nd);
    }

    @Test
    public void testUTF8Encoding() throws Exception {
        
        @Cleanup DustEngine engine = new DustEngine();
        
        engine.loadTemplate(TEMPLATE_NAME, "¡Héllø {name}!");
        
        Assert.assertTrue(engine.exists(TEMPLATE_NAME));

        @Cleanup StringWriter writer = new StringWriter();
        engine.render(TEMPLATE_NAME, "{ \"name\": \"Yårñ\"}", writer);
        writer.flush();
        
        String expected = "¡Héllø Yårñ!";
        String actual = writer.getBuffer().toString();
        
        Assert.assertEquals(expected, actual);
    }
}
