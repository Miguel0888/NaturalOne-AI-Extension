package de.bund.zrb.natural.ui.chat.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Read bundle resources as bytes or text.
 */
public final class BundleResourceReader {

    private final Bundle bundle;
    private final ILog log;

    public BundleResourceReader(Bundle bundle) {
        this.bundle = Objects.requireNonNull(bundle, "bundle must not be null");
        this.log = Platform.getLog(this.bundle);
    }

    /**
     * Read a UTF-8 resource from the bundle.
     * Throw an exception when the resource cannot be found or read.
     */
    public String readUtf8(String path) {
        byte[] bytes = readBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a UTF-8 resource from the bundle.
     * Return the fallback when the resource cannot be found or read.
     */
    public String readUtf8OrFallback(String path, String fallback) {
        try {
            return readUtf8(path);
        } catch (RuntimeException ex) {
            logMissingResource(path, ex);
            return fallback;
        }
    }

    /**
     * Read a UTF-8 resource from the bundle.
     * Return an empty string when the resource cannot be found or read.
     */
    public String readUtf8OrEmpty(String path) {
        return readUtf8OrFallback(path, "");
    }

    /**
     * Read a binary resource from the bundle.
     * Throw an exception when the resource cannot be found or read.
     */
    public byte[] readBytes(String path) {
        try (InputStream in = open(path)) {
            return toByteArray(in);
        } catch (IOException ex) {
            throw new IllegalStateException(buildReadFailedMessage(path), ex);
        }
    }

    /**
     * Open a resource stream from the bundle.
     * Throw an exception when the resource cannot be found.
     */
    public InputStream open(String path) throws IOException {
        URL url = findResourceUrl(path);
        if (url == null) {
            throw new IllegalStateException(buildNotFoundMessage(path));
        }
        return url.openStream();
    }

    private URL findResourceUrl(String path) throws IOException {
        String normalized = normalize(path);
        // Try direct bundle entry lookup first.
        URL url = bundle.getEntry(normalized);
        if (url != null) {
            return url;
        }
        // Some callers accidentally pass a leading slash.
        url = bundle.getEntry("/" + normalized);
        if (url != null) {
            return url;
        }

        // Use FileLocator to resolve resources from JARs and fragments.
        url = FileLocator.find(bundle, new Path(normalized), null);
        if (url != null) {
            return url;
        }

        // Try again with a leading slash for completeness.
        url = FileLocator.find(bundle, new Path("/" + normalized), null);
        if (url != null) {
            return url;
        }
        return null;
    }

    private String normalize(String path) {
        String p = Objects.requireNonNull(path, "path must not be null").trim();
        p = p.replace('\\', '/');

        // Remove leading slash to make bundle lookups consistent.
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
        byte[] buffer = new byte[8 * 1024];

        // Read fully into memory to keep API simple.
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private void logMissingResource(String path, RuntimeException ex) {
        String message = buildNotFoundMessage(path);
        log.log(new Status(Status.WARNING, bundle.getSymbolicName(), message, ex));
        }
    private String buildNotFoundMessage(String path) {
        return "Resource not found: " + path
                + " (bundle=" + bundle.getSymbolicName() + "). "
                + "Ensure the resource is included in the built bundle (e.g. build.properties bin.includes: css/, js/, fonts/, icons/).";
        }
    private String buildReadFailedMessage(String path) {
        return "Failed to read resource: " + path
                + " (bundle=" + bundle.getSymbolicName() + ").";
    }
}
