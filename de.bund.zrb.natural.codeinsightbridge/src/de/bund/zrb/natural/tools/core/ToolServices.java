package de.bund.zrb.natural.tools.core;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Small helper for accessing OSGi services without DS.
 */
public final class ToolServices {

    private ToolServices() {
    }

    public static <T> T getService(Class<T> type) {
        BundleContext ctx = FrameworkUtil.getBundle(ToolServices.class).getBundleContext();
        if (ctx == null) {
            return null;
        }
        ServiceReference<T> ref = ctx.getServiceReference(type);
        if (ref == null) {
            return null;
        }
        return ctx.getService(ref);
    }
}

