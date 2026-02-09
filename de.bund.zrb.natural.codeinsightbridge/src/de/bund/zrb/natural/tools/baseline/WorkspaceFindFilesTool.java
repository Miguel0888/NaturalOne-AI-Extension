package de.bund.zrb.natural.tools.baseline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
 * Baseline tool: find workspace files by name pattern.
 */
public final class WorkspaceFindFilesTool implements Tool {

    public static final String ID = "workspace.findFiles";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ToolDescriptor describe() {
        return new ToolDescriptor(ID, "Find files in workspace",
                "Finds files by name (regex or substring) in the Eclipse workspace", "Workspace", ToolCapability.READ,
                ToolRiskLevel.SAFE,
                ToolSchema.builder().hint("Input")
                        .field("namePattern", "File name pattern. If isRegex=true: Java regex; else substring")
                        .field("isRegex", "true to interpret namePattern as regex (default false)")
                        .field("fileGlobs", "Optional file globs like **/*.java (v1 supports simple suffix matching if provided)")
                        .field("maxResults", "Max results (default 200)")
                        .build(),
                ToolSchema.builder().hint("Output")
                        .field("files", "Workspace-relative paths")
                        .field("truncated", "true if maxResults was hit")
                        .build(),
                "{ \"namePattern\": \".*\\\\.NSN\", \"isRegex\": true, \"maxResults\": 200 }");
    }

    @Override
    public boolean supports(ToolContext context) {
        return context != null && context.getWorkspaceRoot() != null;
    }

    @Override
    public ToolResult execute(ToolRequest request, ToolContext context, IProgressMonitor monitor) {
        String namePattern = asString(request, "namePattern");
        if (namePattern == null || namePattern.trim().isEmpty()) {
            return ToolResult.error("Missing argument: namePattern", null);
        }
        boolean isRegex = asBoolean(request, "isRegex", false);
        int maxResults = asInt(request, "maxResults", 200);
        if (maxResults <= 0) {
            maxResults = 200;
        }
        final int maxResultsFinal = maxResults;

        final Pattern regex = isRegex ? Pattern.compile(namePattern) : null;
        final String needle = isRegex ? null : namePattern;

        final IWorkspaceRoot root = context.getWorkspaceRoot();
        final List<String> files = new ArrayList<String>();
        final boolean[] truncated = new boolean[] { false };

        try {
            root.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    if (monitor != null && monitor.isCanceled()) {
                        throw new CoreException(org.eclipse.core.runtime.Status.CANCEL_STATUS);
                    }
                    if (resource instanceof IFile) {
                        IFile f = (IFile) resource;
                        String name = f.getName();
                        boolean match;
                        if (regex != null) {
                            match = regex.matcher(name).matches();
                        } else {
                            match = name.indexOf(needle) >= 0;
                        }
                        if (match) {
                            IPath p = f.getFullPath();
                            files.add(p.toPortableString());
                            if (files.size() >= maxResultsFinal) {
                                truncated[0] = true;
                                return false;
                            }
                        }
                        return true;
                    }
                    return true;
                }
            });
        } catch (CoreException ex) {
            if (ex.getStatus() != null && ex.getStatus().getSeverity() == org.eclipse.core.runtime.IStatus.CANCEL) {
                return ToolResult.error("Canceled", null);
            }
            return ToolResult.error("Failed to scan workspace", String.valueOf(ex));
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("files", files);
        payload.put("truncated", Boolean.valueOf(truncated[0]));
        return ToolResult.ok("Found " + files.size() + " file(s)" + (truncated[0] ? " (truncated)" : ""), payload);
    }

    private static String asString(ToolRequest req, String key) {
        Object v = req.getArguments().get(key);
        return v == null ? null : String.valueOf(v);
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

    private static boolean asBoolean(ToolRequest req, String key, boolean def) {
        Object v = req.getArguments().get(key);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v != null) {
            return Boolean.parseBoolean(String.valueOf(v));
        }
        return def;
    }
}
