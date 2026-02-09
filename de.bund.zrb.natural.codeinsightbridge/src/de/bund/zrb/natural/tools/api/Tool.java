package de.bund.zrb.natural.tools.api;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Stable SPI for tool implementations.
 */
public interface Tool {

    /** Stable tool id, e.g. "workspace.readFile". */
    String getId();

    ToolDescriptor describe();

    /** Return true if this tool can run in the given context. */
    boolean supports(ToolContext context);

    ToolResult execute(ToolRequest request, ToolContext context, IProgressMonitor monitor);
}

