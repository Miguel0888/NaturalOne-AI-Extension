package de.bund.zrb.natural.ui.tools;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;

final class ToolConfirmationDialog {

    enum Decision {
        ALLOW_ONCE,
        ALLOW_ALWAYS,
        DENY_ONCE,
        DENY_NEVER
    }

    private final Shell shell;
    private final ToolDescriptor descriptor;
    private final ToolRequest request;

    ToolConfirmationDialog(Shell shell, ToolDescriptor descriptor, ToolRequest request) {
        this.shell = shell;
        this.descriptor = descriptor;
        this.request = request;
    }

    Decision openAndGetDecision() {
        String title = "Tool-Bestätigung";
        String message = buildMessage();

        String[] buttons = new String[] { "Einmal erlauben", "Immer erlauben", "Ablehnen", "Nie erlauben" };
        MessageDialog dlg = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttons, 0);
        int rc = dlg.open();
        switch (rc) {
        case 0:
            return Decision.ALLOW_ONCE;
        case 1:
            return Decision.ALLOW_ALWAYS;
        case 2:
            return Decision.DENY_ONCE;
        case 3:
            return Decision.DENY_NEVER;
        default:
            return Decision.DENY_ONCE;
        }
    }

    private String buildMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Die KI möchte folgendes Tool ausführen:\n\n");
        sb.append(descriptor.getDisplayName());
        sb.append(" (" + descriptor.getId() + ")\n");
        if (descriptor.getDescription() != null && !descriptor.getDescription().isEmpty()) {
            sb.append(descriptor.getDescription());
            sb.append("\n");
        }
        sb.append("\nArgumente:\n");
        for (Map.Entry<String, Object> e : request.getArguments().entrySet()) {
            sb.append("- ");
            sb.append(e.getKey());
            sb.append(": ");
            sb.append(String.valueOf(e.getValue()));
            sb.append("\n");
        }
        return sb.toString();
    }
}

