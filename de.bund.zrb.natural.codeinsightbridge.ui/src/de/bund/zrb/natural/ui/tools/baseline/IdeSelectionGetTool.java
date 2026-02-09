package de.bund.zrb.natural.ui.tools.baseline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.core.runtime.IProgressMonitor;

import de.bund.zrb.natural.tools.api.Tool;
import de.bund.zrb.natural.tools.api.ToolCapability;
import de.bund.zrb.natural.tools.api.ToolContext;
import de.bund.zrb.natural.tools.api.ToolDescriptor;
import de.bund.zrb.natural.tools.api.ToolRequest;
import de.bund.zrb.natural.tools.api.ToolResult;
import de.bund.zrb.natural.tools.api.ToolRiskLevel;
import de.bund.zrb.natural.tools.api.ToolSchema;

/**
 * UI tool: returns current selection (structured or text selection).
 */
public final class IdeSelectionGetTool implements Tool {

    public static final String ID = "ide.selection.get";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ToolDescriptor describe() {
        return new ToolDescriptor(ID, "Get current selection",
                "Returns the current selection (Project Explorer selection and/or editor selection)", "IDE",
                ToolCapability.READ, ToolRiskLevel.CAUTION, ToolSchema.builder().hint("Input")
                        .field("maxChars", "Max characters to return if selection text is available (default 2000)")
                        .build(),
                ToolSchema.builder().hint("Output")
                        .field("kind", "none|structured|text")
                        .field("items", "For structured selection: resource paths")
                        .field("file", "For editor: active editor file path")
                        .field("offset", "For text selection")
                        .field("length", "For text selection")
                        .field("startLine", "For text selection")
                        .field("endLine", "For text selection")
                        .build(),
                "{ \"maxChars\": 2000 }");
    }

    @Override
    public boolean supports(ToolContext context) {
        return Display.getDefault() != null;
    }

    @Override
    public ToolResult execute(ToolRequest request, ToolContext context, IProgressMonitor monitor) {
        final int maxChars = asInt(request, "maxChars", 2000);

        final Map<String, Object> payload = new LinkedHashMap<String, Object>();
        final String[] err = new String[] { null };

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window == null) {
                        payload.put("kind", "none");
                        return;
                    }

                    // Structured selection (views)
                    ISelectionService ss = window.getSelectionService();
                    ISelection sel = ss == null ? null : ss.getSelection();
                    if (sel instanceof IStructuredSelection) {
                        IStructuredSelection ssel = (IStructuredSelection) sel;
                        List<String> items = new ArrayList<String>();
                        for (Object o : ssel.toArray()) {
                            if (o instanceof IResource) {
                                items.add(((IResource) o).getFullPath().toPortableString());
                            } else {
                                items.add(String.valueOf(o));
                            }
                            if (items.size() >= 50) {
                                break;
                            }
                        }
                        if (!items.isEmpty()) {
                            payload.put("kind", "structured");
                            payload.put("items", items);
                            return;
                        }
                    }

                    // Active editor selection
                    IWorkbenchPage page = window.getActivePage();
                    IEditorPart editor = page == null ? null : page.getActiveEditor();
                    if (editor != null && editor.getEditorInput() instanceof FileEditorInput) {
                        FileEditorInput fei = (FileEditorInput) editor.getEditorInput();
                        if (fei.getFile() != null) {
                            payload.put("file", fei.getFile().getFullPath().toPortableString());
                        }
                    }

                    ISelection editorSel = (page == null) ? null : page.getSelection();
                    if (editorSel instanceof ITextSelection) {
                        ITextSelection ts = (ITextSelection) editorSel;
                        payload.put("kind", "text");
                        payload.put("offset", Integer.valueOf(ts.getOffset()));
                        payload.put("length", Integer.valueOf(ts.getLength()));
                        payload.put("startLine", Integer.valueOf(ts.getStartLine()));
                        payload.put("endLine", Integer.valueOf(ts.getEndLine()));
                        // We intentionally don't fetch actual selected text from the document in v1.
                        payload.put("text", "");
                        payload.put("truncated", Boolean.FALSE);
                        return;
                    }

                    payload.put("kind", "none");
                } catch (Exception ex) {
                    err[0] = String.valueOf(ex);
                }
            }
        });

        if (err[0] != null) {
            return ToolResult.error("Failed to read selection", err[0]);
        }
        return ToolResult.ok("Selection read", payload);
    }

    private static int asInt(ToolRequest req, String key, int def) {
        Object v = req.getArguments().get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }
}
