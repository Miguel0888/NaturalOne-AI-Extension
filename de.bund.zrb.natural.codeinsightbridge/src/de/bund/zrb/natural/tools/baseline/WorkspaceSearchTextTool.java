package de.bund.zrb.natural.tools.baseline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
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
 * Baseline tool: search text in workspace files.
 *
 * Note: For maximum compatibility with the current target platform, v1 uses a
 * simple IResource traversal + line-based matching. This supports regex and
 * cancel via IProgressMonitor.
 */
public final class WorkspaceSearchTextTool implements Tool {

    public static final String ID = "workspace.searchText";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ToolDescriptor describe() {
        return new ToolDescriptor(ID, "Search text in workspace",
                "Searches workspace files for plain text or regex and returns structured matches", "Workspace",
                ToolCapability.READ, ToolRiskLevel.SAFE,
                ToolSchema.builder().hint("Input")
                        .field("query", "Plain text or regex")
                        .field("isRegex", "true for regex (default false)")
                        .field("caseSensitive", "default false")
                        .field("fileGlobs", "Optional globs; v1 supports simple suffix filtering like **/*.java")
                        .field("maxResults", "Max total matches (default 200)")
                        .field("maxFiles", "Max files to scan (default 5000)")
                        .field("contextLines", "Lines of context before/after (default 1, max 3)")
                        .build(),
                ToolSchema.builder().hint("Output")
                        .field("matches", "List of matches")
                        .field("truncated", "true if limits were hit")
                        .field("stats", "filesScanned, durationMs")
                        .build(),
                "{ \"query\": \"CALL\\\\s+MYPROG\", \"isRegex\": true, \"caseSensitive\": false, \"fileGlobs\": [\"**/*.NSN\", \"**/*.NSP\", \"**/*.java\", \"**/*.xml\"], \"maxResults\": 200, \"maxFiles\": 5000, \"contextLines\": 1 }");
    }

    @Override
    public boolean supports(ToolContext context) {
        return context != null && context.getWorkspaceRoot() != null;
    }

    @Override
    public ToolResult execute(ToolRequest request, ToolContext context, IProgressMonitor monitor) {
        long start = System.currentTimeMillis();

        String query = asString(request, "query");
        if (query == null || query.trim().isEmpty()) {
            return ToolResult.error("Missing argument: query", null);
        }

        boolean isRegex = asBoolean(request, "isRegex", false);
        boolean caseSensitive = asBoolean(request, "caseSensitive", false);
        int maxResults = asInt(request, "maxResults", 200);
        int maxFiles = asInt(request, "maxFiles", 5000);
        int contextLines = asInt(request, "contextLines", 1);
        if (maxResults <= 0) {
            maxResults = 200;
        }
        if (maxFiles <= 0) {
            maxFiles = 5000;
        }
        if (contextLines < 0) {
            contextLines = 0;
        }
        if (contextLines > 3) {
            contextLines = 3;
        }
        final int maxResultsFinal = maxResults;
        final int maxFilesFinal = maxFiles;
        final int contextLinesFinal = contextLines;

        List<String> fileGlobs = asStringList(request.getArguments().get("fileGlobs"));
        final SuffixFilter suffixFilter = SuffixFilter.fromGlobs(fileGlobs);

        final Pattern pattern;
        if (isRegex) {
            int flags = Pattern.MULTILINE;
            if (!caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            pattern = Pattern.compile(query, flags);
        } else {
            pattern = null;
        }
        final String needle = isRegex ? null : (caseSensitive ? query : query.toLowerCase());

        final IWorkspaceRoot root = context.getWorkspaceRoot();
        final List<Map<String, Object>> matches = new ArrayList<Map<String, Object>>();
        final boolean[] truncated = new boolean[] { false };
        final int[] filesScanned = new int[] { 0 };

        try {
            root.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    if (monitor != null && monitor.isCanceled()) {
                        throw new CoreException(org.eclipse.core.runtime.Status.CANCEL_STATUS);
                    }
                    if (resource instanceof IFile) {
                        if (filesScanned[0] >= maxFilesFinal) {
                            truncated[0] = true;
                            return false;
                        }
                        IFile f = (IFile) resource;
                        if (!suffixFilter.accept(f.getName())) {
                            return true;
                        }
                        filesScanned[0]++;
                        scanFile(f, pattern, needle, caseSensitive, contextLinesFinal, maxResultsFinal, matches, truncated, monitor);
                        if (matches.size() >= maxResultsFinal) {
                            truncated[0] = true;
                            return false;
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
            return ToolResult.error("Failed to search workspace", String.valueOf(ex));
        }

        long durationMs = System.currentTimeMillis() - start;
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("filesScanned", Integer.valueOf(filesScanned[0]));
        stats.put("durationMs", Long.valueOf(durationMs));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("matches", matches);
        payload.put("truncated", Boolean.valueOf(truncated[0]));
        payload.put("stats", stats);

        return ToolResult.ok("Found " + matches.size() + " match(es)" + (truncated[0] ? " (truncated)" : ""), payload);
    }

    private static void scanFile(IFile file, Pattern pattern, String needle, boolean caseSensitive, int contextLines,
            int maxResults, List<Map<String, Object>> matches, boolean[] truncated, IProgressMonitor monitor) {
        InputStream in = null;
        try {
            in = file.getContents(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));

            // Keep a sliding window for context.
            List<String> prev = new ArrayList<String>();
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (monitor != null && monitor.isCanceled()) {
                    return;
                }

                List<MatchPos> positions = findMatchesInLine(line, pattern, needle, caseSensitive);
                if (!positions.isEmpty()) {
                    // collect after-lines lazily by reading ahead
                    List<String> after = contextLines > 0 ? readNextLines(reader, contextLines) : Collections.<String>emptyList();

                    for (int i = 0; i < positions.size(); i++) {
                        MatchPos mp = positions.get(i);
                        Map<String, Object> m = new LinkedHashMap<String, Object>();
                        m.put("file", file.getFullPath().toPortableString());
                        m.put("line", Integer.valueOf(lineNo));
                        m.put("column", Integer.valueOf(mp.column));
                        m.put("matchText", mp.text);
                        m.put("lineText", line);
                        m.put("before", new ArrayList<String>(prev));
                        m.put("after", new ArrayList<String>(after));
                        matches.add(m);
                        if (matches.size() >= maxResults) {
                            truncated[0] = true;
                            return;
                        }
                    }

                    // After-lines were consumed by readNextLines, so we need to update the prev buffer with
                    // the current line and those after-lines, and continue from there.
                    pushPrev(prev, line, contextLines);
                    for (int i = 0; i < after.size(); i++) {
                        line = after.get(i);
                        lineNo++;
                        pushPrev(prev, line, contextLines);
                    }
                    continue;
                }

                pushPrev(prev, line, contextLines);
            }
        } catch (Exception ex) {
            // ignore file-level issues (binary, encoding etc.)
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

    private static void pushPrev(List<String> prev, String line, int contextLines) {
        if (contextLines <= 0) {
            return;
        }
        prev.add(line);
        while (prev.size() > contextLines) {
            prev.remove(0);
        }
    }

    private static List<String> readNextLines(BufferedReader reader, int n) throws IOException {
        if (n <= 0) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            reader.mark(64 * 1024);
            String l = reader.readLine();
            if (l == null) {
                break;
            }
            lines.add(l);
        }
        return lines;
    }

    private static List<MatchPos> findMatchesInLine(String line, Pattern pattern, String needle, boolean caseSensitive) {
        if (line == null) {
            return Collections.emptyList();
        }
        if (pattern != null) {
            Matcher m = pattern.matcher(line);
            List<MatchPos> res = new ArrayList<MatchPos>();
            while (m.find()) {
                int col = m.start() + 1;
                String text = safeSubstring(line, m.start(), m.end());
                res.add(new MatchPos(col, text));
            }
            return res;
        }

        String hay = caseSensitive ? line : line.toLowerCase();
        int idx = hay.indexOf(needle);
        if (idx < 0) {
            return Collections.emptyList();
        }
        List<MatchPos> res = new ArrayList<MatchPos>();
        while (idx >= 0) {
            res.add(new MatchPos(idx + 1, safeSubstring(line, idx, idx + needle.length())));
            idx = hay.indexOf(needle, idx + 1);
        }
        return res;
    }

    private static String safeSubstring(String s, int start, int end) {
        int a = Math.max(0, Math.min(start, s.length()));
        int b = Math.max(a, Math.min(end, s.length()));
        return s.substring(a, b);
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

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object v) {
        if (v instanceof List) {
            List<?> l = (List<?>) v;
            List<String> out = new ArrayList<String>();
            for (int i = 0; i < l.size(); i++) {
                Object o = l.get(i);
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        if (v instanceof String[]) {
            String[] a = (String[]) v;
            List<String> out = new ArrayList<String>();
            for (int i = 0; i < a.length; i++) {
                out.add(a[i]);
            }
            return out;
        }
        return Collections.emptyList();
    }

    private static final class MatchPos {
        final int column;
        final String text;

        MatchPos(int column, String text) {
            this.column = column;
            this.text = text;
        }
    }

    private static final class SuffixFilter {
        private final List<String> suffixes;

        private SuffixFilter(List<String> suffixes) {
            this.suffixes = suffixes;
        }

        boolean accept(String name) {
            if (suffixes == null || suffixes.isEmpty()) {
                return true;
            }
            for (int i = 0; i < suffixes.size(); i++) {
                if (name.endsWith(suffixes.get(i))) {
                    return true;
                }
            }
            return false;
        }

        static SuffixFilter fromGlobs(List<String> globs) {
            if (globs == null || globs.isEmpty()) {
                return new SuffixFilter(Collections.<String>emptyList());
            }
            List<String> s = new ArrayList<String>();
            for (int i = 0; i < globs.size(); i++) {
                String g = globs.get(i);
                if (g == null) {
                    continue;
                }
                String t = g.trim();
                // support **/*.ext
                int star = t.lastIndexOf("*.");
                if (star >= 0 && star + 2 < t.length()) {
                    s.add(t.substring(star + 1)); // includes dot
                }
            }
            return new SuffixFilter(s);
        }
    }
}

