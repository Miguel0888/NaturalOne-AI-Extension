package de.bund.zrb.natural.tools.core;

import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;

/**
 * UI callback for ASK decisions.
 */
public interface ToolConfirmationHandler {

    ToolUserDecision confirm(ToolDescriptor descriptor, ToolRequest request, ToolContext context);
}

