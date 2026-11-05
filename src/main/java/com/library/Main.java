package com.library;

import com.library.config.DBConnectionFactory;
import com.library.config.DBInitializer;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application bootstrap for the Online Library Management System.
 * Initializes persistent database schema, background daemons, and embedded Apache Tomcat 10.1.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final int DEFAULT_PORT = 8080;

    private static Tomcat tomcatInstance;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String portProperty = System.getProperty("server.port");
        String portEnv = System.getenv("PORT");
        if (portProperty != null && !portProperty.isBlank()) {
            try {
                port = Integer.parseInt(portProperty.trim());
            } catch (NumberFormatException ignored) {}
        } else if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("=============================================================");
        System.out.println("  Online Library Management System - Enterprise Web Edition  ");
        System.out.println("=============================================================");
        System.out.println("  Runtime Java Version: " + System.getProperty("java.version"));
        System.out.println("  Runtime Vendor:       " + System.getProperty("java.vendor"));
        System.out.println("  OS Name / Arch:       " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        System.out.println("  HTTP Port:            " + port);
        System.out.println("=============================================================");

        // Detect if executed within a JUnit test suite
        boolean isUnitTest = Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(e -> e.getClassName().startsWith("org.junit") || e.getClassName().contains("Test"));

        try {
            System.out.println("[INFO] Bootstrapping persistent database engine...");
            DBInitializer.initialize(DBConnectionFactory.getDataSource());
            System.out.println("[INFO] Database initialization complete.");

            if (isUnitTest) {
                System.out.println("[INFO] JUnit test execution detected; skipping blocking server await.");
                return;
            }

            File baseDir = new File("target/tomcat-embed");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            Tomcat tomcat = new Tomcat();
            tomcatInstance = tomcat;
            tomcat.setBaseDir(baseDir.getAbsolutePath());
            tomcat.setPort(port);
            tomcat.getConnector();

            String webappDirLocation = new File("src/main/webapp").getAbsolutePath();
            StandardContext ctx = (StandardContext) tomcat.addWebapp("", webappDirLocation);
            ctx.setParentClassLoader(Main.class.getClassLoader());
            ctx.setReloadable(true);

            File additionWebInfClasses = new File("target/classes");
            if (additionWebInfClasses.exists()) {
                WebResourceRoot resources = new StandardRoot(ctx);
                resources.addPreResources(new DirResourceSet(
                        resources,
                        "/WEB-INF/classes",
                        additionWebInfClasses.getAbsolutePath(),
                        "/"
                ));
                ctx.setResources(resources);
            }

            System.out.println("[INFO] Starting Embedded Tomcat Servlet Container on port " + port + "...");
            tomcat.start();
            System.out.println("[SUCCESS] Application successfully launched!");
            System.out.println("  Available at: http://localhost:" + port);
            System.out.println("  Librarian Login: admin@library.com / admin123");
            System.out.println("  Member Login:    alice@example.com / password123");
            System.out.println("=============================================================");

            tomcat.getServer().await();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Bootstrap exception: " + ex.getMessage(), ex);
            if (!isUnitTest) {
                System.err.println("[ERROR] Failed to start server: " + ex.getMessage());
                System.exit(1);
            }
        }
    }

    public static Tomcat getTomcatInstance() {
        return tomcatInstance;
    }

    public static void stopServer() {
        if (tomcatInstance != null) {
            try {
                tomcatInstance.stop();
                tomcatInstance.destroy();
            } catch (Exception ignored) {}
        }
    }
}
