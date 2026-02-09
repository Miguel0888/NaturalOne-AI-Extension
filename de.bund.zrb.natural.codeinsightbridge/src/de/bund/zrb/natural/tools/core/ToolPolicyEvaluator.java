package de.bund.zrb.natural.tools.core;

import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolOrigin;

/**
 * Abstracts policy storage (UI bundle) from core execution.
 */
public interface ToolPolicyEvaluator {

    ToolPolicyDecision getPolicy(String toolId, ToolDescriptor descriptor, ToolOrigin origin);
}

