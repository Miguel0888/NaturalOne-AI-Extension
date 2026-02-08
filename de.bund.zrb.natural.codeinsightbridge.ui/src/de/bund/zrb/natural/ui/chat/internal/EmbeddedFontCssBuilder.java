package de.bund.zrb.natural.ui.chat.internal;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Build @font-face CSS with embedded base64 data URLs.
 */
public final class EmbeddedFontCssBuilder {

    private final BundleResourceReader resourceReader;

    public EmbeddedFontCssBuilder(BundleResourceReader resourceReader) {
        this.resourceReader = resourceReader;
    }

    public String buildCss() {
        List<FontSpec> fonts = new ArrayList<FontSpec>();

        // Font Awesome (used by fa6.all.min.css)
        fonts.add(new FontSpec("fonts/fa-regular-400.ttf", "Font Awesome 6 Free", "400", "normal"));
        fonts.add(new FontSpec("fonts/fa-solid-900.ttf", "Font Awesome 6 Free", "900", "normal"));

        // KaTeX fonts (used by katex.min.css)
        addKatexFonts(fonts);

        StringBuilder out = new StringBuilder();
        for (FontSpec font : fonts) {
            out.append(toFontFace(font)).append("\n\n");
        }
        return out.toString();
    }

    private void addKatexFonts(List<FontSpec> fonts) {
        String[] katexFontFiles = new String[] {
                "fonts/KaTeX_AMS-Regular.ttf",
                "fonts/KaTeX_Caligraphic-Bold.ttf",
                "fonts/KaTeX_Caligraphic-Regular.ttf",
                "fonts/KaTeX_Fraktur-Bold.ttf",
                "fonts/KaTeX_Fraktur-Regular.ttf",
                "fonts/KaTeX_Main-Bold.ttf",
                "fonts/KaTeX_Main-BoldItalic.ttf",
                "fonts/KaTeX_Main-Italic.ttf",
                "fonts/KaTeX_Main-Regular.ttf",
                "fonts/KaTeX_Math-BoldItalic.ttf",
                "fonts/KaTeX_Math-Italic.ttf",
                "fonts/KaTeX_SansSerif-Bold.ttf",
                "fonts/KaTeX_SansSerif-Italic.ttf",
                "fonts/KaTeX_SansSerif-Regular.ttf",
                "fonts/KaTeX_Script-Regular.ttf",
                "fonts/KaTeX_Size1-Regular.ttf",
                "fonts/KaTeX_Size2-Regular.ttf",
                "fonts/KaTeX_Size3-Regular.ttf",
                "fonts/KaTeX_Size4-Regular.ttf",
                "fonts/KaTeX_Typewriter-Regular.ttf"
        };

        for (String path : katexFontFiles) {
            FontSpec spec = katexSpecFor(path);
            fonts.add(spec);
        }
    }

    private FontSpec katexSpecFor(String path) {
        String fileName = path;
        int slash = fileName.lastIndexOf('/');
        if (slash >= 0) {
            fileName = fileName.substring(slash + 1);
        }
        String base = fileName.replaceAll("\\.[^.]+$", "");
        String family = base;
        String weight = "normal";
        String style = "normal";

        // Derive family and style from filename like KaTeX_Main-BoldItalic
        int dash = base.lastIndexOf('-');
        if (dash > 0) {
            family = base.substring(0, dash);
            String variant = base.substring(dash + 1);
            if (variant.toLowerCase().contains("bold")) {
                weight = "bold";
            }
            if (variant.toLowerCase().contains("italic")) {
                style = "italic";
            }
            if (variant.toLowerCase().contains("regular")) {
                weight = "normal";
                style = "normal";
            }
        }
        return new FontSpec(path, family, weight, style);
    }

    private String toFontFace(FontSpec spec) {
        byte[] bytes = resourceReader.readBytes(spec.path);
        String base64 = Base64.getEncoder().encodeToString(bytes);

        // Use truetype to keep it compatible with SWT Browser engines.
        StringBuilder css = new StringBuilder();
        css.append("@font-face {");
        css.append("font-family: '").append(escapeCss(spec.family)).append("';");
        css.append("src: url('data:font/truetype;base64,").append(base64).append("') format('truetype');");
        css.append("font-weight: ").append(spec.weight).append(";");
        css.append("font-style: ").append(spec.style).append(";");
        css.append("}");
        return css.toString();
    }

    private static String escapeCss(String text) {
        return text == null ? "" : text.replace("'", "\\'");
    }

    private static final class FontSpec {
        private final String path;
        private final String family;
        private final String weight;
        private final String style;

        private FontSpec(String path, String family, String weight, String style) {
            this.path = path;
            this.family = family;
            this.weight = weight;
            this.style = style;
        }
    }
}
