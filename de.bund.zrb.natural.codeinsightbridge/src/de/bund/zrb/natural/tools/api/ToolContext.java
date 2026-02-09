package de.bund.zrb.natural.tools.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

/**
 * Minimal context for tool selection/execution.
 */
public final class ToolContext {

    private final IWorkspaceRoot workspaceRoot;
    private final IProject activeProject;

    public ToolContext(IWorkspaceRoot workspaceRoot, IProject activeProject) {
        this.workspaceRoot = workspaceRoot;
        this.activeProject = activeProject;
    }

    public IWorkspaceRoot getWorkspaceRoot() {
        return workspaceRoot;
    }

    public IProject getActiveProject() {
        return activeProject;
    }
}

