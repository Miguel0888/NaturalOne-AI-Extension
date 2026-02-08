package de.bund.zrb.natural.ui.chat.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Read resources from an OSGi bundle or classpath.
 *
 * Try multiple lookup strategies and provide fallback reads so UI can keep running.
 */
public final class BundleResourceReader {

    private final Class<?> anchorClass;

    public BundleResourceReader(Class<?> anchorClass) {
        this.anchorClass = Objects.requireNonNull(anchorClass, "anchorClass must not be null");
    }

    public String readUtf8(String path) {
        return readString(path, Charset.forName("UTF-8"));
    }

    public String readUtf8OrEmpty(String path) {
        return readUtf8OrFallback(path, "");
    }

    public String readUtf8OrFallback(String path, String fallback) {
        try {
            return readUtf8(path);
        } catch (RuntimeException ex) {
            logMissingResource(path, ex);
            return fallback;
        }
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
        } catch (IOException ex) {
            throw new IllegalStateException(buildReadFailedMessage(path), ex);
        } finally {
            closeQuietly(in);
        }
    }

    public InputStream open(String path) throws IOException {
        URL url = findResourceUrl(path);
        if (url == null) {
            throw new IllegalStateException(buildNotFoundMessage(path));
        }
        return url.openStream();
    }

    /**
     * List bundle entries for diagnostics (e.g. listBundleEntries("/css", "*.css", true)).
     */
    public List<String> listBundleEntries(String root, String filePattern, boolean recurse) {
        Bundle bundle = getBundle();
        if (bundle == null) {
            return new ArrayList<String>();
        }

        String normalizedRoot = normalizeRoot(root);
        Enumeration<URL> entries = bundle.findEntries(normalizedRoot, filePattern, recurse);

        List<String> result = new ArrayList<String>();
        if (entries == null) {
            return result;
        }

        while (entries.hasMoreElements()) {
            URL u = entries.nextElement();
            result.add(String.valueOf(u));
        }
        return result;
    }

    private URL findResourceUrl(String path) throws IOException {
        String normalized = normalizePath(path);

        Bundle bundle = getBundle();
        if (bundle != null) {
            // 1) Direct bundle lookup
            URL url = tryBundleEntry(bundle, normalized);
            if (url != null) {
                return url;
            }

            // 2) FileLocator resolves from JARs/fragments
            url = FileLocator.find(bundle, new Path(normalized), null);
            if (url != null) {
                return url;
            }

            // 3) Best effort: search by basename if path mapping is broken
            url = tryFindByBasename(bundle, normalized);
            if (url != null) {
                return url;
            }
        }

        // 4) Fallback: classpath lookup
        URL classUrl = anchorClass.getResource("/" + normalized);
        if (classUrl != null) {
            return classUrl;
        }
        ClassLoader cl = anchorClass.getClassLoader();
        if (cl != null) {
            URL clUrl = cl.getResource(normalized);
            if (clUrl != null) {
                return clUrl;
            }
        }

        return null;
    }

    private URL tryBundleEntry(Bundle bundle, String normalized) {
        URL url = bundle.getEntry(normalized);
        if (url != null) {
            return url;
        }
        url = bundle.getEntry("/" + normalized);
        if (url != null) {
            return url;
        }
        return null;
    }

    private URL tryFindByBasename(Bundle bundle, String normalized) {
        int idx = normalized.lastIndexOf('/');
        if (idx < 0 || idx >= normalized.length() - 1) {
            return null;
        }

        String fileName = normalized.substring(idx + 1);
        Enumeration<URL> urls = bundle.findEntries("/", fileName, true);
        if (urls == null || !urls.hasMoreElements()) {
            return null;
        }
        return urls.nextElement();
    }

    private Bundle getBundle() {
        return FrameworkUtil.getBundle(anchorClass);
    }

    private void logMissingResource(String path, RuntimeException ex) {
        Bundle bundle = getBundle();
        if (bundle == null) {
            return;
        }

        ILog log = Platform.getLog(bundle);

        String message = buildNotFoundMessage(path)
                + "\nAvailable CSS entries: " + listBundleEntries("/css", "*.css", true)
                + "\nAvailable JS entries: " + listBundleEntries("/js", "*.js", true);

        log.log(new Status(Status.WARNING, bundle.getSymbolicName(), message, ex));
    }

    private String buildNotFoundMessage(String path) {
        Bundle bundle = getBundle();
        String symbolicName = bundle == null ? "<no-osgi-bundle>" : bundle.getSymbolicName();

        return "Resource not found: " + path
                + " (bundle=" + symbolicName + "). "
                + "Ensure resources are included in the built bundle JAR (build.properties bin.includes: css/, js/, fonts/, icons/).";
    }

    private String buildReadFailedMessage(String path) {
        Bundle bundle = getBundle();
        String symbolicName = bundle == null ? "<no-osgi-bundle>" : bundle.getSymbolicName();
        return "Failed to read resource: " + path + " (bundle=" + symbolicName + ").";
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private static String normalizeRoot(String root) {
        if (root == null || root.trim().isEmpty()) {
            return "/";
        }

        String r = root.trim().replace('\\', '/');
        if (!r.startsWith("/")) {
            r = "/" + r;
        }
        return r;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
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
