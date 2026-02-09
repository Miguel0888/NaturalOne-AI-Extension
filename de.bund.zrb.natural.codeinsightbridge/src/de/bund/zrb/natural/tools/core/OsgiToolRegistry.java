package de.bund.zrb.natural.tools.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.bund.zrb.natural.tools.api.Tool;
import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;

/**
 * Simple registry backed by OSGi service lookups.
 */
public final class OsgiToolRegistry implements ToolRegistry {

    private static final String PROP_RANKING = "service.ranking";
    private static final String BASELINE_BUNDLE_SYMBOLIC_NAME = "de.bund.zrb.natural.codeinsightbridge";

    private final BundleContext bundleContext;

    public OsgiToolRegistry(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public List<ToolDescriptor> listAllTools(ToolContext context) {
        List<ToolRef> tools = getAllTools();
        Map<String, ToolRef> best = new HashMap<String, ToolRef>();

        for (ToolRef t : tools) {
            if (t == null || t.tool == null) {
                continue;
            }
            if (context != null && !t.tool.supports(context)) {
                continue;
            }
            String id = t.tool.getId();
            ToolRef current = best.get(id);
            if (current == null || t.ranking > current.ranking) {
                best.put(id, t);
            }
        }

        List<ToolDescriptor> descriptors = new ArrayList<ToolDescriptor>();
        for (ToolRef t : best.values()) {
            descriptors.add(t.tool.describe());
        }

        Collections.sort(descriptors, new Comparator<ToolDescriptor>() {
            @Override
            public int compare(ToolDescriptor a, ToolDescriptor b) {
                int cat = safe(a.getCategory()).compareToIgnoreCase(safe(b.getCategory()));
                if (cat != 0) {
                    return cat;
                }
                return safe(a.getDisplayName()).compareToIgnoreCase(safe(b.getDisplayName()));
            }

            private String safe(String s) {
                return s == null ? "" : s;
            }
        });

        return descriptors;
    }

    @Override
    public Tool resolveBestTool(String toolId, ToolContext context) {
        if (toolId == null) {
            return null;
        }

        ToolRef best = null;
        for (ToolRef t : getAllTools()) {
            if (t == null || t.tool == null) {
                continue;
            }
            if (!toolId.equals(t.tool.getId())) {
                continue;
            }
            if (context != null && !t.tool.supports(context)) {
                continue;
            }
            if (best == null || t.ranking > best.ranking) {
                best = t;
            }
        }

        return best == null ? null : best.tool;
    }

    private List<ToolRef> getAllTools() {
        if (bundleContext == null) {
            return Collections.emptyList();
        }

        List<ToolRef> tools = getAllToolsOnce();
        if (!tools.isEmpty()) {
            return tools;
        }

        // Best-effort: ensure baseline bundle is started so its BundleActivator can register tools.
        tryStartBaselineBundle();

        return getAllToolsOnce();
    }

    private List<ToolRef> getAllToolsOnce() {
        List<ToolRef> tools = new ArrayList<ToolRef>();
        try {
            ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(Tool.class.getName(), null);
            if (refs == null) {
                return Collections.emptyList();
            }
            for (ServiceReference<?> r : refs) {
                @SuppressWarnings("unchecked")
                ServiceReference<Tool> tr = (ServiceReference<Tool>) r;

                Tool service = bundleContext.getService(tr);
                if (service != null) {
                    tools.add(new ToolRef(service, tr, getRanking(tr)));
                }
            }
        } catch (InvalidSyntaxException e) {
            return Collections.emptyList();
        }

        return tools;
    }

    private void tryStartBaselineBundle() {
        try {
            Bundle b = bundleContext.getBundle(BASELINE_BUNDLE_SYMBOLIC_NAME);
            if (b == null) {
                // Fallback: search by name
                for (Bundle candidate : bundleContext.getBundles()) {
                    if (candidate != null && BASELINE_BUNDLE_SYMBOLIC_NAME.equals(candidate.getSymbolicName())) {
                        b = candidate;
                        break;
                    }
                }
            }
            if (b == null) {
                return;
            }
            int state = b.getState();
            if (state != Bundle.ACTIVE && state != Bundle.STARTING) {
                b.start(Bundle.START_TRANSIENT);
            }
        } catch (BundleException ex) {
            // best-effort
        } catch (RuntimeException ex) {
            // best-effort
        }
    }

    private int getRanking(ServiceReference<?> ref) {
        if (ref == null) {
            return 0;
        }
        Object v = ref.getProperty(PROP_RANKING);
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        return 0;
    }

    private static final class ToolRef {
        final Tool tool;
        @SuppressWarnings("unused")
        final ServiceReference<Tool> ref;
        final int ranking;

        ToolRef(Tool tool, ServiceReference<Tool> ref, int ranking) {
            this.tool = tool;
            this.ref = ref;
            this.ranking = ranking;
        }
    }
}
