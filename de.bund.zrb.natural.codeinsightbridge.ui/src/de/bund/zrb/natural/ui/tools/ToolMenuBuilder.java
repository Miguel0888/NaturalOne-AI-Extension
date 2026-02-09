package de.bund.zrb.natural.ui.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolOrigin;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;
import de.bund.zrb.natural.tools.core.ToolGateway;
import de.bund.zrb.natural.tools.core.ToolGatewayImpl;

public final class ToolMenuBuilder {

    public interface ToolResultSink {
        void onToolResult(ToolDescriptor descriptor, ToolRequest request, ToolResult result);
    }

    private final Shell shell;
    private final ToolContext context;
    private final ToolPolicyStore policyStore;
    private final ToolGateway gateway;

    public ToolMenuBuilder(Shell shell, ToolContext context, ToolPolicyStore policyStore) {
        this.shell = shell;
        this.context = context;
        this.policyStore = policyStore;

        BundleContext bc = FrameworkUtil.getBundle(ToolMenuBuilder.class).getBundleContext();
        this.gateway = new ToolGatewayImpl(bc, policyStore, new ToolUiConfirmationHandler(shell, policyStore));
    }

    public void rebuildMenu(Menu menu, final ToolResultSink sink) {
        for (MenuItem it : menu.getItems()) {
            it.dispose();
        }

        List<ToolDescriptor> tools = gateway.listAdvertisedTools(context);
        if (tools.isEmpty()) {
            MenuItem empty = new MenuItem(menu, SWT.NONE);
            empty.setText("Keine Tools verfügbar");
            empty.setEnabled(false);
            return;
        }

        Map<String, Menu> categoryMenus = new LinkedHashMap<String, Menu>();

        for (final ToolDescriptor d : tools) {
            String cat = d.getCategory() == null ? "Tools" : d.getCategory();
            Menu catMenu = categoryMenus.get(cat);
            if (catMenu == null) {
                MenuItem parentItem = new MenuItem(menu, SWT.CASCADE);
                parentItem.setText(cat);
                catMenu = new Menu(menu);
                parentItem.setMenu(catMenu);
                categoryMenus.put(cat, catMenu);
            }

            MenuItem toolItem = new MenuItem(catMenu, SWT.PUSH);
            toolItem.setText(d.getDisplayName());
            toolItem.setToolTipText(d.getDescription());
            toolItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ToolRequest req = new ToolRequest(d.getId(), java.util.Collections.<String, Object>emptyMap(), ToolOrigin.USER,
                            "user-" + String.valueOf(System.currentTimeMillis()));
                    ToolResult res = gateway.executeTool(req, context);
                    if (sink != null) {
                        sink.onToolResult(d, req, res);
                    }
                }
            });

            MenuItem policyCascade = new MenuItem(catMenu, SWT.CASCADE);
            policyCascade.setText("Policy…");
            Menu polMenu = new Menu(catMenu);
            policyCascade.setMenu(polMenu);

            addPolicyItem(polMenu, d, ToolPolicy.ALWAYS, "Immer erlauben");
            addPolicyItem(polMenu, d, ToolPolicy.ASK, "Auf Nachfrage");
            addPolicyItem(polMenu, d, ToolPolicy.NEVER, "Nie erlauben");

            if (policyStore.isManualExecutionEnabled()) {
                new MenuItem(catMenu, SWT.SEPARATOR);
                MenuItem run = new MenuItem(catMenu, SWT.PUSH);
                run.setText("Tool testen…");
                run.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ToolManualRunDialog dlg = new ToolManualRunDialog(shell, d, gateway, context);
                        ToolResult res = dlg.openAndRun();
                        if (sink != null && res != null) {
                            ToolRequest r = dlg.getLastRequest();
                            sink.onToolResult(d, r, res);
                        }
                    }
                });
            }

            new MenuItem(catMenu, SWT.SEPARATOR);
        }
    }

    private void addPolicyItem(Menu menu, final ToolDescriptor d, final ToolPolicy policy, String label) {
        final MenuItem it = new MenuItem(menu, SWT.RADIO);
        it.setText(label);
        ToolPolicy current = policyStore.getPolicy(d.getId());
        if (current != null) {
            it.setSelection(current == policy);
        }
        it.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (it.getSelection()) {
                    policyStore.setPolicy(d.getId(), policy);
                }
            }
        });
    }
}

