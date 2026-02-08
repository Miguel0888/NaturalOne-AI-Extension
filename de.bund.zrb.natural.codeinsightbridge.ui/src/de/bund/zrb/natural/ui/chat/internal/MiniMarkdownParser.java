package de.bund.zrb.natural.ui.chat.internal;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert a small subset of Markdown to HTML.
 */
public final class MiniMarkdownParser {

    private static final Pattern CODE_BLOCK_FENCE = Pattern.compile("^\\s*```([a-zA-Z0-9]*)\\s*$");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    public String toHtml(String markdown) {
        if (markdown == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        boolean inCodeBlock = false;
        String currentLang = "";
        String currentCodeBlockId = null;

        String[] lines = markdown.replace("\r", "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher fence = CODE_BLOCK_FENCE.matcher(line);
            if (fence.matches()) {
                if (!inCodeBlock) {
                    inCodeBlock = true;
                    currentLang = fence.group(1) == null ? "" : fence.group(1);
                    currentCodeBlockId = UUID.randomUUID().toString();
                    out.append(openCodeBlock(currentCodeBlockId, currentLang));
                } else {
                    inCodeBlock = false;
                    out.append(closeCodeBlock());
                }
                continue;
            }

            if (inCodeBlock) {
                out.append(escapeHtml(line)).append("\n");
                continue;
            }

            out.append(convertInlineMarkdownToHtml(escapeHtml(line))).append("<br/>");
        }

        if (inCodeBlock) {
            out.append(closeCodeBlock());
        }

        return out.toString();
    }

    private static String openCodeBlock(String codeBlockId, String lang) {
        String blockClass = "diff".equals(lang) ? "diff-block" : "code-block";
        String safeLang = escapeHtml(lang);

        // Keep the structure compatible with AssistAI's textview.css / textview.js.
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"codeBlock ").append(blockClass).append("\">\n");
        html.append("  <div class=\"codeBlockButtons\">\n");
        html.append("    <input type=\"button\" onClick=\"eclipseCopyCode(document.getElementById('").append(codeBlockId).append("').innerText)\" value=\"Copy\" />\n");
        html.append("    <input class=\"code-only\" type=\"button\" onClick=\"eclipseInsertCode(document.getElementById('").append(codeBlockId).append("').innerText)\" value=\"Insert\" />\n");
        html.append("    <input class=\"code-only\" type=\"button\" onClick=\"eclipseNewFile(document.getElementById('").append(codeBlockId).append("').innerText, '").append(safeLang).append("')\" value=\"New File\" />\n");
        html.append("    <input class=\"code-only\" type=\"button\" onClick=\"eclipseDiffCode(document.getElementById('").append(codeBlockId).append("').innerText)\" value=\"Diff\" />\n");
        html.append("    <input class=\"diff-only\" type=\"button\" onClick=\"eclipseApplyPatch(document.getElementById('").append(codeBlockId).append("').innerText)\" value=\"Apply\"/>\n");
        html.append("  </div>\n");
        html.append("  <pre><code lang=\"").append(safeLang).append("\" id=\"").append(codeBlockId).append("\">");
        return html.toString();
    }

    private static String closeCodeBlock() {
        return "</code></pre></div>\n";
    }

    private static String convertInlineMarkdownToHtml(String escapedLine) {
        Matcher m = INLINE_CODE.matcher(escapedLine);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(1);
            String replacement = "<span class=\"inline-code\">" + inner + "</span>";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&#39;");
        return s;
    }
}
