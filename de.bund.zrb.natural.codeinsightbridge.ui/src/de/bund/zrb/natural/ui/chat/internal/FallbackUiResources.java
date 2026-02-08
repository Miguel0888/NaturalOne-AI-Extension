package de.bund.zrb.natural.ui.chat.internal;


/**
 * Provide minimal inline CSS/JS so the ChatView can open even when the real UI assets are missing.
 */
public final class FallbackUiResources {


    private FallbackUiResources() {
// Prevent instantiation
    }


    public static String minimalCss() {
        return "body{font-family:Arial, sans-serif; margin:0; padding:12px; background:#111; color:#eee;}"
                + "#content{max-width:900px; margin:0 auto;}"
                + ".chat-bubble{border-radius:14px; padding:10px 12px; margin:10px 0;}"
                + ".me{background:#2b3a67;}"
                + ".you{background:#222;}"
                + ".message-toolbar{font-size:12px; opacity:0.8; margin-bottom:6px;}"
                + "#notification-container{position:fixed; top:12px; right:12px; z-index:9999;}"
                + ".notification{padding:8px 10px; border-radius:10px; margin-bottom:8px; background:#333; color:#fff;}";
    }


    public static String minimalJs() {
// Provide functions that ChatView calls (renderCode/showNotification/removeNotification).
        return "function renderCode(){ /* no-op fallback */ }"
                + "function showNotification(id, icon, bg, fg, msg){"
                + " var c=document.getElementById('notification-container');"
                + " if(!c){return;}"
                + " var n=document.createElement('div');"
                + " n.setAttribute('id', id);"
                + " n.setAttribute('class','notification');"
                + " n.style.background=bg; n.style.color=fg;"
                + " n.textContent=msg;"
                + " c.appendChild(n);"
                + "}"
                + "function removeNotification(id){"
                + " var n=document.getElementById(id); if(n){n.remove();}"
                + "}";
    }


    public static String missingAssetsBannerHtml(String text) {
        String safe = escapeHtml(text);
        return "<div style=\"max-width:900px;margin:0 auto 12px auto;padding:10px 12px;border-radius:12px;"
                + "background:#3a1f1f;color:#ffdede;border:1px solid #7a0000;\">"
                + "<b>AssistAI UI Assets fehlen</b><br/>"
                + safe
                + "</div>";
    }


    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        String r = s;
        r = r.replace("&", "&amp;");
        r = r.replace("<", "&lt;");
        r = r.replace(">", "&gt;");
        r = r.replace("\"", "&quot;");
        return r;
    }
}