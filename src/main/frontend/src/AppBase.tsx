import React, { useState } from "react";
import { 
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
import { BackupInfoResponse, FileRecord, StandardResponse } from "./index";

const { Title, Text } = Typography;

interface AppBaseProps {
  data: BackupInfoResponse;
  setResponse: React.Dispatch<React.SetStateAction<StandardResponse<BackupInfoResponse> | null>>;
}

const AppBase: React.FC<AppBaseProps> = ({ data, setResponse }) => {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const { message } = App.useApp();

  const [loading, setLoading] = useState<boolean>(false);
  const [settingsVisible, setSettingsVisible] = useState<boolean>(false);
  const [settingsLoading, setSettingsLoading] = useState<boolean>(false);

  const [form] = Form.useForm();

  // Reload page data
  const refreshPage = async (silent = false) => {
    setLoading(true);
    try {
      const { data: res } = await axios.get<StandardResponse<BackupInfoResponse>>("json");
      if (res.success) {
        setResponse(res);
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

  // Handle saving backup configuration
  const handleSaveSettings = async () => {
    try {
      const values = await form.validateFields();
      setSettingsLoading(true);

      const params = new URLSearchParams();
      params.append("cycle", values.cycle);
      params.append("backupPassword", values.backupPassword || "");
      params.append("backupFilePath", values.backupFilePath || "");

      const { data: res } = await axios.post<StandardResponse<any>>("update", params, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" }
      });

      if (res && res.success) {
        message.success("配置保存成功");
        setSettingsVisible(false);
        // Refresh page logs
        setTimeout(() => refreshPage(true), 800);
      } else {
        message.error(res.message || "配置保存失败");
      }
    } catch (e) {
      console.error(e);
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

  const getCycleLabel = (value: string) => {
    switch (value) {
      case "60": return "1 分钟 (测试用)";
      case "3600": return "1 小时";
      case "21600": return "6 小时";
      case "43200": return "12 小时";
      case "86400": return "1 天 (标准周期)";
      default: return `${parseInt(value) / 3600} 小时`;
    }
  };

  return (
    <div
      style={{
        width: "100%",
        maxWidth: 1240,
        margin: "0 auto",
        padding: screens.xs ? 14 : 20,
        boxSizing: "border-box",
      }}
    >
      {/* Title & Actions Bar */}
      <Flex
        justify="space-between"
        align="flex-start"
        gap={16}
        vertical={screens.xs}
        style={{ marginBottom: 18 }}
      >
        <div>
          <Flex align="center" gap={8}>
            <DatabaseOutlined style={{ fontSize: 24, color: token.colorPrimary }} />
            <Title level={2} style={{ margin: 0, fontSize: 24, lineHeight: "32px", fontWeight: 650 }}>
              数据库备份设置
            </Title>
          </Flex>
          <Text type="secondary" style={{ marginTop: 6, display: "block", fontSize: 14 }}>
            提供 MySQL 数据库的全自动定时归档归一化备份。
          </Text>
        </div>
        <Space wrap style={{ marginTop: screens.xs ? 12 : 0 }}>
          <Button icon={<ReloadOutlined/>} onClick={() => refreshPage(false)} loading={loading}>刷新</Button>
          <Button icon={<SettingOutlined/>} onClick={() => {
            form.setFieldsValue({
              cycle: data.config.cycle,
              backupPassword: data.config.backupPassword,
              backupFilePath: data.config.backupFilePath
            });
            setSettingsVisible(true);
          }}>配置策略</Button>
          <Button type="dashed" icon={<CloudDownloadOutlined/>} onClick={handleBackupNow} loading={loading}>
            立即备份
          </Button>
          <Button type="primary" icon={<DownloadOutlined/>} onClick={handleExportSql}>
            导出SQL文件
          </Button>
        </Space>
      </Flex>

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
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>定时周期</Typography.Text>
            <div style={{ fontSize: 20, fontWeight: 600, color: token.colorPrimary, marginTop: 4 }}>
              {getCycleLabel(data.config.cycle)}
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
                size="middle"
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
                size="middle"
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

      {/* Parameter Settings Modal */}
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center", gap: "8px", borderBottom: `1px solid ${token.colorBorderSecondary}`, paddingBottom: "12px", width: "100%" }}>
            <SettingOutlined style={{ color: token.colorPrimary }} />
            <span>配置自动备份参数</span>
          </div>
        }
        open={settingsVisible}
        onOk={handleSaveSettings}
        confirmLoading={settingsLoading}
        onCancel={() => setSettingsVisible(false)}
        okText="保存配置"
        cancelText="取消"
        width={560}
        destroyOnClose
        style={{ top: "8vh" }}
      >
        <Form
          form={form}
          layout="vertical"
          style={{ paddingTop: 16 }}
        >
          {/* Scroll wrapper to prevent modal content overflow */}
          <div style={{ maxHeight: "60vh", overflowY: "auto", paddingRight: 8, overflowX: "hidden" }}>
            <Form.Item
              name="cycle"
              label="备份自动执行周期"
              tooltip="设置定时自动归档备份的任务时间定时间隔"
              rules={[{ required: true, message: "请选择备份间隔时间" }]}
            >
              <Select>
                <Select.Option value="60">1分钟 (极不推荐，仅限测试使用)</Select.Option>
                <Select.Option value="3600">1小时</Select.Option>
                <Select.Option value="21600">6小时</Select.Option>
                <Select.Option value="43200">12小时</Select.Option>
                <Select.Option value="86400">1天 (系统推荐)</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="backupPassword"
              label="归档压缩包加密密码"
              tooltip="输入后，打包出来的 .sql 压缩包会经过 AES 对称加密，防备服务器目录文件泄露泄密"
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
          </div>
        </Form>
      </Modal>
    </div>
  );
};

export default AppBase;
