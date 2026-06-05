package com.zrlog.plugin.backup.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.SiteExportPreviewResponse;
import com.zrlog.plugin.common.model.BlogRunTime;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SiteExportService {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Gson JSONL_GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String ATTACHED_ROOT = "/attached";
    private static final String AI_MESSAGE_PREFIX = "ai_chat_message_";
    private static final String LOG_TABLE = "log";
    private static final String COMMENT_TABLE = "comment";
    private static final List<String> SENSITIVE_TOKENS = List.of(
            "password", "token", "secret", "credential", "apikey", "accesskey", "privatekey", "proxy"
    );

    private final IOSession session;

    public SiteExportService(IOSession session) {
        this.session = session;
    }

    public SiteExportPreviewResponse preview(SiteExportPreviewResponse.SiteExportOptions options) throws SQLException {
        SiteExportDatabase.ensureConfigured(session);
        SiteExportPreviewResponse.SiteExportOptions normalizedOptions = normalizeOptions(options);
        SiteExportPreviewResponse response = new SiteExportPreviewResponse();
        String exportId = UUID.randomUUID().toString();
        response.setExportId(exportId);
        response.setGeneratedAt(System.currentTimeMillis());
        response.setPackageName("zrlog-export-" + exportId + ".zip");
        response.setOptions(normalizedOptions);
        response.setPackagePaths(buildPackagePaths(normalizedOptions));
        response.setCounts(buildCounts());
        response.setRedactions(buildRedactions(response.getCounts(), normalizedOptions));
        response.setNotes(buildNotes(normalizedOptions));
        return response;
    }

    public SiteExportPackage createPackage(SiteExportPreviewResponse.SiteExportOptions options)
            throws SQLException, IOException {
        SiteExportDatabase.ensureConfigured(session);
        SiteExportPreviewResponse.SiteExportOptions normalizedOptions = normalizeOptions(options);
        SiteExportPreviewResponse manifest = preview(normalizedOptions);
        File targetDir = new File(System.getProperty("java.io.tmpdir"), "zrlog-site-export-" + manifest.getExportId());
        if (!targetDir.mkdirs() && !targetDir.isDirectory()) {
            throw new IOException("create temp export directory failed: " + targetDir);
        }
        File target = new File(targetDir, manifest.getPackageName());
        List<String> checksums = new ArrayList<>();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            writeJson(zipOutputStream, "zrlog-export.json", manifest, checksums);
            writeJsonLines(zipOutputStream, "content/articles.jsonl", loadArticles(normalizedOptions), checksums);
            writeJson(zipOutputStream, "content/types.json", loadTypes(), checksums);
            writeJson(zipOutputStream, "content/tags.json", loadTags(), checksums);
            if (normalizedOptions.isIncludeComments()) {
                writeJsonLines(zipOutputStream, "content/comments.jsonl", loadComments(), checksums);
            }
            writeJson(zipOutputStream, "content/navs.json", loadNavs(), checksums);
            writeJson(zipOutputStream, "content/links.json", loadLinks(), checksums);
            writeJson(zipOutputStream, "site/website.json", loadWebsite(), checksums);
            writeJson(zipOutputStream, "site/redactions.json", manifest.getRedactions(), checksums);
            List<File> mediaFiles = collectMediaFiles(attachedRoot());
            writeJsonLines(zipOutputStream, "media/index.jsonl", buildMediaIndex(mediaFiles), checksums);
            if (normalizedOptions.isIncludeMediaFiles()) {
                writeMediaFiles(zipOutputStream, mediaFiles, checksums);
            }
            if (normalizedOptions.isIncludeThemeFiles()) {
                writeJson(zipOutputStream, "themes/config.json", loadThemeConfig(), checksums);
            }
            writeJson(zipOutputStream, "plugins/manifest.json", loadPluginManifest(normalizedOptions), checksums);
            writeAiMessages(zipOutputStream, checksums, normalizedOptions.isIncludeAiMessages());
            writeText(zipOutputStream, "checksums/sha256.txt", String.join("\n", checksums) + "\n", null);
        }
        return new SiteExportPackage(target, manifest.getPackageName());
    }

    private SiteExportPreviewResponse.SiteExportOptions normalizeOptions(
            SiteExportPreviewResponse.SiteExportOptions options) {
        SiteExportPreviewResponse.SiteExportOptions normalized =
                Objects.requireNonNullElseGet(options, SiteExportPreviewResponse.SiteExportOptions::new);
        normalized.setIncludePluginRuntimeState(false);
        return normalized;
    }

    private List<String> buildPackagePaths(SiteExportPreviewResponse.SiteExportOptions options) {
        List<String> paths = new ArrayList<>();
        paths.add("zrlog-export.json");
        paths.add("content/articles.jsonl");
        paths.add("content/types.json");
        paths.add("content/tags.json");
        if (options.isIncludeComments()) {
            paths.add("content/comments.jsonl");
        }
        paths.add("content/navs.json");
        paths.add("content/links.json");
        paths.add("site/website.json");
        paths.add("site/redactions.json");
        paths.add("media/index.jsonl");
        if (options.isIncludeMediaFiles()) {
            paths.add("media/files/...");
        }
        if (options.isIncludeThemeFiles()) {
            paths.add("themes/config.json");
        }
        paths.add("plugins/manifest.json");
        paths.add("ai/article-ai-messages.jsonl");
        paths.add("checksums/sha256.txt");
        return paths;
    }

    private List<String> buildNotes(SiteExportPreviewResponse.SiteExportOptions options) {
        List<String> notes = new ArrayList<>();
        notes.add("previewOnly");
        notes.add("sqlBackupIsNotSiteExport");
        if (options.isIncludeAiMessages()) {
            notes.add("aiMessagesIncludedExplicitly");
        } else {
            notes.add("aiMessagesExcludedByDefault");
        }
        notes.add("pluginDataRequiresExportCapability");
        return notes;
    }

    private SiteExportPreviewResponse.SiteExportCounts buildCounts() throws SQLException {
        SiteExportPreviewResponse.SiteExportCounts counts = new SiteExportPreviewResponse.SiteExportCounts();
        fillArticleCounts(counts);
        counts.setArticleVersions(countTable("log_version"));
        counts.setComments(countTable(COMMENT_TABLE));
        counts.setTypes(countTable("type"));
        counts.setTags(countTable("tag"));
        counts.setNavs(countTable("lognav"));
        counts.setLinks(countTable("link"));
        counts.setWebsiteKeys(countTable("website"));
        MediaStats mediaStats = countMediaFiles(attachedRoot());
        counts.setMediaFiles(mediaStats.files);
        counts.setMediaBytes(mediaStats.bytes);
        List<Map<String, Object>> aiMessages = listAiMessages();
        counts.setAiMessageArticles(aiMessages.size());
        counts.setAiMessageBytes(aiMessages.stream().mapToLong(row -> toLong(row.get("size"))).sum());
        return counts;
    }

    private void fillArticleCounts(SiteExportPreviewResponse.SiteExportCounts counts) throws SQLException {
        Map<String, Object> row = queryFirst(
                "SELECT "
                        + "count(1) AS totalCount,"
                        + "SUM(CASE WHEN l.rubbish = ? AND l.privacy = ? THEN 1 ELSE 0 END) AS publishedCount,"
                        + "SUM(CASE WHEN l.privacy = ? THEN 1 ELSE 0 END) AS privateCount,"
                        + "SUM(CASE WHEN l.rubbish = ? THEN 1 ELSE 0 END) AS draftCount "
                        + "FROM " + LOG_TABLE + " l "
                        + "inner join user u on u.userId = l.userId "
                        + "inner join type t on t.typeId = l.typeId "
                        + "where l.typeId is not null",
                false, false, true, true);
        counts.setArticles(toLong(row, "totalCount"));
        counts.setPublishedArticles(toLong(row, "publishedCount"));
        counts.setPrivateArticles(toLong(row, "privateCount"));
        counts.setDraftArticles(toLong(row, "draftCount"));
    }

    private List<SiteExportPreviewResponse.RedactionEntry> buildRedactions(
            SiteExportPreviewResponse.SiteExportCounts counts,
            SiteExportPreviewResponse.SiteExportOptions options) throws SQLException {
        List<SiteExportPreviewResponse.RedactionEntry> redactions = new ArrayList<>();
        for (Map<String, Object> row : queryList("select name from website order by name")) {
            String name = Objects.toString(row.get("name"), "");
            if (isSensitiveName(name)) {
                redactions.add(new SiteExportPreviewResponse.RedactionEntry("website", name, "sensitiveFieldName"));
            }
        }
        if (!options.isIncludeDrafts()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("content", "draft-articles", "excludedByOption"));
        }
        if (!options.isIncludePrivateArticles()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("content", "private-articles", "excludedByOption"));
        }
        if (!options.isIncludeComments()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("content", "comments", "excludedByOption"));
        }
        if (!options.isIncludeMediaFiles()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("media", "media/files", "excludedByOption"));
        }
        if (!options.isIncludeThemeFiles()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("themes", "config", "excludedByOption"));
        }
        if (!options.isIncludePluginConfigs()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("plugins", "config", "excludedByOption"));
        }
        if (!options.isIncludeAiMessages()) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("options", "includeAiMessages", "defaultFalse"));
            redactions.add(new SiteExportPreviewResponse.RedactionEntry(
                    "ai", "article-ai-messages", counts.getAiMessageArticles() > 0 ? "excludedByDefault" : "notPresent"));
        } else if (counts.getAiMessageArticles() == 0) {
            redactions.add(new SiteExportPreviewResponse.RedactionEntry("ai", "article-ai-messages", "notPresent"));
        }
        redactions.add(new SiteExportPreviewResponse.RedactionEntry(
                "plugins", "runtime-state", "defaultFalseWithoutPluginExportCapability"));
        return redactions;
    }

    private long countTable(String tableName) throws SQLException {
        return ((Number) queryFirstObj("select count(1) from " + tableName)).longValue();
    }

    private List<Map<String, Object>> loadArticles(SiteExportPreviewResponse.SiteExportOptions options) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where typeId is not null");
        if (!options.isIncludeDrafts()) {
            where.append(" and rubbish = ?");
            params.add(false);
        }
        if (!options.isIncludePrivateArticles()) {
            where.append(" and privacy = ?");
            params.add(false);
        }
        return queryList(
                "select logId as id,title,alias,digest,keywords,thumbnail,typeId,userId,releaseTime,"
                        + "last_update_date as lastUpdateDate,rubbish,privacy,canComment,markdown,content,"
                        + "plain_content,editor_type as editorType,version from " + LOG_TABLE
                        + where + " order by logId",
                params.toArray());
    }

    private List<Map<String, Object>> loadComments() throws SQLException {
        return queryList(
                "select commentId as id,userComment,commTime,userMail,userHome,userIp,userName,hide,logId "
                        + "from " + COMMENT_TABLE + " order by commentId");
    }

    private List<Map<String, Object>> loadTypes() throws SQLException {
        return queryList(
                "select t.typeId as id,t.alias,t.typeName,t.remark,t.arrange_plugin,"
                        + "(select count(logId) from " + LOG_TABLE
                        + " where rubbish=? and privacy=? and typeid=t.typeid) as typeamount from type t",
                false, false);
    }

    private List<Map<String, Object>> loadTags() throws SQLException {
        return queryList("select tagId as id,text,count from tag");
    }

    private List<Map<String, Object>> loadNavs() throws SQLException {
        return queryList(
                "select l.navId as id,l.navName,l.url,l.sort,l.icon from lognav l "
                        + "where l.url is not null and l.navName is not null order by sort");
    }

    private List<Map<String, Object>> loadLinks() throws SQLException {
        return queryList("select linkName,linkId as id,sort,url,alt,icon from link order by sort");
    }

    private List<Map<String, Object>> loadWebsite() throws SQLException {
        List<Map<String, Object>> rows = queryList("select name,value from website order by name");
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = Objects.toString(row.get("name"), "");
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            if (isSensitiveName(name)) {
                entry.put("redacted", true);
                entry.put("reason", "sensitiveFieldName");
            } else {
                entry.put("value", row.get("value"));
            }
            entries.add(entry);
        }
        return entries;
    }

    private Map<String, Object> loadThemeConfig() throws SQLException {
        Map<String, Object> data = new LinkedHashMap<>();
        String template = websiteValue("template");
        data.put("template", template);
        if (!template.isEmpty()) {
            data.put("config", websiteValue(template + "_setting"));
        }
        return data;
    }

    private Map<String, Object> loadPluginManifest(SiteExportPreviewResponse.SiteExportOptions options) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("exportCapabilityRequired", "data.export");
        manifest.put("included", false);
        manifest.put("includePluginConfigs", options.isIncludePluginConfigs());
        manifest.put("includePluginRuntimeState", options.isIncludePluginRuntimeState());
        manifest.put("reason", options.isIncludePluginConfigs()
                ? "pluginDataRequiresExportCapability"
                : "includePluginConfigsFalse");
        return manifest;
    }

    private boolean isSensitiveName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String token : SENSITIVE_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private File attachedRoot() {
        BlogRunTime blogRunTime = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.BLOG_RUN_TIME, BlogRunTime.class);
        String path = blogRunTime == null ? "" : Objects.toString(blogRunTime.getPath(), "");
        if (path.trim().isEmpty()) {
            return new File(ATTACHED_ROOT);
        }
        return new File(path, "attached");
    }

    private MediaStats countMediaFiles(File root) {
        MediaStats stats = new MediaStats();
        try {
            if (root == null || !root.exists()) {
                return stats;
            }
            File canonicalRoot = root.getCanonicalFile();
            collectMediaStats(canonicalRoot, canonicalRoot, stats);
        } catch (IOException ignored) {
            return stats;
        }
        return stats;
    }

    private List<File> collectMediaFiles(File root) throws IOException {
        List<File> files = new ArrayList<>();
        if (root == null || !root.exists()) {
            return files;
        }
        File canonicalRoot = root.getCanonicalFile();
        collectMediaFiles(canonicalRoot, canonicalRoot, files);
        return files;
    }

    private void collectMediaFiles(File file, File root, List<File> files) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        File canonicalFile = file.getCanonicalFile();
        if (!isInsideRoot(canonicalFile, root)) {
            return;
        }
        if (canonicalFile.isFile()) {
            files.add(canonicalFile);
            return;
        }
        File[] children = canonicalFile.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            collectMediaFiles(child, root, files);
        }
    }

    private List<Map<String, Object>> buildMediaIndex(List<File> mediaFiles) throws IOException {
        List<Map<String, Object>> entries = new ArrayList<>();
        File root = attachedRoot().getCanonicalFile();
        for (File mediaFile : mediaFiles) {
            String relativePath = toAttachedRelativePath(root, mediaFile);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("originalPath", ATTACHED_ROOT + "/" + relativePath);
            entry.put("exportPath", "media/files/attached/" + relativePath);
            entry.put("size", mediaFile.length());
            entry.put("mime", toMimeType(mediaFile.getName()));
            entry.put("lastModified", mediaFile.lastModified());
            entry.put("sha256", sha256(mediaFile));
            entries.add(entry);
        }
        return entries;
    }

    private void collectMediaStats(File file, File root, MediaStats stats) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        File canonicalFile = file.getCanonicalFile();
        if (!isInsideRoot(canonicalFile, root)) {
            return;
        }
        if (canonicalFile.isFile()) {
            stats.files += 1;
            stats.bytes += canonicalFile.length();
            return;
        }
        File[] children = canonicalFile.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            collectMediaStats(child, root, stats);
        }
    }

    private List<Map<String, Object>> listAiMessages() throws SQLException {
        return queryList(
                "SELECT `name`, `value`, length(`value`) AS `size` FROM `website` WHERE `name` LIKE ? ORDER BY `name`",
                AI_MESSAGE_PREFIX + "%");
    }

    private List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        return SiteExportDatabase.queryList(session, sql, params);
    }

    private Map<String, Object> queryFirst(String sql, Object... params) throws SQLException {
        return SiteExportDatabase.queryFirst(session, sql, params);
    }

    private Object queryFirstObj(String sql, Object... params) throws SQLException {
        return SiteExportDatabase.queryFirstObj(session, sql, params);
    }

    private String websiteValue(String name) throws SQLException {
        return SiteExportDatabase.queryString(session, "select value from website where name=?", name);
    }

    private long toLong(Map<String, Object> row, String key) {
        if (row == null) {
            return 0L;
        }
        Object value = row.get(key);
        if (value == null) {
            value = row.get(key.toLowerCase(Locale.ROOT));
        }
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return toLong(value);
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void writeJson(ZipOutputStream zipOutputStream, String path, Object data, List<String> checksums)
            throws IOException {
        writeBytes(zipOutputStream, path, (GSON.toJson(data) + "\n").getBytes(StandardCharsets.UTF_8), checksums);
    }

    private void writeJsonLines(ZipOutputStream zipOutputStream, String path, List<?> rows, List<String> checksums)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object row : rows) {
            sb.append(JSONL_GSON.toJson(row)).append("\n");
        }
        writeBytes(zipOutputStream, path, sb.toString().getBytes(StandardCharsets.UTF_8), checksums);
    }

    private void writeText(ZipOutputStream zipOutputStream, String path, String text, List<String> checksums)
            throws IOException {
        writeBytes(zipOutputStream, path, text.getBytes(StandardCharsets.UTF_8), checksums);
    }

    private void writeBytes(ZipOutputStream zipOutputStream, String path, byte[] bytes, List<String> checksums)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        zipOutputStream.write(bytes);
        zipOutputStream.closeEntry();
        if (checksums != null) {
            checksums.add(sha256(bytes) + "  " + path);
        }
    }

    private void writeMediaFiles(ZipOutputStream zipOutputStream, List<File> mediaFiles, List<String> checksums)
            throws IOException {
        File root = attachedRoot().getCanonicalFile();
        for (File mediaFile : mediaFiles) {
            String relativePath = toAttachedRelativePath(root, mediaFile);
            writeFile(zipOutputStream, "media/files/attached/" + relativePath, mediaFile, checksums);
        }
    }

    private void writeAiMessages(ZipOutputStream zipOutputStream, List<String> checksums, boolean includeMessages)
            throws IOException {
        List<Map<String, Object>> rows;
        try {
            rows = listAiMessages();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            if (includeMessages) {
                appendAiMessageRows(sb, row);
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", row.get("name"));
                entry.put("included", false);
                entry.put("size", row.get("size"));
                entry.put("reason", "includeAiMessagesDefaultFalse");
                sb.append(JSONL_GSON.toJson(entry)).append("\n");
            }
        }
        writeBytes(zipOutputStream, "ai/article-ai-messages.jsonl", sb.toString().getBytes(StandardCharsets.UTF_8), checksums);
    }

    private void appendAiMessageRows(StringBuilder sb, Map<String, Object> row) {
        String key = Objects.toString(row.get("name"), "");
        List<?> messages;
        try {
            messages = JSONL_GSON.fromJson(Objects.toString(row.get("value"), "[]"), List.class);
        } catch (RuntimeException e) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", key);
            entry.put("included", false);
            entry.put("size", row.get("size"));
            entry.put("reason", "invalidAiMessageJson");
            sb.append(JSONL_GSON.toJson(entry)).append("\n");
            return;
        }
        if (messages == null) {
            return;
        }
        long articleId = parseAiMessageArticleId(key);
        for (Object message : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", key);
            entry.put("articleId", articleId);
            entry.put("draft", articleId == 0);
            entry.put("message", message);
            sb.append(JSONL_GSON.toJson(entry)).append("\n");
        }
    }

    private long parseAiMessageArticleId(String key) {
        if (!key.startsWith(AI_MESSAGE_PREFIX)) {
            return -1;
        }
        try {
            return Long.parseLong(key.substring(AI_MESSAGE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void writeFile(ZipOutputStream zipOutputStream, String path, File file, List<String> checksums)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            inputStream.transferTo(zipOutputStream);
        }
        zipOutputStream.closeEntry();
        checksums.add(sha256(file) + "  " + path);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toHex(byte[] bytes) {
        StringJoiner joiner = new StringJoiner("");
        for (byte b : bytes) {
            joiner.add(String.format("%02x", b));
        }
        return joiner.toString();
    }

    private String toAttachedRelativePath(File root, File file) throws IOException {
        File canonicalFile = file.getCanonicalFile();
        if (!isInsideRoot(canonicalFile, root)) {
            throw new IOException("Media file is outside attached root: " + canonicalFile);
        }
        return root.toPath().relativize(canonicalFile.toPath()).toString().replace(File.separatorChar, '/');
    }

    private boolean isInsideRoot(File file, File root) {
        return file.toPath().startsWith(root.toPath());
    }

    private String toMimeType(String fileName) {
        String mime = URLConnection.guessContentTypeFromName(fileName);
        if (mime != null && !mime.trim().isEmpty()) {
            return mime;
        }
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".avif")) {
            return "image/avif";
        }
        if (lower.endsWith(".md")) {
            return "text/markdown";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    private static class MediaStats {
        private long files;
        private long bytes;
    }

    public static class SiteExportPackage {

        private final File file;
        private final String fileName;

        public SiteExportPackage(File file, String fileName) {
            this.file = file;
            this.fileName = fileName;
        }

        public File getFile() {
            return file;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
