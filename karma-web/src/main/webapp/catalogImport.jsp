<div class="modal fade" id="catalogImportDialog" tabindex="-1">
    <div class="modal-dialog">
        <form class="bs-example bs-example-form" role="form">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title">Catalog Import</h4>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="catalogURL">URL to a CKAN Instance</label>
                        <input class="form-control" type="text" id="catalogURL" required>
                    </div>
                    <div class="form-group">
                        <label for="catalogApiKey">API KEY to a CKAN Instance (Optional)</label>
                        <input class="form-control" type="text" id="catalogApiKey">
                    </div>

                    <div class="error" style="display: none">Please enter a URL</div>
                </div> <!-- /.modal-body -->
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary" id="btnSave">OK</button>
                </div> <!-- /.modal-footer -->
            </div><!-- /.modal-content -->
        </form>
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div class="modal fade" id="iFrameDialog" tabindex="-1">
    <div class="modal-dialog" id="iFrame-modal-dialog">
        <div class="modal-content" id="iFrameDialogContainer">
            <div class="modal-header">
                <button id="closeIframe" type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Catalog Import</h4>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div class="modal download-custom-modal fade" id="downloadProgressDialog" data-backdrop="false">
    <div class="modal-dialog">
        <div class="modal-content" id="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times</button>
                <h4 class="modal-title">Catalog Download</h4>
            </div>
            <div class="modal-body" id="body">
                <div class="panel-group " id="accordion">
                </div>
            </div> <!-- /.modal-body -->
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->




