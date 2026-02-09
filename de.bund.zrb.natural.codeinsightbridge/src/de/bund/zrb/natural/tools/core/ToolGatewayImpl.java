package de.bund.zrb.natural.tools.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.BundleContext;

import de.bund.zrb.natural.tools.api.Tool;
import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolOrigin;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;

/**
 * Default gateway that enforces policies and optionally asks the UI.
 */
public final class ToolGatewayImpl implements ToolGateway {

    private final ToolRegistry registry;
    private final ToolPolicyEvaluator policyEvaluator;
    private final ToolConfirmationHandler confirmationHandler;

    public ToolGatewayImpl(BundleContext context, ToolPolicyEvaluator policyEvaluator, ToolConfirmationHandler confirmationHandler) {
        this.registry = new OsgiToolRegistry(context);
        this.policyEvaluator = policyEvaluator;
        this.confirmationHandler = confirmationHandler;
    }

    @Override
    public List<ToolDescriptor> listAdvertisedTools(ToolContext context) {
        List<ToolDescriptor> all = registry.listAllTools(context);
        List<ToolDescriptor> advertised = new ArrayList<ToolDescriptor>();
        for (ToolDescriptor d : all) {
            ToolPolicyDecision p = policyEvaluator == null ? ToolPolicyDecision.ASK
                    : policyEvaluator.getPolicy(d.getId(), d, ToolOrigin.AI);
            if (p != ToolPolicyDecision.NEVER) {
                advertised.add(d);
            }
        }
        return advertised;
    }

    @Override
    public ToolResult executeTool(ToolRequest request, ToolContext context) {
        if (request == null || request.getToolId() == null) {
            return ToolResult.error("Tool request is missing toolId", null);
        }

        Tool tool = registry.resolveBestTool(request.getToolId(), context);
        if (tool == null) {
            return ToolResult.error("Tool not found: " + request.getToolId(), null);
        }

        ToolDescriptor desc = tool.describe();
        ToolOrigin origin = request.getOrigin();

        ToolPolicyDecision policy = policyEvaluator == null ? ToolPolicyDecision.ASK
                : policyEvaluator.getPolicy(desc.getId(), desc, origin);

        if (policy == ToolPolicyDecision.NEVER) {
            return ToolResult.denied("Tool is not allowed by policy: " + desc.getDisplayName());
        }

        if (policy == ToolPolicyDecision.ASK && origin == ToolOrigin.AI) {
            if (confirmationHandler == null) {
                return ToolResult.denied("Tool requires confirmation, but no confirmation handler is available");
            }
            ToolUserDecision userDecision = confirmationHandler.confirm(desc, request, context);
            if (userDecision == null || !userDecision.isAllowOnce()) {
                return ToolResult.denied("Tool execution was denied by user: " + desc.getDisplayName());
            }
        }

        IProgressMonitor monitor = new NullProgressMonitor();
        try {
            return tool.execute(request, context, monitor);
        } catch (RuntimeException ex) {
            return ToolResult.error("Tool failed: " + desc.getDisplayName(), String.valueOf(ex));
        }
    }
}

