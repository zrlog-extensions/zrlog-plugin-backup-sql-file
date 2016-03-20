<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version}</title>

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
    <script src="js/set_update.js"></script>
    <script>
        $(document).ready(function() {
            $("#testEmailService").click(function(e){
                $.get("testEmailService",function(data){

                })
            })
        });
    </script>
</head>
<body>
<div class="main-container">
    <form id="ajaxemailServiceMsg" class="form-horizontal" role="form">
        <h4 class="header blue">${_plugin.desc}</h4>

        <div class="form-group">
            <label class="col-sm-3 control-label no-padding-right" for="form-field-select-1"> 备份周期 </label>

            <div class="col-sm-9">
                <select id="form-field-select-1" class="form-control" name="cycle">
                    <option value="3600"  <#if '3600'==cycle>selected="selected"</#if> >1小时</option>
                    <option value="21600" <#if '21600'==cycle>selected="selected"</#if>>6小时</option>
                    <option value="43200" <#if '43200'==cycle>selected="selected"</#if>>12小时</option>
                    <option value="86400" <#if '86400'==cycle>selected="selected"</#if>>1天</option>
                </select>
            </div>
        </div>

        <div class="clearfix form-actions">
            <div class="col-md-offset-3 col-md-9">
                <button class="btn btn-info" type="button" id="emailServiceMsg">
                    <i class="icon-ok bigger-110"></i> 提交
                </button>
            </div>
        </div>
    </form>
    <a href="filelist">查看历史备份文件</a>
</div>
<input id="gritter-light" checked="" type="checkbox" class="ace ace-switch ace-switch-5"/>
</body>
</html>