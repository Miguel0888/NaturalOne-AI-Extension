package de.bund.zrb.natural.ui.tools;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.core.ToolConfirmationHandler;
import de.bund.zrb.natural.tools.core.ToolUserDecision;

/**
 * Shows an ASK dialog on the UI thread.
 */
public final class ToolUiConfirmationHandler implements ToolConfirmationHandler {

    private final Shell parentShell;
    private final ToolPolicyStore policyStore;

    public ToolUiConfirmationHandler(Shell parentShell, ToolPolicyStore policyStore) {
        this.parentShell = parentShell;
        this.policyStore = policyStore;
    }

    @Override
    public ToolUserDecision confirm(final ToolDescriptor descriptor, final ToolRequest request, final ToolContext context) {
        final ToolUserDecision[] result = new ToolUserDecision[1];

        Runnable r = new Runnable() {
            @Override
            public void run() {
                ToolConfirmationDialog dlg = new ToolConfirmationDialog(parentShell, descriptor, request);
                ToolConfirmationDialog.Decision d = dlg.openAndGetDecision();
                if (d == null) {
                    result[0] = ToolUserDecision.denyOnce();
                    return;
                }

                switch (d) {
                case ALLOW_ONCE:
                    result[0] = ToolUserDecision.allowOnce();
                    break;
                case ALLOW_ALWAYS:
                    policyStore.setPolicy(descriptor.getId(), ToolPolicy.ALWAYS);
                    result[0] = ToolUserDecision.allowAndRememberAlways();
                    break;
                case DENY_ONCE:
                    result[0] = ToolUserDecision.denyOnce();
                    break;
                case DENY_NEVER:
                    policyStore.setPolicy(descriptor.getId(), ToolPolicy.NEVER);
                    result[0] = ToolUserDecision.denyAndRememberNever();
                    break;
                default:
                    result[0] = ToolUserDecision.denyOnce();
                    break;
                }
            }
        };

        Display display = parentShell != null ? parentShell.getDisplay() : Display.getDefault();
        if (Display.getCurrent() != null) {
            r.run();
        } else {
            display.syncExec(r);
        }

        return result[0];
    }
}

