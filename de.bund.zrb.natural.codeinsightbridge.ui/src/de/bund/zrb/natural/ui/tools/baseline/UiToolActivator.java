package de.bund.zrb.natural.ui.tools.baseline;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.bund.zrb.natural.tools.api.Tool;

/**
 * Registers UI/IDE related tools as OSGi services.
 */
public final class UiToolActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        // UI tools: still baseline, but can be overridden by enhanced UI tool implementations.
        props.put("service.ranking", Integer.valueOf(0));

        context.registerService(Tool.class.getName(), new IdeOpenFileTool(), props);
        context.registerService(Tool.class.getName(), new IdeSelectionGetTool(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing
    }
}
