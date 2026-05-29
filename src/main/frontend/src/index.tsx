import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import axios from "axios";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";

const {darkAlgorithm, defaultAlgorithm} = theme;

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
                <App>
                    <AppBase data={response.data} setResponse={setResponse}/>
                </App>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
