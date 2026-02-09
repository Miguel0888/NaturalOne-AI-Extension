package de.bund.zrb.natural.tools.core;

import java.util.List;

import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;

/**
 * Entry point for AI integrations.
 */
public interface ToolGateway {

    List<ToolDescriptor> listAdvertisedTools(ToolContext context);

    ToolResult executeTool(ToolRequest request, ToolContext context);
}

