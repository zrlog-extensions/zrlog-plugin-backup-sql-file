import {DownloadOutlined, FileZipOutlined, ReloadOutlined, UploadOutlined} from "@ant-design/icons";
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  List,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from "antd";
import type {ColumnsType} from "antd/es/table";
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import type {
  RedactionEntry,
  SiteExportOptions,
  SiteExportPreviewData,
  SiteImportPrecheckData,
  SiteImportPrecheckEntry,
  StandardResponse,
} from "./index";

type SiteExportPanelProps = {
  initialPreview?: SiteExportPreviewData;
  initialError?: string;
};

const defaultOptions: SiteExportOptions = {
  includeDrafts: true,
  includePrivateArticles: true,
  includeComments: true,
  includeMediaFiles: true,
  includeThemeFiles: true,
  includePluginConfigs: true,
  includePluginRuntimeState: false,
  includeAiMessages: false,
};

const defaultPreview: SiteExportPreviewData = {
  schemaVersion: 1,
  exportId: "",
  generatedAt: 0,
  packageName: "",
  options: defaultOptions,
  counts: {
    articles: 0,
    publishedArticles: 0,
    draftArticles: 0,
    privateArticles: 0,
    articleVersions: 0,
    comments: 0,
    types: 0,
    tags: 0,
    navs: 0,
    links: 0,
    websiteKeys: 0,
    mediaFiles: 0,
    mediaBytes: 0,
    aiMessageArticles: 0,
    aiMessageBytes: 0,
  },
  packagePaths: [],
  redactions: [],
  notes: [],
};

const optionKeys = [
  "includeDrafts",
  "includePrivateArticles",
  "includeComments",
  "includeMediaFiles",
  "includeThemeFiles",
  "includePluginConfigs",
  "includePluginRuntimeState",
  "includeAiMessages",
] as Array<keyof SiteExportOptions>;

const optionLabelMap: Record<keyof SiteExportOptions, string> = {
  includeDrafts: "包含草稿文章",
  includePrivateArticles: "包含私密文章",
  includeComments: "包含评论",
  includeMediaFiles: "包含媒体文件",
  includeThemeFiles: "包含主题配置",
  includePluginConfigs: "包含插件配置清单",
  includePluginRuntimeState: "包含插件运行状态",
  includeAiMessages: "包含文章 AI 对话",
};

const redactionScopeMap: Record<string, string> = {
  content: "内容",
  website: "站点配置",
  options: "导出选项",
  media: "媒体",
  themes: "主题",
  ai: "AI 数据",
  plugins: "插件",
};

const redactionReasonMap: Record<string, string> = {
  sensitiveFieldName: "敏感字段名",
  defaultFalse: "默认不包含",
  excludedByDefault: "默认排除",
  excludedByOption: "按选项排除",
  notPresent: "当前不存在",
  defaultFalseWithoutPluginExportCapability: "缺少插件导出接口，默认不包含",
};

const noteMap: Record<string, string> = {
  previewOnly: "预览只统计将导出的内容，不写入数据",
  sqlBackupIsNotSiteExport: "SQL 备份不是全站迁移包",
  aiMessagesExcludedByDefault: "文章 AI 对话默认不包含",
  aiMessagesIncludedExplicitly: "文章 AI 对话已按选项包含",
  pluginDataRequiresExportCapability: "插件数据需要插件提供独立导出接口",
};

const importCheckScopeMap: Record<string, string> = {
  manifest: "清单",
  content: "内容",
  site: "站点",
  media: "媒体",
  themes: "主题",
  plugins: "插件",
  ai: "AI 数据",
  checksums: "校验",
};

const importCheckStatusMap: Record<string, string> = {
  ok: "通过",
  warning: "警告",
  error: "错误",
  skipped: "跳过",
};

const importCheckDetailMap: Record<string, string> = {
  manifestPresent: "清单存在",
  missingManifest: "缺少清单",
  invalidManifest: "清单格式无效",
  schemaSupported: "结构版本支持",
  unsupportedSchema: "结构版本不支持",
  requiredEntryPresent: "必需文件存在",
  missingRequiredEntry: "缺少必需文件",
  optionalEntryPresent: "可选文件存在",
  optionalEntrySkipped: "可选文件已跳过",
  missingOptionalEntry: "缺少可选文件",
  noArticleAliasConflicts: "没有文章别名冲突",
  articleAliasConflicts: "存在文章别名冲突",
  aiMessagesIncluded: "AI 对话已包含",
  aiMessagesExcluded: "AI 对话已排除",
  aiMessagesMissing: "缺少 AI 对话文件",
};

const resolveRecordLabel = (records: Record<string, string>, key: string) => records[key] || key;

const normalizePreview = (preview?: SiteExportPreviewData): SiteExportPreviewData => ({
  ...defaultPreview,
  ...(preview || {}),
  options: {
    ...defaultOptions,
    ...(preview?.options || {}),
    includePluginRuntimeState: false,
  },
  counts: {
    ...defaultPreview.counts,
    ...(preview?.counts || {}),
  },
  packagePaths: preview?.packagePaths || [],
  redactions: preview?.redactions || [],
  notes: preview?.notes || [],
});

const SiteExportPanel: FunctionComponent<SiteExportPanelProps> = ({initialPreview, initialError}) => {
  const [preview, setPreview] = useState<SiteExportPreviewData>(() => normalizePreview(initialPreview));
  const [options, setOptions] = useState<SiteExportOptions>(() => normalizePreview(initialPreview).options);
  const [importPreview, setImportPreview] = useState<SiteImportPrecheckData>();
  const [refreshing, setRefreshing] = useState(false);
  const [prechecking, setPrechecking] = useState(false);
  const [messageApi, messageContextHolder] = message.useMessage({maxCount: 3});

  const disabledOptionKeys = useMemo(() => new Set<keyof SiteExportOptions>(["includePluginRuntimeState"]), []);

  const formatBytes = (bytes: number) => {
    if (!Number.isFinite(bytes) || bytes <= 0) {
      return "0 B";
    }
    const units = ["B", "KB", "MB", "GB"];
    let value = bytes;
    let index = 0;
    while (value >= 1024 && index < units.length - 1) {
      value /= 1024;
      index += 1;
    }
    return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
  };

  const buildOptionSearch = (targetOptions: SiteExportOptions) => {
    const params = new URLSearchParams();
    optionKeys.forEach((key) => {
      params.set(key, String(targetOptions[key]));
    });
    return params.toString();
  };

  const refreshPreview = async (targetOptions = options, showSuccess = true) => {
    setRefreshing(true);
    try {
      const {data: response} = await axios.get<StandardResponse<SiteExportPreviewData>>(
        `siteExportPreview?${buildOptionSearch(targetOptions)}`
      );
      if (!response.success) {
        messageApi.error(response.message || "预览刷新失败");
        return;
      }
      const nextPreview = normalizePreview(response.data);
      setPreview(nextPreview);
      setOptions(nextPreview.options);
      if (showSuccess) {
        messageApi.success("预览已刷新");
      }
    } catch (e) {
      messageApi.error(e instanceof Error ? e.message : "预览刷新失败");
    } finally {
      setRefreshing(false);
    }
  };

  const downloadPackage = () => {
    window.location.href = `siteExportDownload?${buildOptionSearch(options)}`;
  };

  const formatReportCell = (value: string | number | boolean | undefined) =>
    `${value ?? ""}`.replace(/\|/g, "\\|").replace(/\r?\n/g, " ");

  const buildReportFileName = (packageName?: string) => {
    const baseName = (packageName || "zrlog-site-export")
      .replace(/\.zip$/i, "")
      .replace(/[^a-zA-Z0-9._-]+/g, "-")
      .replace(/^-+|-+$/g, "");
    return `${baseName || "zrlog-site-export"}-import-precheck.md`;
  };

  const downloadTextFile = (fileName: string, content: string) => {
    const blob = new Blob([content], {type: "text/markdown;charset=utf-8"});
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const buildImportPrecheckReport = (precheck: SiteImportPrecheckData) => {
    const lines = [
      "# 全站导入预检报告",
      "",
      `- 包名: ${precheck.packageName || "-"}`,
      `- 是否有效: ${precheck.validPackage ? "有效" : "无效"}`,
      `- 结构版本: ${precheck.schemaVersion}`,
      `- 导出 ID: ${precheck.exportId || "-"}`,
      `- 生成时间: ${precheck.generatedAt ? new Date(precheck.generatedAt).toLocaleString() : "-"}`,
      `- 文章行数: ${precheck.articleRows}`,
      `- 文章别名冲突: ${precheck.articleAliasConflicts}`,
      `- AI 对话包含行数: ${precheck.aiMessageIncludedRows}`,
      `- AI 对话排除行数: ${precheck.aiMessageExcludedRows}`,
      "",
      "## 检查项",
      "",
      "| 范围 | 键 | 状态 | 详情 |",
      "| --- | --- | --- | --- |",
      ...precheck.checks.map(
        (entry) =>
          `| ${formatReportCell(resolveRecordLabel(importCheckScopeMap, entry.scope))} | ${formatReportCell(
            entry.key
          )} | ${formatReportCell(resolveRecordLabel(importCheckStatusMap, entry.status))} | ${formatReportCell(
            resolveRecordLabel(importCheckDetailMap, entry.detail)
          )} |`
      ),
      "",
    ];
    return lines.join("\n");
  };

  const downloadImportPrecheckReport = () => {
    if (!importPreview) {
      return;
    }
    downloadTextFile(buildReportFileName(importPreview.packageName), buildImportPrecheckReport(importPreview));
    messageApi.success("预检报告已下载");
  };

  const precheckImportPackage = async (source: string) => {
    setPrechecking(true);
    try {
      const {data: response} = await axios.post<StandardResponse<SiteImportPrecheckData>>(
        "siteImportPreview",
        new URLSearchParams({source}),
        {headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"}}
      );
      if (!response.success) {
        messageApi.error(response.message || "导入前预检失败");
        return;
      }
      setImportPreview(response.data);
      messageApi.success("导入前预检完成");
    } catch (e) {
      messageApi.error(e instanceof Error ? e.message : "导入前预检失败");
    } finally {
      setPrechecking(false);
    }
  };

  const updateOption = (key: keyof SiteExportOptions, checked: boolean) => {
    const nextOptions = {
      ...options,
      [key]: checked,
    };
    setOptions(nextOptions);
    void refreshPreview(nextOptions, false);
  };

  const redactionColumns: ColumnsType<RedactionEntry> = [
    {
      title: "范围",
      dataIndex: "scope",
      render: (value: string) => resolveRecordLabel(redactionScopeMap, value),
    },
    {
      title: "键",
      dataIndex: "key",
    },
    {
      title: "原因",
      dataIndex: "reason",
      render: (value: string) => resolveRecordLabel(redactionReasonMap, value),
    },
  ];

  const importCheckColumns: ColumnsType<SiteImportPrecheckEntry> = [
    {
      title: "范围",
      dataIndex: "scope",
      render: (value: string) => resolveRecordLabel(importCheckScopeMap, value),
    },
    {
      title: "键",
      dataIndex: "key",
    },
    {
      title: "状态",
      dataIndex: "status",
      render: (value: string) => (
        <Tag
          color={
            value === "ok"
              ? "success"
              : value === "warning"
              ? "warning"
              : value === "error"
              ? "error"
              : "default"
          }
        >
          {resolveRecordLabel(importCheckStatusMap, value)}
        </Tag>
      ),
    },
    {
      title: "详情",
      dataIndex: "detail",
      render: (value: string) => resolveRecordLabel(importCheckDetailMap, value),
    },
  ];

  const countItems = [
    {key: "articles", title: "文章", value: preview.counts.articles},
    {key: "comments", title: "评论", value: preview.counts.comments},
    {key: "mediaFiles", title: "媒体文件", value: preview.counts.mediaFiles},
    {key: "mediaBytes", title: "媒体大小", value: formatBytes(preview.counts.mediaBytes)},
    {key: "websiteKeys", title: "站点配置", value: preview.counts.websiteKeys},
    {key: "aiMessages", title: "AI 对话文章", value: preview.counts.aiMessageArticles},
    {key: "types", title: "分类", value: preview.counts.types},
    {key: "tags", title: "标签", value: preview.counts.tags},
    {key: "navs", title: "导航", value: preview.counts.navs},
    {key: "links", title: "链接", value: preview.counts.links},
  ];

  return (
    <>
      {messageContextHolder}
      <Space direction="vertical" size="middle" style={{width: "100%"}}>
        {initialError ? <Alert type="warning" showIcon message="全站导出预览加载失败" description={initialError}/> : null}
        <Card
          title={
            <Space>
              <FileZipOutlined/>
              <span>全站导出</span>
            </Space>
          }
          extra={
            <Space wrap>
              <Button icon={<ReloadOutlined/>} loading={refreshing} onClick={() => void refreshPreview()}>
                刷新预览
              </Button>
              <Button type="primary" icon={<DownloadOutlined/>} onClick={downloadPackage}>
                下载 zip
              </Button>
            </Space>
          }
        >
          <Typography.Paragraph type="secondary">
            生成可迁移的站点 zip 包，覆盖文章、分类、标签、评论、媒体、站点配置、主题配置和可审计的排除项
          </Typography.Paragraph>
          <Alert
            type="info"
            showIcon
            message="边界说明"
            description="SQL 备份用于数据库快照；全站导出用于站点迁移，默认不包含插件运行状态和文章 AI 对话"
          />
        </Card>

        <Card
          title={
            <Space>
              <UploadOutlined/>
              <span>导入前预检</span>
            </Space>
          }
        >
          <Space direction="vertical" size="middle" style={{width: "100%"}}>
            <Typography.Paragraph type="secondary">
              上传 ZrLog 全站导出 zip，只读取包结构和清单，不写入站点数据
            </Typography.Paragraph>
            <Space wrap>
              <Upload
                name="file"
                accept=".zip,application/zip"
                action="../upload?ext=zip"
                maxCount={1}
                disabled={prechecking}
                showUploadList={false}
                onChange={({file}) => {
                  if (file.status === "done") {
                    const response = file.response || {};
                    const source = response.url || response.data?.url || "";
                    if (!source) {
                      messageApi.error("上传响应缺少文件地址");
                      return;
                    }
                    void precheckImportPackage(source);
                  }
                  if (file.status === "error") {
                    messageApi.error("上传失败");
                  }
                }}
              >
                <Button icon={<UploadOutlined/>} loading={prechecking}>
                  选择 zip 预检
                </Button>
              </Upload>
              {importPreview ? (
                <Button icon={<DownloadOutlined/>} onClick={downloadImportPrecheckReport}>
                  下载预检报告
                </Button>
              ) : null}
            </Space>
            {importPreview ? (
              <Space direction="vertical" size="middle" style={{width: "100%"}}>
                <Descriptions column={{xs: 1, sm: 2, lg: 3}} size="small">
                  <Descriptions.Item label="是否有效">
                    <Tag color={importPreview.validPackage ? "success" : "error"}>
                      {importPreview.validPackage ? "有效" : "无效"}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="包名">{importPreview.packageName}</Descriptions.Item>
                  <Descriptions.Item label="结构版本">{importPreview.schemaVersion}</Descriptions.Item>
                  <Descriptions.Item label="导出 ID">{importPreview.exportId}</Descriptions.Item>
                  <Descriptions.Item label="文章行数">{importPreview.articleRows}</Descriptions.Item>
                  <Descriptions.Item label="文章别名冲突">{importPreview.articleAliasConflicts}</Descriptions.Item>
                  <Descriptions.Item label="AI 对话包含行数">
                    {importPreview.aiMessageIncludedRows}
                  </Descriptions.Item>
                  <Descriptions.Item label="AI 对话排除行数">
                    {importPreview.aiMessageExcludedRows}
                  </Descriptions.Item>
                </Descriptions>
                <Table
                  size="small"
                  rowKey={(entry) => `${entry.scope}-${entry.key}-${entry.detail}`}
                  columns={importCheckColumns}
                  dataSource={importPreview.checks}
                  pagination={false}
                  scroll={{x: 760}}
                />
              </Space>
            ) : null}
          </Space>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title="清单">
              <Descriptions column={1} size="small">
                <Descriptions.Item label="结构版本">{preview.schemaVersion}</Descriptions.Item>
                <Descriptions.Item label="导出 ID">{preview.exportId || "-"}</Descriptions.Item>
                <Descriptions.Item label="包名">{preview.packageName || "-"}</Descriptions.Item>
                <Descriptions.Item label="生成时间">
                  {preview.generatedAt ? new Date(preview.generatedAt).toLocaleString() : "-"}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="导出选项">
              <Space direction="vertical" size="small">
                {optionKeys.map((key) => (
                  <Checkbox
                    key={key}
                    checked={options[key]}
                    disabled={disabledOptionKeys.has(key)}
                    onChange={(event) => updateOption(key, event.target.checked)}
                  >
                    {optionLabelMap[key]}
                  </Checkbox>
                ))}
              </Space>
            </Card>
          </Col>
        </Row>

        <Card title="内容统计">
          <Row gutter={[16, 16]}>
            {countItems.map((item) => (
              <Col key={item.key} xs={12} sm={8} lg={6} xl={4}>
                <Statistic title={item.title} value={item.value}/>
              </Col>
            ))}
          </Row>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title="包内路径">
              <List size="small" dataSource={preview.packagePaths} renderItem={(item) => <List.Item>{item}</List.Item>}/>
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="排除和脱敏项">
              <Table
                size="small"
                rowKey={(entry) => `${entry.scope}-${entry.key}-${entry.reason}`}
                columns={redactionColumns}
                dataSource={preview.redactions}
                pagination={false}
                scroll={{x: 620}}
              />
            </Card>
          </Col>
        </Row>

        <Card title="说明">
          <List
            size="small"
            dataSource={preview.notes}
            renderItem={(item) => <List.Item>{resolveRecordLabel(noteMap, item)}</List.Item>}
          />
        </Card>
      </Space>
    </>
  );
};

export default SiteExportPanel;
