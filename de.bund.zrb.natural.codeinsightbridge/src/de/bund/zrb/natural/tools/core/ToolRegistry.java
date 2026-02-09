package de.bund.zrb.natural.tools.core;

import java.util.List;

import de.bund.zrb.natural.tools.api.Tool;
import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;

public interface ToolRegistry {

    List<ToolDescriptor> listAllTools(ToolContext context);

    Tool resolveBestTool(String toolId, ToolContext context);
}

