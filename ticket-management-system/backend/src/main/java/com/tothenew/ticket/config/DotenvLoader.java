package com.tothenew.ticket.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads KEY=VALUE pairs from a local {@code .env} file into JVM system properties
 * so Spring {@code ${...}} placeholders resolve without committing secrets.
 */
public final class DotenvLoader {

    private DotenvLoader() {}

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = unquote(trimmed.substring(separator + 1).trim());
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Optional file; Spring will fail later if required properties are missing.
        }
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of(".env");
        if (Files.isRegularFile(cwd)) {
            return cwd;
        }
        Path fromRepoRoot = Path.of("backend", ".env");
        if (Files.isRegularFile(fromRepoRoot)) {
            return fromRepoRoot;
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
