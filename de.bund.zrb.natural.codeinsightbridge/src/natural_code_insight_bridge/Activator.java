package natural_code_insight_bridge;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Manage the OSGi bundle lifecycle.
 */
public final class Activator implements BundleActivator {

    private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());

    @Override
    public void start(BundleContext context) {
        LOGGER.log(Level.INFO, "Natural Code Insight Bridge started. Bundle={0}", context.getBundle().getSymbolicName());
    }

    @Override
    public void stop(BundleContext context) {
        LOGGER.log(Level.INFO, "Natural Code Insight Bridge stopped. Bundle={0}", context.getBundle().getSymbolicName());
    }
}
