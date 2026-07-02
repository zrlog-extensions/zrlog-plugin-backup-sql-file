package com.zrlog.plugin.backup.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.SiteExportPreviewResponse;
import com.zrlog.plugin.backup.model.SiteImportAiMessageRow;
import com.zrlog.plugin.backup.model.SiteImportArticleRow;
import com.zrlog.plugin.backup.model.SiteImportPrecheckResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SiteImportPrecheckService {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final String STATUS_OK = "ok";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SKIPPED = "skipped";
    private static final String MANIFEST_PATH = "zrlog-export.json";
    private static final String ARTICLES_PATH = "content/articles.jsonl";
    private static final String AI_MESSAGES_PATH = "ai/article-ai-messages.jsonl";

    private final IOSession session;

    public SiteImportPrecheckService(IOSession session) {
        this.session = session;
    }

    public SiteImportPrecheckResponse precheck(File packageFile) throws IOException, SQLException {
        SiteExportDatabase.ensureConfigured(session);
        SiteImportPrecheckResponse response = new SiteImportPrecheckResponse();
        response.setPackageName(packageFile.getName());
        try (ZipFile zipFile = new ZipFile(packageFile)) {
            response.setPackagePaths(listPaths(zipFile));
            SiteExportPreviewResponse manifest = readManifest(zipFile, response);
            if (manifest == null) {
                finish(response);
                return response;
            }
            fillManifest(response, manifest);
            checkSchema(response, manifest.getSchemaVersion());
            checkCoreEntries(zipFile, response);
            checkOptionalEntries(zipFile, response, manifest.getOptions());
            analyzeArticles(zipFile, response);
            analyzeAiMessages(zipFile, response, manifest.getOptions());
        }
        finish(response);
        return response;
    }

    private List<String> listPaths(ZipFile zipFile) {
        List<String> paths = new ArrayList<>();
        Collections.list(zipFile.entries()).stream()
                .filter(entry -> !entry.isDirectory())
                .map(ZipEntry::getName)
                .sorted()
                .forEach(paths::add);
        return paths;
    }

    private SiteExportPreviewResponse readManifest(ZipFile zipFile, SiteImportPrecheckResponse response)
            throws IOException {
        ZipEntry entry = zipFile.getEntry(MANIFEST_PATH);
        if (entry == null) {
            addCheck(response, "manifest", MANIFEST_PATH, STATUS_ERROR, "missingManifest");
            return null;
        }
        try {
            SiteExportPreviewResponse manifest = GSON.fromJson(readEntry(zipFile, entry), SiteExportPreviewResponse.class);
            if (manifest == null) {
                addCheck(response, "manifest", MANIFEST_PATH, STATUS_ERROR, "invalidManifest");
                return null;
            }
            addCheck(response, "manifest", MANIFEST_PATH, STATUS_OK, "manifestPresent");
            return manifest;
        } catch (JsonSyntaxException e) {
            addCheck(response, "manifest", MANIFEST_PATH, STATUS_ERROR, "invalidManifest");
            return null;
        }
    }

    private String readEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void fillManifest(SiteImportPrecheckResponse response, SiteExportPreviewResponse manifest) {
        response.setSchemaVersion(manifest.getSchemaVersion());
        response.setExportId(manifest.getExportId());
        response.setGeneratedAt(manifest.getGeneratedAt());
        response.setOptions(manifest.getOptions());
        response.setCounts(manifest.getCounts());
    }

    private void checkSchema(SiteImportPrecheckResponse response, int schemaVersion) {
        if (schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            addCheck(response, "manifest", "schemaVersion", STATUS_OK, "schemaSupported");
            return;
        }
        addCheck(response, "manifest", "schemaVersion", STATUS_ERROR, "unsupportedSchema");
    }

    private void checkCoreEntries(ZipFile zipFile, SiteImportPrecheckResponse response) {
        checkRequiredEntry(zipFile, response, "content", ARTICLES_PATH);
        checkRequiredEntry(zipFile, response, "content", "content/types.json");
        checkRequiredEntry(zipFile, response, "content", "content/tags.json");
        checkRequiredEntry(zipFile, response, "content", "content/navs.json");
        checkRequiredEntry(zipFile, response, "content", "content/links.json");
        checkRequiredEntry(zipFile, response, "site", "site/website.json");
        checkRequiredEntry(zipFile, response, "site", "site/redactions.json");
        checkRequiredEntry(zipFile, response, "media", "media/index.jsonl");
        checkRequiredEntry(zipFile, response, "plugins", "plugins/manifest.json");
        checkRequiredEntry(zipFile, response, "checksums", "checksums/sha256.txt");
    }

    private void checkOptionalEntries(ZipFile zipFile, SiteImportPrecheckResponse response,
                                      SiteExportPreviewResponse.SiteExportOptions options) {
        if (options == null) {
            return;
        }
        checkOptionalEntry(zipFile, response, "content", "content/comments.jsonl", options.isIncludeComments());
        checkOptionalEntry(zipFile, response, "themes", "themes/config.json", options.isIncludeThemeFiles());
    }

    private void checkRequiredEntry(ZipFile zipFile, SiteImportPrecheckResponse response, String scope, String path) {
        if (zipFile.getEntry(path) == null) {
            addCheck(response, scope, path, STATUS_ERROR, "missingRequiredEntry");
            return;
        }
        addCheck(response, scope, path, STATUS_OK, "requiredEntryPresent");
    }

    private void checkOptionalEntry(ZipFile zipFile, SiteImportPrecheckResponse response, String scope, String path,
                                    boolean expected) {
        if (!expected) {
            addCheck(response, scope, path, STATUS_SKIPPED, "optionalEntrySkipped");
            return;
        }
        if (zipFile.getEntry(path) == null) {
            addCheck(response, scope, path, STATUS_WARNING, "missingOptionalEntry");
            return;
        }
        addCheck(response, scope, path, STATUS_OK, "optionalEntryPresent");
    }

    private void analyzeArticles(ZipFile zipFile, SiteImportPrecheckResponse response)
            throws IOException, SQLException {
        ZipEntry entry = zipFile.getEntry(ARTICLES_PATH);
        if (entry == null) {
            return;
        }
        Set<String> existingAliases = loadExistingAliases();
        Set<String> conflictedAliases = new LinkedHashSet<>();
        long rows = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows++;
                String alias = readAlias(line);
                if (existingAliases.contains(alias)) {
                    conflictedAliases.add(alias);
                }
            }
        }
        response.setArticleRows(rows);
        response.setArticleAliasConflicts(conflictedAliases.size());
        addCheck(response, "content", "article-alias", conflictedAliases.isEmpty() ? STATUS_OK : STATUS_WARNING,
                conflictedAliases.isEmpty() ? "noArticleAliasConflicts" : "articleAliasConflicts");
    }

    private Set<String> loadExistingAliases() throws SQLException {
        Set<String> aliases = new HashSet<>();
        for (Map<String, Object> row : SiteExportDatabase.queryList(session,
                "select alias from log where alias is not null and alias <> ''")) {
            aliases.add(Objects.toString(row.get("alias"), ""));
        }
        return aliases;
    }

    private String readAlias(String jsonLine) {
        try {
            SiteImportArticleRow row = GSON.fromJson(jsonLine, SiteImportArticleRow.class);
            return Objects.toString(row == null ? null : row.getAlias(), "");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void analyzeAiMessages(ZipFile zipFile, SiteImportPrecheckResponse response,
                                   SiteExportPreviewResponse.SiteExportOptions options) throws IOException {
        ZipEntry entry = zipFile.getEntry(AI_MESSAGES_PATH);
        boolean includeAiMessages = options != null && options.isIncludeAiMessages();
        if (entry == null) {
            addCheck(response, "ai", AI_MESSAGES_PATH, includeAiMessages ? STATUS_ERROR : STATUS_SKIPPED,
                    includeAiMessages ? "aiMessagesMissing" : "aiMessagesExcluded");
            return;
        }
        long rows = 0;
        long included = 0;
        long excluded = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows++;
                try {
                    SiteImportAiMessageRow row = GSON.fromJson(line, SiteImportAiMessageRow.class);
                    if (row != null && row.getMessage() != null) {
                        included++;
                    } else {
                        excluded++;
                    }
                } catch (RuntimeException e) {
                    excluded++;
                }
            }
        }
        response.setAiMessageRows(rows);
        response.setAiMessageIncludedRows(included);
        response.setAiMessageExcludedRows(excluded);
        if (!includeAiMessages) {
            addCheck(response, "ai", AI_MESSAGES_PATH, STATUS_SKIPPED, "aiMessagesExcluded");
            return;
        }
        addCheck(response, "ai", AI_MESSAGES_PATH, STATUS_OK, "aiMessagesIncluded");
    }

    private void addCheck(SiteImportPrecheckResponse response, String scope, String key, String status,
                          String detail) {
        response.getChecks().add(new SiteImportPrecheckResponse.PrecheckEntry(scope, key, status, detail));
    }

    private void finish(SiteImportPrecheckResponse response) {
        response.setValidPackage(response.getChecks().stream()
                .noneMatch(check -> Objects.equals(check.getStatus(), STATUS_ERROR)));
    }
}
