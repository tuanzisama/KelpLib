package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.api.storage.SqlDialect;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 迁移：从插件 jar 资源 {@code migrations/{dialect}/V*.sql} 与 {@code migrations/common/}
 * 查找脚本（方言目录优先），经版本表记录增量执行（§6.9）。
 */
final class Migrations {

    private static final Pattern SCRIPT = Pattern.compile("V(\\d+)__(.+)\\.sql");
    private static final String TABLE = "kelplib_migrations";

    record Script(String name, int version, String content) {
    }

    private Migrations() {
    }

    /** 增量执行迁移脚本（幂等）。 */
    static void apply(Connection connection, ClassLoader loader, SqlDialect dialect, String namespace) throws SQLException {
        ensureTable(connection);
        Set<String> applied = loadApplied(connection, namespace);
        for (Script script : collectScripts(loader, dialect)) {
            if (applied.contains(script.name())) {
                continue;
            }
            execute(connection, script);
            recordApplied(connection, namespace, script.name());
        }
    }

    private static void ensureTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "plugin VARCHAR(64) NOT NULL, script VARCHAR(255) NOT NULL, applied_at TIMESTAMP NOT NULL, "
                    + "PRIMARY KEY (plugin, script))");
        }
    }

    private static Set<String> loadApplied(Connection connection, String namespace) throws SQLException {
        Set<String> applied = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT script FROM " + TABLE + " WHERE plugin = ?")) {
            ps.setString(1, namespace);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applied.add(rs.getString(1));
                }
            }
        }
        return applied;
    }

    private static void recordApplied(Connection connection, String namespace, String script) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO " + TABLE + " (plugin, script, applied_at) VALUES (?, ?, ?)")) {
            ps.setString(1, namespace);
            ps.setString(2, script);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    private static void execute(Connection connection, Script script) throws SQLException {
        for (String statement : splitStatements(script.content())) {
            if (statement.isBlank()) {
                continue;
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(statement);
            }
        }
    }

    private static List<String> splitStatements(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(current.substring(0, current.lastIndexOf(";")));
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            statements.add(current.toString());
        }
        return statements;
    }

    /** 方言目录脚本优先，同名覆盖 common 目录脚本；按版本号升序。 */
    static List<Script> collectScripts(ClassLoader loader, SqlDialect dialect) {
        String dialectDir = "migrations/" + dialect.name().toLowerCase(java.util.Locale.ROOT);
        String commonDir = "migrations/common";
        List<Script> scripts = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Script script : readScripts(loader, dialectDir)) {
            scripts.add(script);
            names.add(script.name());
        }
        for (Script script : readScripts(loader, commonDir)) {
            if (names.add(script.name())) {
                scripts.add(script);
            }
        }
        scripts.sort((a, b) -> {
            int byVersion = Integer.compare(a.version(), b.version());
            return byVersion != 0 ? byVersion : a.name().compareTo(b.name());
        });
        return scripts;
    }

    private static List<Script> readScripts(ClassLoader loader, String dir) {
        List<Script> scripts = new ArrayList<>();
        try {
            Enumeration<URL> resources = loader.getResources(dir);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> readFromDirectory(Path.of(url.toURI()), scripts);
                    case "jar" -> readFromJar(url, dir, scripts);
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan migrations in " + dir, e);
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Invalid migration resource URL", e);
        }
        return scripts;
    }

    private static void readFromDirectory(Path dir, List<Script> scripts) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                parseScript(file.getFileName().toString(), Files.readString(file, StandardCharsets.UTF_8))
                        .ifPresent(scripts::add);
            }
        }
    }

    private static void readFromJar(URL url, String dir, List<Script> scripts) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(dir + "/") && name.endsWith(".sql")) {
                    try (InputStream input = jar.getInputStream(entry)) {
                        parseScript(Path.of(name).getFileName().toString(),
                                new String(input.readAllBytes(), StandardCharsets.UTF_8)).ifPresent(scripts::add);
                    }
                }
            }
        }
    }

    private static java.util.Optional<Script> parseScript(String fileName, String content) {
        Matcher matcher = SCRIPT.matcher(fileName);
        if (!matcher.matches()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new Script(fileName, Integer.parseInt(matcher.group(1)), content));
    }
}
