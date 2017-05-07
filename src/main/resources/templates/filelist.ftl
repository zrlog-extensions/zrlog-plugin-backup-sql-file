<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version} - 记录查看</title>
    <link rel="stylesheet" href="assets/css/bootstrap.min.css"/>
</head>
<body>
<div class="main-container">
    <div class="col-xs-8">
        <h3>查看文件列表 (仅展示最近20条记录)</h3>
        <hr/>
        <#list files as file>
            <div class="rows">
                <div class="col-xs-5">
                    <span><a href="downfile?file=${file.fileName}">${file.fileName}</a></span>
                </div>
                <div class="col-xs-7">
                    ${file.lastModified}
                </div>
            </div>
        </#list>
        <hr/>
    </div>
</div>
</body>
</html>