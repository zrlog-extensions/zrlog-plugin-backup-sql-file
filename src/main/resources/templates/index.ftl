<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version}</title>

    <link rel="stylesheet" href="assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="assets/css/font-awesome.min.css"/>
    <link rel="stylesheet" href="assets/css/jquery.gritter.css"/>

    <script src="assets/js/jquery-2.0.3.min.js"></script>
    <script src="assets/js/bootstrap.min.js"></script>
    <script src="assets/js/jquery.gritter.min.js"></script>
    <script src="js/set_update.js"></script>
</head>
<body>
<div class="col-xs-8">
    <h3>${_plugin.desc}</h3>
    <div class="text-right">
        <a href="exportSqlFile">
            <button class="btn btn-default" type="button" id="emailServiceMsg">
                <i class="icon-download-alt bigger-110"></i>&nbsp;导出SQL文件
            </button>
        </a>
    </div>
    <hr/>
    <form id="ajaxemailServiceMsg" class="form-horizontal" role="form">
        <div class="form-group">
            <label class="col-sm-3 control-label no-padding-right"> 备份周期 </label>
            <div class="col-sm-3">
                <select class="form-control" name="cycle">
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
            <div>
                <h4><i class="icon-file bigger-110"></i>&nbsp;<a href="filelist">查看历史备份文件</a></h4>
            </div>
        </div>
        <hr/>
        <div class="col-md-offset-3 col-md-9">
            <button class="btn btn-info" type="button" id="emailServiceMsg">
                <i class="icon-ok bigger-110"></i> 提交
            </button>
        </div>
    </form>
    <input id="gritter-light" checked="" type="checkbox" style="display:none"/>
</div>
</body>
</html>