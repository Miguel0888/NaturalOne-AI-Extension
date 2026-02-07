/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Properties;

public class MavenWrapperDownloader {

    private static final String WRAPPER_PROPERTIES = ".mvn/wrapper/maven-wrapper.properties";
    private static final String WRAPPER_JAR = ".mvn/wrapper/maven-wrapper.jar";
    private static final String WRAPPER_URL_PROPERTY = "wrapperUrl";

    public static void main(String[] args) throws Exception {
        File baseDirectory = new File(args.length > 0 ? args[0] : ".");
        baseDirectory = baseDirectory.getCanonicalFile();

        File propertiesFile = new File(baseDirectory, WRAPPER_PROPERTIES);
        if (!propertiesFile.isFile()) {
            throw new FileNotFoundException("Missing " + propertiesFile.getAbsolutePath());
        }

        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(propertiesFile)) {
            properties.load(in);
        }

        String wrapperUrl = properties.getProperty(WRAPPER_URL_PROPERTY);
        if (wrapperUrl == null || wrapperUrl.trim().isEmpty()) {
            // Default for Maven Wrapper 3.2.0
            wrapperUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar";
        }

        File jarFile = new File(baseDirectory, WRAPPER_JAR);
        File jarDir = jarFile.getParentFile();
        if (!jarDir.isDirectory() && !jarDir.mkdirs()) {
            throw new IOException("Could not create directory " + jarDir.getAbsolutePath());
        }

        downloadFile(wrapperUrl, jarFile);
    }

    private static void downloadFile(String urlString, File destination) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
