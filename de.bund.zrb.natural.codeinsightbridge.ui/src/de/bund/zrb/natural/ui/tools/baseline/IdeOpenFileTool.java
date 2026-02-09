package de.bund.zrb.natural.ui.tools.baseline;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.bund.zrb.natural.tools.api.Tool;
import de.bund.zrb.natural.tools.api.ToolCapability;
import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;
import de.bund.zrb.natural.tools.api.ToolRiskLevel;
import de.bund.zrb.natural.tools.api.ToolSchema;

/**
 * UI tool: open a workspace file in an editor.
 */
public final class IdeOpenFileTool implements Tool {

    public static final String ID = "ide.openFile";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ToolDescriptor describe() {
        return new ToolDescriptor(ID, "Open file", "Opens a workspace file in the IDE", "IDE", ToolCapability.READ,
                ToolRiskLevel.CAUTION,
                ToolSchema.builder().hint("Input")
                        .field("file", "Workspace path, e.g. /project/folder/file.txt")
                        .field("line", "Optional line number (1-based)")
                        .field("column", "Optional column (1-based)")
                        .build(),
                ToolSchema.builder().hint("Output")
                        .field("opened", "true if editor was opened")
                        .field("file", "Path")
                        .build(),
                "{ \"file\": \"/target-platform/pom.xml\", \"line\": 1, \"column\": 1 }");
    }

    @Override
    public boolean supports(ToolContext context) {
        return context != null && context.getWorkspaceRoot() != null && Display.getDefault() != null;
    }

    @Override
    public ToolResult execute(final ToolRequest request, final ToolContext context, IProgressMonitor monitor) {
        final String file = asString(request, "file");
        if (file == null || file.trim().isEmpty()) {
            return ToolResult.error("Missing argument: file", null);
        }

        final Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("file", file);

        final boolean[] ok = new boolean[] { false };
        final String[] err = new String[] { null };

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window == null) {
                        err[0] = "No active workbench window";
                        return;
                    }
                    IWorkbenchPage page = window.getActivePage();
                    if (page == null) {
                        err[0] = "No active workbench page";
                        return;
                    }

                    IWorkspaceRoot root = context.getWorkspaceRoot();
                    IPath p = org.eclipse.core.runtime.Path.fromPortableString(file);
                    IFile wsFile = root.getFile(p);
                    if (wsFile == null || !wsFile.exists()) {
                        err[0] = "Workspace file not found: " + file;
                        return;
                    }

                    IDE.openEditor(page, wsFile, true);
                    ok[0] = true;
                } catch (PartInitException ex) {
                    err[0] = String.valueOf(ex);
                } catch (Exception ex) {
                    err[0] = String.valueOf(ex);
                }
            }
        });

        payload.put("opened", Boolean.valueOf(ok[0]));
        if (!ok[0]) {
            return ToolResult.error("Failed to open editor", err[0]);
        }
        return ToolResult.ok("Opened " + file, payload);
    }

    private static String asString(ToolRequest req, String key) {
        Object v = req.getArguments().get(key);
        return v == null ? null : String.valueOf(v);
    }
}
