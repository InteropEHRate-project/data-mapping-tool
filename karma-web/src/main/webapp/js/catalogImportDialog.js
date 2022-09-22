$(document).on("click", "#importCatalogButton", function () {
	CatalogImportDialog.getInstance().show();
});

$(document).on("click", "#showDownloadProgress", function () {
	$("#downloadProgressDialog").modal('show');
});


var CatalogImportDialog = (function () {
	var instance = null;

	function PrivateConstructor() {
		var dialog = $("#catalogImportDialog");
		var iframeDialog = $("#iFrameDialog");
		var catalogURL = $('#catalogURL', dialog);
		var catalogApiKey = $('#catalogApiKey', dialog);
		var downloadProgressDialog = $("#downloadProgressDialog");

		function init() {
			let iframeClosed = false;

			/**
			 * Initialize what happens when we show the dialog
			 */
			dialog.on('show.bs.modal', function (e) {
				console.log("catalog URL dialog displayed");
				hideError();
				//populate text box with url form environment variable or configuration file.
				catalogURL.val(catalog_base_url);
				catalogApiKey.val(catalog_instance_api_key);
			});

			/**
			 * Initialize handler for catalogURL Dialog Save button.
			 * it creates an iframe of ckan-client if doesnot exist already and display it on GUI
			 */
			$('#btnSave', dialog).on('click', function (e) {
				e.preventDefault();

				window.setTimeout(function () {
					dialog.modal('hide');
				}, 100);

				console.log("dialog hidden after save clicked");

				updateSettings();

				localStorage.setItem('apiUrl', catalogURL.val());
				localStorage.setItem('apiKey', catalogApiKey.val());
				localStorage.setItem('karmaPathName', window.location.pathname);

				if ($('#catalogImportFrame', iframeDialog).length === 0) {
					console.log('creating iframe window');
					const src = window.location.protocol + "//" + window.location.host
						+ window.location.pathname + 'ckan-client' + '/';
					$('<iframe>', {
						src: src,
						id: 'catalogImportFrame',
						style: 'position:fixed; width:100%; height:100%; border: none;'
					}).appendTo('#iFrameDialogContainer', iframeDialog);
				}
				//reset selected dataset values.
				localStorage.setItem('isDatasetSelected', 'false');
				localStorage.setItem('datasetId', '');
				iframeDialog.modal('show');
				iframeClosed = false;

				/**
				 * Handler for iframe close.
				 * @type {number}
				 */
				let timer = setInterval(function () {
					if (localStorage.getItem('isDatasetSelected') === 'true') {
						clearInterval(timer);
						$('#closeIframe', iframeDialog).click();
					}
					if (iframeClosed) {
						clearInterval(timer);
					}
				}, 500);

			});

			/**
			 * Handler after the close of iframe
			 */
			$('#closeIframe', iframeDialog).on('click', function (e) {
				iframeClosed = true;
				e.preventDefault();
				window.setTimeout(function () {
					dialog.modal('hide');
				}, 100);

				const datasetId = localStorage.getItem('datasetId');
				const selectedResources = localStorage.getItem('selectedResources');

				if (typeof datasetId !== 'undefined' && datasetId) {
					console.log('Dataset Selected');
					importCatalog(datasetId, selectedResources);
					showWaitingSignOnScreen();
				} else {
					$.sticky("No Dataset Selected");
				}
				$('#catalogImportFrame', iframeDialog).remove();
			});
		}

		/**
		 * update knowDive config file with catalog url and api-key
		 */
		function updateSettings() {
			var info = generateInfoObject("", "", "UpdateKnowDiveServicesConfigurationCommand");
			var newInfo = info['newInfo'];

			newInfo.push(getParamObject("new_catalog_base_url", catalogURL.val(), "other"));
			newInfo.push(getParamObject("new_catalog_instance_api_key", catalogApiKey.val(), "other"));

			info["newInfo"] = JSON.stringify(newInfo);

			$.ajax({
				url: "RequestController",
				type: "POST",
				data: info,
				dataType: "json",
				complete: function (xhr, textStatus) {
					let json = $.parseJSON(xhr.responseText);
					parse(json);
					catalog_base_url = catalogURL.val();
					catalog_instance_api_key = catalogApiKey.val();
				},
				error: function (xhr, textStatus) {
					alert("Error occurred with " + info['command'] + textStatus);
				}
			});
		}

		/**
		 * handler for download progress dialog
		 *
		 * @param datasetId
		 * @param selectedResources
		 */
		function importCatalog(datasetId, selectedResources) {
			var info = generateInfoObject("", "", "ImportCatalogCommand");
			info["datasetId"] = datasetId;
			info["selectedResources"] = selectedResources;

			$.ajax({
				url: "RequestController",
				type: "POST",
				data: info,
				dataType: "json",
				complete: function (xhr) {
					let json = $.parseJSON(xhr.responseText);
					parse(json);
					downloadProgressDialog.modal('show');
				},
				error: function (xhr, textStatus) {
					alert("Error occurred with Catalog Import! " + textStatus);
				}
			});
		}

		/**
		 * handler when download dialog is shown
		 */
		downloadProgressDialog.on('show.bs.modal', function (e) {
			hideWaitingSignOnScreen();
			$("body").removeClass("modal-open");
			downloadProgressDialog.removeClass("modal");
			const fetchAll = true;
			const isLastIteration = false;
			showProgress(fetchAll, isLastIteration);
		});

		/**
		 * handler to keep track of the download progress.
		 * create accordion with panel-group and list-group.
		 *
		 * @param fetchAll set it to true for initial and final fetch request, otherwise false
		 * @param isLastIteration set it to true for final fetch.
		 */
		function showProgress(fetchAll, isLastIteration) {
			setTimeout(function () {
				let info = {};
				info["fetchAll"] = fetchAll;
				let url = window.location.pathname;
				url += window.location.pathname.endsWith('/') ? '' : '/';
				url += "catalog-rest/download/fetch";
				console.log(url);
				$.ajax({
					url: url,
					type: "POST",
					data: info,
					dataType: "json",
					contentType: "application/x-www-form-urlencoded",
					complete: function (xhr, textStatus) {
						let json = $.parseJSON(xhr.responseText);

						// https://codepen.io/bootpen/pen/BooWaR
						let accordion = $("#accordion", downloadProgressDialog);
						if (fetchAll === true) { // initial data loading
							accordion.remove();
							accordion = $("<div>")
								.attr("class", "panel-group")
								.attr("aria-multiselectable", "true")
								.attr("role", "tablist")
								.attr("id", "accordion");
							accordion.appendTo("#body", downloadProgressDialog);
						}
						// variable use to expand most resent downloading dataset
						let isExpand = true;
						$.each(json['datasets'], function (index, dataset) {

							const datasetUniqueName = dataset["uniqueName"];
							if (fetchAll === true) { // initial data loading
								let panel = ($("<div>")
									.attr("class", "panel panel-default"))
									.attr("id", datasetUniqueName);
								panel.appendTo(accordion);
								appendPanelHeading(panel, datasetUniqueName, isExpand);
								appendPanelBody(panel, datasetUniqueName, dataset['resources'], isExpand);
								isExpand = false;
							} else if (!dataset['isDatasetDownloaded']
								&& $("#collapse" + datasetUniqueName, accordion).length !== 0) { // reset the panel body with new Info
								$("#listResources" + datasetUniqueName, accordion).remove();
								appendListResources($("#collapse" + datasetUniqueName, accordion), datasetUniqueName, dataset['resources']);
							}

						});
						if (!json['isAllDownloaded']) {
							showProgress(false, false);
						} else { //for final iteration fetch all the resource
							if (!isLastIteration) { //final download Fetch
								showProgress(true, true);
							} else {
								$.sticky("Download Completed");
							}
						}
					},
					error: function (xhr, textStatus) {
						console.log("Error" + textStatus);
					}
				});
			}, 500);
		}

		/**
		 * function to create panel heading for individual dataset
		 *
		 * @param toAppend Jquery variable where new html will appended to
		 * @param datasetUniqueName
		 * @param isExpand
		 */
		function appendPanelHeading(toAppend, datasetUniqueName, isExpand) {
			let heading = $("<div>")
				.attr("class", "panel-heading")
				.attr("role", "tab")
				.attr("id", "heading" + datasetUniqueName);
			let h4 = $("<h4>").attr("class", "panel-title");
			let a = $("<a>")
				.attr("role", "button")
				.attr("data-toggle", "collapse")
				.attr("data-parent", "#accordion")
				.attr("href", "#collapse" + datasetUniqueName)
				.attr("aria-expanded", isExpand ? "true" : "false")
				.attr("aria-controls", "collapse" + datasetUniqueName)
				.html(datasetUniqueName)
				.append($("<i>")
					.attr("class", "more-less glyphicon glyphicon-plus")
				);
			toAppend.append(heading.append(h4).append(a));
		}

		/**
		 * function to create panel body for a given dataset.
		 *
		 * @param appendTo Jquery variable where new html will appended to
		 * @param datasetUniqueName
		 * @param datasetResources
		 * @param isExpand
		 */
		function appendPanelBody(appendTo, datasetUniqueName, datasetResources, isExpand) {
			let panel_collapse = $("<div>")
				.attr("class", isExpand ? "panel-collapse collapse in" : " panel-collapse collapse")
				.attr("role", "tabpanel")
				.attr("aria-expanded", isExpand ? "true" : "false")
				.attr("aria-labelledby", "heading" + datasetUniqueName)
				.attr("id", "collapse" + datasetUniqueName);
			appendTo.append(panel_collapse);
			appendListResources(panel_collapse, datasetUniqueName, datasetResources);
		}

		/**
		 * function to create list of resources for a given data set in to be displayed in download dialog.
		 *
		 * @param appendTo Jquery variable where new html will appended to
		 * @param datasetUniqueName
		 * @param datasetResources
		 */
		function appendListResources(appendTo, datasetUniqueName, datasetResources) {
			//reset all the on click events linked to appendTo
			appendTo.off('click');
			let body = $("<ul>")
				.attr("class", "panel-body list-group")
				.attr("id", "listResources" + datasetUniqueName);

			$.each(datasetResources, function (index, resource) {
				// resource name
				const finalSize = resource["finalSize"] !== -1 ? "/" + formatBytes(resource["finalSize"]) : "";
				let resourceContainerHTML = resource["name"] + " (" + formatBytes(resource["currentSize"]) + finalSize + ")";
				resourceContainerHTML.concat(resource["isError"] ? "ERROR WHILE DOWNLOADING" : "");
				let resourceContainer = $("<li>")
					.attr("class", "list-group-item container")
					.html(resourceContainerHTML);
				body.append(resourceContainer);

				if (resource["isError"] || resource["isCanceled"])
					return;

				// progress bar
				if (resource["finalSize"] !== -1 && resource["currentSize"] <= resource["finalSize"]) {
					const aria_valuenow = ~~((resource["currentSize"] / resource["finalSize"]) * 100);
					let progressBar = ($("<div>")
							.attr("class", 'progress')
					).append($("<div>")
						.attr("class", "progress-bar progress-bar-success progress-bar-striped")
						.attr("role", "progressbar")
						.attr("aria-valuenow", aria_valuenow)
						.attr("aria-valuemin", "0")
						.attr("aria-valuemax", "100")
						.attr("style", "width: " + aria_valuenow + "%")
					);
					resourceContainer.append(progressBar);
				}

				// Load In Karma button
				let disableLoadButton = true;
				if (resource["isResourceDownloaded"] === true && resource["isCanceled"] === false)
					disableLoadButton = false;
				let loadInKarmaButton = $("<div>")
					.attr("class", "pull-right").append($("<button>")
						.attr("type", "button")
						.attr("class", 'btn btn-xs btn-primary')
						.attr("id", "listResources" + datasetUniqueName + index)
						.attr('disabled', disableLoadButton)
						.html("Load In Karma")
					);

				function loadResource() {
					loadResourceToWorkSpace(datasetUniqueName, resource);
				}

				//on click Load In Karma button
				appendTo.on('click', "#listResources" + datasetUniqueName + index, loadResource);
				resourceContainer.append(loadInKarmaButton);

				//cancel Button
				let disableCancelButton = false;
				if (resource["isResourceDownloaded"] === true || resource["isCanceled"] === true)
					disableCancelButton = true;
				let cancelButton = $("<div>")
					.attr("class", "pull-right").append($("<button>")
						.attr("type", "button")
						.attr("class", 'btn btn-xs btn-danger')
						.attr("id", "cancelButton" + datasetUniqueName + index)
						.attr('disabled', disableCancelButton)
						.html("Cancel")
					);

				function cancelDownload() {
					let info = {};
					info["resourceName"] = resource["name"];
					info["datasetDirectoryName"] = datasetUniqueName;
					$.ajax({
						url: "/catalog-rest/download/cancel",
						type: "POST",
						data: info,
						contentType: "application/x-www-form-urlencoded",
						dataType: "json",
						complete: function (xhr, textStatus) {
							let json = $.parseJSON(xhr.responseText);
							if (json === true)
								console.log('resource download cancelled successful ' + info["resourceName"])
						},
						error: function (xhr, textStatus) {
							alert("Error occurred while canceling the download! " + textStatus);
						}
					});
				}

				//on click cancel button
				appendTo.on('click', "#cancelButton" + datasetUniqueName + index, cancelDownload);
				resourceContainer.append(cancelButton);

			});
			appendTo.append(body);
		}

		/**
		 * function to load a downloaded resource in karma
		 *
		 * @param datasetUniqueName
		 * @param resource
		 */
		function loadResourceToWorkSpace(datasetUniqueName, resource) {
			let data = [];
			data["fileName"] = resource["name"];
			data["datasetName"] = datasetUniqueName;
			FileFormatSelectionDialog.getInstance().showForCatalogResource(data);
		}

		/**
		 * dataset resources toggle for download dialog
		 * @param e
		 */
		function toggleIcon(e) {
			$(e.target)
				.prev('.panel-heading')
				.find(".more-less")
				.toggleClass('glyphicon-plus glyphicon-minus');
		}

		$('.panel-group', downloadProgressDialog).on('hidden.bs.collapse', toggleIcon);
		$('.panel-group', downloadProgressDialog).on('shown.bs.collapse', toggleIcon);

		/**
		 * handler to adjust settings when download dialog is moved around on the screen.
		 */
		$('#modal-content', downloadProgressDialog).draggable().resizable();
		$('#modal-content', downloadProgressDialog).on("dragstop", function (event, ui) {
			downloadProgressDialog.css({
				top: ' calc( ' + downloadProgressDialog.css("top") + ' + ' + $('#modal-content', downloadProgressDialog).css("top") + ')',
				left: ' calc( ' + downloadProgressDialog.css("left") + ' + ' + $('#modal-content', downloadProgressDialog).css("left") + ')'
			});
			$('#modal-content', downloadProgressDialog).css({
				top: '0px',
				left: '0px'
			});
		});


		function hideError() {
			$("div.error", dialog).hide();
		}

		function show() {
			dialog.modal({
				keyboard: true,
				show: true,
				backdrop: 'static'
			});
		}


		return { // Return back the public methods
			show: show,
			init: init
		};
	}

	/**
	 * a singleton to be used for catalog import dialog.
	 *
	 * @returns instance
	 */
	function getInstance() {
		if (!instance) {
			instance = new PrivateConstructor();
			instance.init();
		}
		return instance;
	}

	return {
		getInstance: getInstance
	};


})();
