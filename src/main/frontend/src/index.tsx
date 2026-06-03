import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, Layout, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import axios from "axios";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";

const {darkAlgorithm, defaultAlgorithm} = theme;
const {Content} = Layout;

export interface Plugin {
    id: string;
    version: string;
    name: string;
    desc: string;
    author: string;
}

export interface BackupConfig {
    cycle: string;
    backupCron: string;
    backupPassword?: string;
    backupFilePath?: string;
}

export interface BackupNotificationChannels {
    successChannels: string[];
    failedChannels: string[];
}

export interface NotificationProviderRow {
    channel: string;
    providerPluginId: string;
    providerPluginName?: string;
    providerPluginPreviewImageBase64?: string;
    capabilityKey: string;
    capabilityLabel?: string;
    providerStatus: string;
    selected: boolean;
    confirmed: boolean;
    reviewRequired: boolean;
}

export interface BackupNotificationChannelInfo {
    settings: BackupNotificationChannels;
    providers: NotificationProviderRow[];
}

export interface BackupSchedule {
    success: boolean;
    errorMessage?: string;
    automationId?: string;
    capabilityKey?: string;
    name?: string;
    cron?: string;
    timezone?: string;
    enabled?: boolean;
    nextRunAt?: string;
    lastRunAt?: string;
}

export interface FileRecord {
    fileName: string;
    index: number;
    size: string;
    lastModified: string;
}

export interface HistoryRecord {
    time: string;
    success: boolean;
    filesCount: number;
    message: string;
}

export interface BackupInfoResponse {
    dark: boolean;
    colorPrimary: string;
    plugin: Plugin;
    config: BackupConfig;
    files: FileRecord[];
    history: HistoryRecord[];
    maxKeepSize: number;
    schedulerTimezone: string;
    schedule: BackupSchedule;
    notificationChannels: BackupNotificationChannels;
}

export interface StandardResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

const loadFromDocument = () => {
    try {
        const node = document.getElementById("pluginInfo");
        if (node === null || node.innerText.length === 0) {
            return null;
        }
        const text = node.innerText.trim();
        if (text.startsWith("${") && text.endsWith("}")) {
            return null;
        }
        return JSON.parse(text) as StandardResponse<BackupInfoResponse>;
    } catch (e) {
        return null;
    }
};

const Index = () => {
    const [response, setResponse] = useState<StandardResponse<BackupInfoResponse> | null>(loadFromDocument);

    useEffect(() => {
        if (response === null) {
            axios.get<StandardResponse<BackupInfoResponse>>("json").then(({data}) => {
                setResponse(data);
            });
        }
    }, [response]);

    if (response === null || !response.success) {
        return <></>;
    }

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: response.data.dark ? darkAlgorithm : defaultAlgorithm,
                token: {
                    colorPrimary: response.data.colorPrimary || "#1677ff",
                },
            }}
        >
            <StyleProvider transformers={[legacyLogicalPropertiesTransformer]}>
                <Content>
                    <App>
                        <AppBase data={response.data} setResponse={setResponse}/>
                    </App>
                </Content>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
