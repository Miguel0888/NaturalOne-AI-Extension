package de.bund.zrb.natural.tools.baseline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
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
 * Baseline tool: read a workspace file.
 */
public final class WorkspaceReadFileTool implements Tool {

    public static final String ID = "workspace.readFile";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ToolDescriptor describe() {
        return new ToolDescriptor(ID, "Read workspace file", "Reads a file from the Eclipse workspace", "Workspace",
                ToolCapability.READ, ToolRiskLevel.SAFE,
                ToolSchema.builder().hint("Input as JSON map")
                        .field("path", "Workspace-relative path, e.g. /project/folder/file.txt")
                        .field("maxBytes", "Max bytes to read (default 20000)")
                        .build(),
                ToolSchema.builder().hint("Output")
                        .field("path", "Path")
                        .field("content", "UTF-8 content")
                        .field("truncated", "true if maxBytes was hit")
                        .build(),
                "{ \"path\": \"/target-platform/pom.xml\", \"maxBytes\": 20000 }");
    }

    @Override
    public boolean supports(ToolContext context) {
        return context != null && context.getWorkspaceRoot() != null;
    }

    @Override
    public ToolResult execute(ToolRequest request, ToolContext context, IProgressMonitor monitor) {
        String path = asString(request, "path");
        if (path == null || path.trim().isEmpty()) {
            return ToolResult.error("Missing argument: path", null);
        }

        int maxBytes = asInt(request, "maxBytes", 20000);
        if (maxBytes <= 0) {
            maxBytes = 20000;
        }

        IWorkspaceRoot root = context.getWorkspaceRoot();
        IPath p = org.eclipse.core.runtime.Path.fromPortableString(path);
        IFile file = root.getFile(p);
        if (file == null || !file.exists()) {
            return ToolResult.error("Workspace file not found: " + path, null);
        }

        try {
            ReadResult rr = readFile(file, maxBytes);
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("path", path);
            payload.put("content", rr.content);
            payload.put("truncated", Boolean.valueOf(rr.truncated));
            return ToolResult.ok("Read " + path + (rr.truncated ? " (truncated)" : ""), payload);
        } catch (Exception ex) {
            return ToolResult.error("Failed to read: " + path, String.valueOf(ex));
        }
    }

    private static ReadResult readFile(IFile file, int maxBytes) throws CoreException, IOException {
        InputStream in = null;
        try {
            in = file.getContents(true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            int total = 0;
            boolean truncated = false;
            while ((read = in.read(buf)) != -1) {
                int allowed = Math.min(read, Math.max(0, maxBytes - total));
                if (allowed > 0) {
                    bos.write(buf, 0, allowed);
                    total += allowed;
                }
                if (total >= maxBytes) {
                    truncated = true;
                    break;
                }
            }
            String content = new String(bos.toByteArray(), Charset.forName("UTF-8"));
            return new ReadResult(content, truncated);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
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

    private static final class ReadResult {
        final String content;
        final boolean truncated;

        ReadResult(String content, boolean truncated) {
            this.content = content;
            this.truncated = truncated;
        }
    }
}

