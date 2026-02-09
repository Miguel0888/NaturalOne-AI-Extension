package de.bund.zrb.natural.ui.tools;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolOrigin;
import de.bund.zrb.natural.tools.api.ToolRiskLevel;
import de.bund.zrb.natural.tools.core.ToolPolicyDecision;
import de.bund.zrb.natural.tools.core.ToolPolicyEvaluator;

/**
 * Persists tool policies in Eclipse InstanceScope preferences.
 */
public final class ToolPolicyStore implements ToolPolicyEvaluator {

    public static final String PREF_MANUAL_EXECUTION_ENABLED = "tools.manualExecutionEnabled";

    private static final String NODE = "de.bund.zrb.natural.codeinsightbridge.ui";
    private static final String KEY_PREFIX = "tools.policy.";

    private final IEclipsePreferences prefs;

    public ToolPolicyStore() {
        this.prefs = InstanceScope.INSTANCE.getNode(NODE);
    }

    public ToolPolicy getPolicy(String toolId) {
        String v = prefs.get(KEY_PREFIX + toolId, null);
        if (v == null) {
            return null;
        }
        try {
            return ToolPolicy.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setPolicy(String toolId, ToolPolicy policy) {
        if (toolId == null) {
            return;
        }
        if (policy == null) {
            prefs.remove(KEY_PREFIX + toolId);
        } else {
            prefs.put(KEY_PREFIX + toolId, policy.name());
        }
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            // ignore
        }
    }

    public boolean isManualExecutionEnabled() {
        return prefs.getBoolean(PREF_MANUAL_EXECUTION_ENABLED, false);
    }

    @Override
    public ToolPolicyDecision getPolicy(String toolId, ToolDescriptor descriptor, ToolOrigin origin) {
        // For USER runs: always allow (debug/test)
        if (origin == ToolOrigin.USER) {
            return ToolPolicyDecision.ALWAYS;
        }

        ToolPolicy stored = getPolicy(toolId);
        if (stored != null) {
            switch (stored) {
            case ALWAYS:
                return ToolPolicyDecision.ALWAYS;
            case NEVER:
                return ToolPolicyDecision.NEVER;
            case ASK:
            default:
                return ToolPolicyDecision.ASK;
            }
        }

        // Default policy based on risk/capability.
        if (descriptor != null) {
            if (descriptor.getRiskLevel() == ToolRiskLevel.SAFE) {
                return ToolPolicyDecision.ALWAYS;
            }
            // Write-tools are ASK for now.
            return ToolPolicyDecision.ASK;
        }

        return ToolPolicyDecision.ASK;
    }
}

