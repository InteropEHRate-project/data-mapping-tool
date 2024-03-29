<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>

<div class="modal fade" id="githubSettingsDialog" tabindex="-1">
  <div class="modal-dialog">
  		<form class="bs-example bs-example-form" role="form">
			<div class="modal-content">
			     <div class="modal-header">
				      <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				       <h4 class="modal-title">Github Settings</h4>
				  </div>
				  <div class="modal-body">

		              <div class="form-group">
						<label for="txtGithubUsername">Github Username</label>
						<input class="form-control" type="text" id="txtGithubUsername" required><br>
						<label for="txtGithubPassword">Github Password</label>
						<input class="form-control" type="password" id="txtGithubPassword" required><br>
						<div class="error"  style="display: none">Error: Please enter valid Username and Password</div>
					 </div>

              		</form>

		  		  </div> <!-- /.modal-body -->
				  <div class="modal-footer">
				        <button type="button" class="btn btn-default" data-dismiss="modal" id="btnCancel">Cancel</button>
				        <input type="button" class="btn btn-danger" value="Delete" id="btnDelete"/>
				        <input type="button" class="btn btn-primary" value="Submit" id="btnSave"/>
				  </div> <!-- /.modal-footer -->
			</div><!-- /.modal-content -->
		</form>
	</div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div class="modal fade" id="knowdiveServicesSettingsDialog" tabindex="-1">
	<div class="modal-dialog">
		<form class="bs-example bs-example-form" role="form">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h4 class="modal-title">KnowDive Micro-service Addresses</h4>
				</div>
				<div class="modal-body">
					<div class="form-group">
						<label for="scroll_service_url">SCROLL API</label>
						<input class="form-control" type="text" id="scroll_service_url" required>
					</div>
					<div class="form-group">
						<label for="eml_service_url">RDF to EML API</label>
						<input class="form-control" type="text" id="eml_service_url" required>
					</div>
					<div class="form-group">
						<label for="kos_service_url">KOS Application</label>
						<input class="form-control" type="text" id="kos_service_url" required>
					</div>
					<div class="form-group">
						<label for="kg_importer_url">Knowledge importer API</label>
						<input class="form-control" type="text" id="kg_importer_url">
					</div>
					<div class="form-group">
						<label for="catalog_base_url">Catalog Base URL</label>
						<input class="form-control" type="text" id="catalog_base_url">
					</div>
					<div class="form-group">
						<label for="catalog_instance_api_key">Catalog API KEY</label>
						<input class="form-control" type="text" id="catalog_instance_api_key">
					</div>


				</div> <!-- /.modal-body -->
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-dismiss="modal" id="btnCancel">Cancel</button>
					<button type="submit" class="btn btn-primary" id="btnSave">Save</button>
				</div> <!-- /.modal-footer -->
			</div><!-- /.modal-content -->
		</form>
	</div><!-- /.modal-dialog -->
</div><!-- /.modal -->

</body>
<html>
