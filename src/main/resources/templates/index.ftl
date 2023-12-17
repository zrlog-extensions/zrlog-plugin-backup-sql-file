<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>

    <link rel="stylesheet" href="assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="assets/css/jquery.gritter.css"/>

    <script src="assets/js/jquery-2.0.3.min.js"></script>
    <script src="assets/js/jquery.gritter.min.js"></script>
    <script src="js/set_update.js"></script>
    <style>
        body {
            margin: 0;
            font-size: 16px;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
            font-variant: tabular-nums;
            line-height: 1.5;
            font-feature-settings: 'tnum';
            background: transparent;
        }

        .dark {
            background-color: #000;
            color: #fff
        }
    </style>
</head>
<body class="${theme}">
<div style="max-width: 960px;width: 100%">
    <h3 style="padding-top: 20px">${_plugin.desc}</h3>
    <div style="display: flex;justify-content: end;gap: 16px">
        <a href="files?spm=index">
            <button class="btn btn-link" type="button">
                查看备份文件
            </button>
        </a>
        <a href="exportSqlFile">
            <button class="btn btn-info" type="button">
                导出SQL文件
            </button>
        </a>
    </div>
    <hr/>
    <form id="ajaxBackupSql" class="form-horizontal" role="form">
        <div style="display: flex;align-items: center;gap: 24px">
            <label class="" style="text-align:end; width: 80px;margin-bottom: 0"> 备份周期 </label>
            <select class="form-control" name="cycle" style="max-width: 180px">
                <option value="3600"
                        <#if '3600'==cycle>selected="selected"</#if>
                >1小时
                </option>
                <option value="21600"
                        <#if '21600'==cycle>selected="selected"</#if>
                >6小时
                </option>
                <option value="43200"
                        <#if '43200'==cycle>selected="selected"</#if>
                >12小时
                </option>
                <option value="86400"
                        <#if '86400'==cycle>selected="selected"</#if>
                >1天
                </option>
            </select>
        </div>
        <div style="display: flex;align-items: center;gap: 24px;padding-top: 24px">
            <label class="" style="text-align:end; width: 80px;margin-bottom: 0"> 备份密码 </label>
            <input class="form-control" name="backupPassword" type="password" value="${backupPassword!''}"
                   style="max-width: 240px"/>
        </div>
        <div style="display: flex;align-items: center;gap: 24px;padding-top: 24px">
            <label class="" style="text-align:end; width: 80px;margin-bottom: 0"> 存储路径 </label>
            <input class="form-control" name="backupFilePath" type="text" value="${backupFilePath!''}"
                   style="max-width: 480px"/>
        </div>
        <hr/>
        <button class="btn btn-primary" type="button" id="BackupSql" style="margin-left: 104px">
            提交
        </button>
    </form>
    <input id="gritter-light" checked="" type="checkbox" style="display:none"/>
</div>
</body>
</html>