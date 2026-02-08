package de.bund.zrb.natural.ui.chat.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Read bundle resources as bytes or text.
 */
public final class BundleResourceReader {

    private final Class<?> anchorClass;

    public BundleResourceReader(Class<?> anchorClass) {
        this.anchorClass = anchorClass;
    }

    public String readUtf8(String path) {
        return readString(path, Charset.forName("UTF-8"));
    }

    public String readString(String path, Charset charset) {
        byte[] bytes = readBytes(path);
        return new String(bytes, charset);
    }

    public byte[] readBytes(String path) {
        InputStream in = null;
        try {
            in = open(path);
            return readAllBytes(in);
        } catch (IOException e) {
            throw new IllegalStateException("Fail reading resource: " + path, e);
        } finally {
            closeQuietly(in);
        }
    }

    private InputStream open(String path) {
        String normalized = normalize(path);
        InputStream in = anchorClass.getResourceAsStream(normalized);
        if (in == null) {
            in = anchorClass.getClassLoader().getResourceAsStream(stripLeadingSlash(normalized));
        }
        if (in == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return in;
    }

    private static String normalize(String path) {
        if (path == null) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ignored) {
            // Ignore close failures
        }
    }
}
