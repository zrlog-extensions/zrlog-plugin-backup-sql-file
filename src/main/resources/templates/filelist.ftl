<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version} - 记录查看</title>

    <link rel="stylesheet" href="assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="assets/css/font-awesome.min.css"/>
    <link rel="stylesheet" href="assets/css/ace.min.css"/>
    <link rel="stylesheet" href="assets/css/ace-rtl.min.css"/>
    <link rel="stylesheet" href="assets/css/ace-skins.min.css"/>
    <link rel="stylesheet" href="assets/css/jquery.gritter.css"/>

    <script src="assets/js/jquery-2.0.3.min.js"></script>
    <script src="assets/js/bootstrap.min.js"></script>
    <script src="assets/js/typeahead-bs2.min.js"></script>
    <script src="assets/js/ace-elements.min.js"></script>
    <script src="assets/js/ace.min.js"></script>
    <script src="assets/js/ace-extra.min.js"></script>

    <script src="assets/js/jquery.gritter.min.js"></script>
    <script src="assets/js/bootbox.min.js"></script>
</head>
<body>
<div class="main-container">
    <h3>查看文件列表 (仅展示后20条记录)</h3>

    <div class="col-xs-8">
        <#list files as file>
            <div class="col-xs-5">
                <span><a href="downfile?file=${file.fileName}">${file.fileName}</a></span>
            </div>
            <div class="col-xs-7">
                ${file.lastModified}
            </div>
        </#list>
    </div>
</div>
</body>
</html>