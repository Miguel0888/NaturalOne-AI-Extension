package de.bund.zrb.natural.ui.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolOrigin;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;
import de.bund.zrb.natural.tools.core.ToolGateway;

final class ToolManualRunDialog extends Dialog {

    private final ToolDescriptor descriptor;
    private final ToolGateway gateway;
    private final ToolContext context;

    private Text json;
    private ToolRequest lastRequest;
    private ToolResult lastResult;

    ToolManualRunDialog(Shell parentShell, ToolDescriptor descriptor, ToolGateway gateway, ToolContext context) {
        super(parentShell);
        this.descriptor = descriptor;
        this.gateway = gateway;
        this.context = context;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite c = new Composite(area, SWT.NONE);
        c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        c.setLayout(new GridLayout(1, false));

        json = new Text(c, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        json.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        json.setText(getInitialJson());

        return area;
    }

    private String getInitialJson() {
        String example = descriptor == null ? null : descriptor.getExampleArgumentsJson();
        if (example == null || example.trim().isEmpty()) {
            return "{}\n";
        }
        String t = example.trim();
        // keep a trailing newline so the text box feels natural
        return t.endsWith("\n") ? t : (t + "\n");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Execute", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        Map<String, Object> args = parseJsonLikeMap(json.getText());
        lastRequest = new ToolRequest(descriptor.getId(), args, ToolOrigin.USER, "manual-" + String.valueOf(System.currentTimeMillis()));
        lastResult = gateway.executeTool(lastRequest, context);
        super.okPressed();
    }

    ToolResult openAndRun() {
        int rc = open();
        return rc == OK ? lastResult : null;
    }

    ToolRequest getLastRequest() {
        return lastRequest;
    }

    // v1: super minimal, accepts: { "k": "v", "n": 1 }
    private static Map<String, Object> parseJsonLikeMap(String text) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (text == null) {
            return map;
        }
        String t = text.trim();
        if (t.startsWith("{") && t.endsWith("}")) {
            t = t.substring(1, t.length() - 1).trim();
        }
        if (t.isEmpty()) {
            return map;
        }
        String[] parts = t.split(",");
        for (String p : parts) {
            String[] kv = p.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            String k = unquote(kv[0].trim());
            String v = kv[1].trim();
            Object val;
            if (v.startsWith("\"") && v.endsWith("\"")) {
                val = unquote(v);
            } else {
                try {
                    val = Integer.valueOf(v.trim());
                } catch (NumberFormatException e) {
                    val = unquote(v);
                }
            }
            map.put(k, val);
        }
        return map;
    }

    private static String unquote(String s) {
        String t = s;
        if (t.startsWith("\"")) {
            t = t.substring(1);
        }
        if (t.endsWith("\"")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}

