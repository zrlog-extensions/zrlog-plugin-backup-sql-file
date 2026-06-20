import React, { useMemo, useState } from "react";
import { 
  Alert,
  Table, 
  Button, 
  Tabs, 
  Modal, 
  Form, 
  Select, 
  Input, 
  Badge, 
  Card, 
  Space, 
  Tooltip,
  Typography,
  Flex,
  Row,
  Col,
  Grid,
  theme,
  App
} from "antd";
import { 
  DatabaseOutlined, 
  DownloadOutlined, 
  SettingOutlined, 
  ReloadOutlined, 
  HistoryOutlined, 
  FileZipOutlined, 
  CloudDownloadOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined
} from "@ant-design/icons";
import axios from "axios";
import {
  BackupInfoResponse,
  BackupNotificationChannelInfo,
  BackupNotificationChannels,
  FileRecord,
  NotificationProviderRow,
  StandardResponse
} from "./index";
import SiteExportPanel from "./SiteExportPanel";

const { Title, Text } = Typography;

interface AppBaseProps {
  data: BackupInfoResponse;
  setResponse: React.Dispatch<React.SetStateAction<StandardResponse<BackupInfoResponse> | null>>;
}

const defaultNotificationChannels = (): BackupNotificationChannels => ({
  successChannels: ["email"],
  failedChannels: ["email"],
});

const AppBase: React.FC<AppBaseProps> = ({ data, setResponse }) => {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const isPhone = Boolean(screens.xs && !screens.sm);
  const isCompact = !screens.lg;
  const { message } = App.useApp();

  const [loading, setLoading] = useState<boolean>(false);
  const [settingsVisible, setSettingsVisible] = useState<boolean>(false);
  const [settingsLoading, setSettingsLoading] = useState<boolean>(false);
  const [channelLoading, setChannelLoading] = useState<boolean>(false);
  const [notificationChannels, setNotificationChannels] = useState<BackupNotificationChannels>(
    data.notificationChannels || defaultNotificationChannels()
  );
  const [notificationProviders, setNotificationProviders] = useState<NotificationProviderRow[]>([]);

  const [form] = Form.useForm();

  const channelOptions = useMemo(() => {
    const rowsByChannel = new Map<string, NotificationProviderRow[]>();
    notificationProviders.forEach(row => {
      if (!row.channel) {
        return;
      }
      rowsByChannel.set(row.channel, [...(rowsByChannel.get(row.channel) || []), row]);
    });
    return Array.from(rowsByChannel.entries()).sort(([left], [right]) => left.localeCompare(right)).map(([channel, rows]) => {
      const provider = rows.find(row => row.selected) || rows.find(row => row.confirmed) || rows[0];
      const providerName = provider?.providerPluginName || provider?.capabilityLabel || "";
      return {
        label: providerName ? `${channel} (${providerName})` : channel,
        value: channel,
      };
    });
  }, [notificationProviders]);

  const availableChannelValues = useMemo(() => new Set(channelOptions.map(option => option.value)), [channelOptions]);

  const filterAvailableChannels = (channels?: string[]) => (channels || []).filter(channel => availableChannelValues.has(channel));

  const scheduleCron = data.schedule?.cron || data.config.backupCron;
  const scheduleAvailable = data.schedule?.success !== false && Boolean(scheduleCron);

  const openSchedulerCenter = () => {
    window.location.href = "../runtime-scheduler";
  };

  // Reload page data
  const refreshPage = async (silent = false) => {
    setLoading(true);
    try {
      const { data: res } = await axios.get<StandardResponse<BackupInfoResponse>>("json");
      if (res.success) {
        setResponse(res);
        setNotificationChannels(res.data.notificationChannels || defaultNotificationChannels());
        if (!silent) {
          message.success("刷新成功");
        }
      } else {
        message.error(res.message || "刷新失败");
      }
    } catch (e) {
      console.error(e);
      message.error("刷新失败");
    } finally {
      setLoading(false);
    }
  };

  const loadNotificationChannels = async () => {
    setChannelLoading(true);
    try {
      const { data: res } = await axios.get<StandardResponse<BackupNotificationChannelInfo>>("notificationChannels");
      if (!res.success) {
        throw new Error(res.message || "通知渠道加载失败");
      }
      const info = res.data;
      setNotificationChannels(info.settings || defaultNotificationChannels());
      setNotificationProviders(info.providers || []);
      const values = new Set((info.providers || []).map(row => row.channel).filter(Boolean));
      const successChannels = (info.settings?.successChannels || []).filter(channel => values.has(channel));
      const failedChannels = (info.settings?.failedChannels || []).filter(channel => values.has(channel));
      form.setFieldsValue({
        successChannels,
        failedChannels: failedChannels.length > 0 ? failedChannels : successChannels,
      });
    } catch (e) {
      console.error(e);
      message.error(e instanceof Error ? e.message : "通知渠道加载失败");
    } finally {
      setChannelLoading(false);
    }
  };

  // Handle saving backup configuration
  const handleSaveSettings = async () => {
    try {
      const values = await form.validateFields();
      setSettingsLoading(true);

      const params = new URLSearchParams();
      params.append("backupPassword", values.backupPassword || "");
      params.append("backupFilePath", values.backupFilePath || "");

      const { data: res } = await axios.post<StandardResponse<any>>("update", params, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" }
      });

      if (res && res.success) {
        const successChannels = filterAvailableChannels(values.successChannels);
        const failedChannels = filterAvailableChannels(values.failedChannels || values.successChannels);
        if (successChannels.length === 0) {
          throw new Error("请选择可用的通知渠道");
        }
        const channelParams = new URLSearchParams();
        channelParams.append("successChannels", successChannels.join(","));
        channelParams.append("failedChannels", (failedChannels.length > 0 ? failedChannels : successChannels).join(","));
        const { data: channelRes } = await axios.post<StandardResponse<BackupNotificationChannelInfo>>(
          "saveNotificationChannels",
          channelParams,
          { headers: { "Content-Type": "application/x-www-form-urlencoded" } }
        );
        if (!channelRes || !channelRes.success) {
          throw new Error(channelRes?.message || "通知配置保存失败");
        }
        setNotificationChannels(channelRes.data.settings || defaultNotificationChannels());
        setNotificationProviders(channelRes.data.providers || notificationProviders);
        message.success("配置保存成功");
        setSettingsVisible(false);
        // Refresh page logs
        setTimeout(() => refreshPage(true), 800);
      } else {
        message.error(res.message || "配置保存失败");
      }
    } catch (e) {
      console.error(e);
      if (e instanceof Error) {
        message.error(e.message);
      }
    } finally {
      setSettingsLoading(false);
    }
  };

  // Trigger manual backup
  const handleBackupNow = async () => {
    setLoading(true);
    const hideLoading = message.loading("正在执行手动备份，请稍候...", 0);
    try {
      const { data: res } = await axios.post<StandardResponse<any>>("backupNow");
      hideLoading();
      if (res && res.success) {
        message.success("手动备份完成，文件已生成！");
        refreshPage(true);
      } else {
        message.error(res.message || "手动备份执行失败");
      }
    } catch (e) {
      hideLoading();
      console.error(e);
      message.error("手动备份请求异常");
    } finally {
      setLoading(false);
    }
  };

  // Export immediate SQL dump
  const handleExportSql = () => {
    window.location.href = "exportSqlFile";
  };

  // Download a saved SQL file
  const downloadBackupFile = (fileName: string) => {
    window.location.href = `downfile?file=${encodeURIComponent(fileName)}`;
  };

  // Backup files list columns
  const fileColumns = [
    {
      title: "#",
      dataIndex: "index",
      key: "index",
      width: 64,
      align: "center" as const,
    },
    {
      title: "备份文件名",
      dataIndex: "fileName",
      key: "fileName",
      render: (text: string) => (
        <Button 
          type="link" 
          icon={<DownloadOutlined />} 
          style={{ padding: 0, height: "auto" }}
          onClick={() => downloadBackupFile(text)}
        >
          {text}
        </Button>
      )
    },
    {
      title: "创建时间",
      dataIndex: "lastModified",
      key: "lastModified",
      width: 180,
    },
    {
      title: "文件大小",
      dataIndex: "size",
      key: "size",
      width: 120,
    },
    {
      title: "操作",
      key: "action",
      width: 84,
      align: "center" as const,
      render: (_: any, record: FileRecord) => (
        <Tooltip title="下载备份">
          <Button 
            type="text" 
            shape="circle" 
            icon={<DownloadOutlined />} 
            onClick={() => downloadBackupFile(record.fileName)}
          />
        </Tooltip>
      )
    }
  ];

  // Database action logs columns
  const historyColumns = [
    {
      title: "操作时间",
      dataIndex: "time",
      key: "time",
      width: 180,
    },
    {
      title: "状态",
      dataIndex: "success",
      key: "success",
      width: 100,
      render: (success: boolean) => success ? (
        <Badge status="success" text={<Text type="success">成功</Text>} />
      ) : (
        <Badge status="error" text={<Text type="danger">失败</Text>} />
      )
    },
    {
      title: "归档文件数",
      dataIndex: "filesCount",
      key: "filesCount",
      width: 100,
      align: "center" as const,
    },
    {
      title: "详情说明",
      dataIndex: "message",
      key: "message",
      render: (text: string) => <Text ellipsis={{ tooltip: text }}>{text}</Text>
    }
  ];

  const backupCronOptions = [
    { value: "*/1 * * * *", label: "每分钟（测试）" },
    { value: "*/5 * * * *", label: "每 5 分钟（测试）" },
    { value: "0 * * * *", label: "每小时" },
    { value: "0 */6 * * *", label: "每 6 小时" },
    { value: "0 */12 * * *", label: "每 12 小时" },
    { value: "0 2 * * *", label: "每天 02:00" },
  ];

  const getCronLabel = (value: string) => {
    const option = backupCronOptions.find(item => item.value === value);
    return option ? option.label : value;
  };

  return (
    <div
      style={{
        width: "100%",
        maxWidth: 1240,
        margin: "0 auto",
        padding: isPhone ? 12 : isCompact ? 16 : 20,
        boxSizing: "border-box",
      }}
    >
      {/* Title & Actions Bar */}
      <Flex
        justify="space-between"
        align="flex-start"
        gap={16}
        vertical={isCompact}
        style={{ marginBottom: 18 }}
      >
        <div>
          <Flex align="center" gap={8}>
            <DatabaseOutlined style={{ fontSize: 24, color: token.colorPrimary }} />
            <Title level={2} style={{ margin: 0, fontSize: isPhone ? 20 : 24, lineHeight: "32px", fontWeight: 650 }}>
              数据库备份设置
            </Title>
          </Flex>
          <Text type="secondary" style={{ marginTop: 6, display: "block", fontSize: 14 }}>
            备份 MySQL 数据库，支持手动执行、定时调度和本地保留。
          </Text>
        </div>
        <Space wrap style={{ width: isPhone ? "100%" : undefined }}>
          <Button icon={<ReloadOutlined/>} onClick={() => refreshPage(false)} loading={loading} style={isPhone ? {flex: 1} : undefined}>刷新</Button>
          <Button icon={<SettingOutlined/>} onClick={() => {
            const channels = notificationChannels || defaultNotificationChannels();
            form.setFieldsValue({
              backupPassword: data.config.backupPassword,
              backupFilePath: data.config.backupFilePath,
              successChannels: channels.successChannels || ["email"],
              failedChannels: channels.failedChannels || channels.successChannels || ["email"],
            });
            setSettingsVisible(true);
            loadNotificationChannels();
          }} style={isPhone ? {flex: 1} : undefined}>备份设置</Button>
          <Button type="dashed" icon={<CloudDownloadOutlined/>} onClick={handleBackupNow} loading={loading} style={isPhone ? {flex: 1} : undefined}>
            立即备份
          </Button>
          <Button type="primary" icon={<DownloadOutlined/>} onClick={handleExportSql} style={isPhone ? {flex: 1} : undefined}>
            导出SQL文件
          </Button>
        </Space>
      </Flex>

      <Tabs
        defaultActiveKey="database"
        items={[
          {
            key: "database",
            label: (
              <span>
                <DatabaseOutlined />
                数据库备份
              </span>
            ),
            children: (
              <>
                {/* Metrics Cards */}
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} md={6}>
          <Card
            bordered
            style={{
              borderColor: token.colorBorderSecondary,
              borderRadius: token.borderRadiusLG,
              backgroundColor: token.colorBgContainer,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>调度周期</Typography.Text>
            <div style={{ fontSize: 18, fontWeight: 600, color: token.colorPrimary, marginTop: 4 }}>
              {scheduleAvailable ? getCronLabel(scheduleCron) : "调度信息不可用"}
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            bordered
            style={{
              borderColor: token.colorBorderSecondary,
              borderRadius: token.borderRadiusLG,
              backgroundColor: token.colorBgContainer,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>已归档文件数</Typography.Text>
            <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>
              {data.files.length} 个 SQL 文件
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            bordered
            style={{
              borderColor: token.colorBorderSecondary,
              borderRadius: token.borderRadiusLG,
              backgroundColor: token.colorBgContainer,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>密码防护状态</Typography.Text>
            <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>
              {data.config.backupPassword ? (
                <Text type="success"><CheckCircleOutlined /> 强密码加密</Text>
              ) : (
                <Text type="warning"><InfoCircleOutlined /> 未启用加密</Text>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card
            bordered
            style={{
              borderColor: token.colorBorderSecondary,
              borderRadius: token.borderRadiusLG,
              backgroundColor: token.colorBgContainer,
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>最大留存份数</Typography.Text>
            <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>
              最近 {data.maxKeepSize} 份
            </div>
          </Card>
        </Col>
      </Row>

      {/* Main Tab Panel */}
      <Card
        bordered
        style={{
          borderColor: token.colorBorderSecondary,
          borderRadius: token.borderRadiusLG,
          backgroundColor: token.colorBgContainer,
        }}
        styles={{ body: { padding: 16 } }}
      >
        <Tabs defaultActiveKey="1" items={[
          {
            key: "1",
            label: (
              <span>
                <FileZipOutlined />
                已存归档文件
              </span>
            ),
            children: (
              <Table 
                rowKey="fileName"
                size={isPhone ? "small" : "middle"}
                columns={fileColumns} 
                dataSource={data.files} 
                loading={loading}
                scroll={{ x: 800 }}
                pagination={{ pageSize: 10, showSizeChanger: true }}
                locale={{ emptyText: "当前暂无定时备份文件，可点击右上角“立即备份”执行第一次归档" }}
              />
            )
          },
          {
            key: "2",
            label: (
              <span>
                <HistoryOutlined />
                执行历史记录
              </span>
            ),
            children: (
              <Table 
                rowKey="time"
                size={isPhone ? "small" : "middle"}
                columns={historyColumns} 
                dataSource={data.history} 
                loading={loading}
                scroll={{ x: 800 }}
                pagination={{ pageSize: 10 }}
                locale={{ emptyText: "暂无备份执行日志，定时调度触发后将自动添加日志记录" }}
              />
            )
          }
        ]} />
      </Card>
              </>
            ),
          },
          {
            key: "siteExport",
            label: (
              <span>
                <FileZipOutlined />
                全站导出
              </span>
            ),
            children: (
              <SiteExportPanel initialPreview={data.siteExport} initialError={data.siteExportError} />
            ),
          },
        ]}
      />

      {/* Parameter Settings Modal */}
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center", gap: "8px", borderBottom: `1px solid ${token.colorBorderSecondary}`, paddingBottom: "12px", width: "100%" }}>
            <SettingOutlined style={{ color: token.colorPrimary }} />
            <span>配置备份参数</span>
          </div>
        }
        open={settingsVisible}
        onOk={handleSaveSettings}
        confirmLoading={settingsLoading}
        onCancel={() => setSettingsVisible(false)}
        okText="保存配置"
        cancelText="取消"
        width={isPhone ? "calc(100vw - 24px)" : 560}
        destroyOnClose
        style={{ top: isPhone ? 12 : "8vh" }}
      >
        <Form
          form={form}
          layout="vertical"
          style={{ paddingTop: 16 }}
        >
          {/* Scroll wrapper to prevent modal content overflow */}
          <div style={{ maxHeight: "60vh", overflowY: "auto", paddingRight: 8, overflowX: "hidden" }}>
            <Alert
              type="info"
              showIcon
              message="自动备份周期在调度中心配置"
              description="当前页面只保存备份文件参数和通知渠道；周期、启停和下次执行时间在调度中心配置。"
              action={<Button size="small" onClick={openSchedulerCenter}>配置调度周期</Button>}
              style={{ marginBottom: 16 }}
            />

            <Form.Item
              name="backupPassword"
              label="备份文件加密密码"
              tooltip="输入后，生成的 .sql 文件会经过 AES 对称加密，留空则保存为明文 SQL 文件"
            >
              <Input.Password placeholder="留空则不进行文件对称加密" />
            </Form.Item>

            <Form.Item
              name="backupFilePath"
              label="存储绝对物理路径"
              tooltip="配置备份文件存储的本地宿主机绝对目录。如留空，将自动写入插件根目录下的 /sql/ 文件夹下"
            >
              <Input placeholder="输入物理文件绝对路径（确保具备读写运行权限）" />
            </Form.Item>

            <Form.Item
              name="successChannels"
              label="备份成功通知渠道"
              rules={[{ required: true, message: "请选择通知渠道" }]}
            >
              <Select
                mode="multiple"
                loading={channelLoading}
                options={channelOptions}
                placeholder="选择通知渠道"
                notFoundContent={channelLoading ? "加载中" : "暂无可用渠道"}
              />
            </Form.Item>

            <Form.Item
              name="failedChannels"
              label="备份失败通知渠道"
            >
              <Select
                mode="multiple"
                loading={channelLoading}
                options={channelOptions}
                placeholder="默认使用成功通知渠道"
                notFoundContent={channelLoading ? "加载中" : "暂无可用渠道"}
              />
            </Form.Item>

            {notificationProviders.length === 0 && !channelLoading && (
              <Alert
                type="warning"
                showIcon
                message="当前没有可用通知渠道"
                style={{ marginBottom: 16 }}
              />
            )}
          </div>
        </Form>
      </Modal>
    </div>
  );
};

export default AppBase;
