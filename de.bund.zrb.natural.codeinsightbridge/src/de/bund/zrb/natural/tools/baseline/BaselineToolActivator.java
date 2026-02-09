package de.bund.zrb.natural.tools.baseline;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.bund.zrb.natural.tools.api.Tool;

/**
 * Registers baseline tools as OSGi services.
 */
public final class BaselineToolActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        // Baseline tools: low ranking so enhanced tools can override.
        props.put("service.ranking", Integer.valueOf(0));

        context.registerService(Tool.class.getName(), new WorkspaceReadFileTool(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing
    }
}

