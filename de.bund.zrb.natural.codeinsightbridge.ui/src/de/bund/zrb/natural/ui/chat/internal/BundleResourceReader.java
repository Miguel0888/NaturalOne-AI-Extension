package de.bund.zrb.natural.ui.chat.internal;


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