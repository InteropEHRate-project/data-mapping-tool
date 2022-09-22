var ExtractStructureDialog = (function () {
	let instance = null;

	function PrivateConstructor() {

		const dialog = $("#extractStructurePreferencesDialog");
		// hidden by default
		dialog.modal('hide');
		let worksheetId, columnId;

		function init() {

			/**
			 * Initialize what happens when we show the dialog
			 */
			dialog.on('shown.bs.modal', function () {
				console.log("dialog displayed");
				hideError();

				/**
				 * Initialize handler for the dropdown to select the nlp pipeline
				 */
				var strPipelinePref = "[";
				var encodedUrl = encodeURIComponent(scroll_service_url + "/nlp/pipelines");
				$.ajax({
					url: "HTTPRequestHandler?" + encodedUrl,
					type: "GET",
					async: false,
					dataType: "json",
					success: function (data, textStatus, xhr) {
						var json = $.parseJSON(xhr.responseText);
						$.each(json, function (index, data) {
							strPipelinePref = strPipelinePref.concat('{\"preferences\": \"', data["name"], '\"},')
						});

						strPipelinePref = strPipelinePref.slice(0, -1);
						strPipelinePref = strPipelinePref.concat(']');

						var jsonPipelinePref = JSON.parse(strPipelinePref);
						var dialogContentPipeline = $("#userSelectionPipeline", dialog);
						dialogContentPipeline.empty();
						$.each(jsonPipelinePref, function (index, data) {
							if (data["preferences"] === "IEPrescriptionPipeline") {
								dialogContentPipeline.append($("<option>")
									.attr("selected", "selected")
									.html(data["preferences"]))
								;
							} else {
								dialogContentPipeline.append($("<option>")
									.html(data["preferences"]))
								;
							}
						});
					},
					error: function (xhr, textStatus) {
						$('#btnCancel', dialog).click();
						alert("No NLP Pipeline Found. Check the SCROLL URL in the settings");
					}
				});

				/**
				 * Initialize handler for the dropdown to select the language of the input text
				 */
				AttachLanguageOptions($("#userSelectionLocale", dialog))
			});

			/**
			 * Initialize handler for User Selection Dialog Save button
			 */
			$('#btnSave', dialog).on('click', function (e) {
				hideError();
				e.preventDefault();
				saveDialog(e);
			});
		}

		function hideError() {
			$("div.error", dialog).hide();
		}

		function saveDialog(e) {
			console.log("Save clicked");

			window.setTimeout(function () {
				dialog.modal('hide');
			}, 100);

			var info = generateInfoObject(worksheetId, columnId, "ExtractStructureCommand");

			info["hTableId"] = "";
			info["rootLocale"] = $("#userSelectionLocale option:selected", dialog).val();
			info["pipeline"] = $("#userSelectionPipeline option:selected", dialog).val();

			var newInfo = info['newInfo'];
			newInfo.push(getParamObject("rootLocale", info["rootLocale"], "other"));
			newInfo.push(getParamObject("pipeline", info["pipeline"], "other"));

			info["newInfo"] = JSON.stringify(newInfo);

			showLoading(info["worksheetId"]);
			$.ajax({
				url: "RequestController",
				type: "POST",
				data: info,
				dataType: "json",
				complete: function (xhr, textStatus) {
					var json = $.parseJSON(xhr.responseText);
					parse(json);
					hideLoading(info["worksheetId"]);
				},
				error: function (xhr, textStatus) {
					alert("Error occurred with " + info['command'] + textStatus);
				}
			});
		}

		function show(wsId, colId) {
			worksheetId = wsId;
			columnId = colId;
			dialog.modal({
				keyboard: true,
				show: true,
				backdrop: 'static'
			});
		}

		return { // Return back the public methods
			show: show,
			init: init
		}
	}

	/**
	 * a singleton to be used for semTest extraction dialog.
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

var ExtractConceptsDialog = (function () {
	var instance = null;

	function PrivateConstructor() {
		var dialog = $("#extractConceptsDialog");
		var extractionConceptsService_URL = $('#extractionConceptsService_URL', dialog);
		var conceptSelDialog = $("#extractionConceptsCapabilitiesDialog");
		// hidden by default
		conceptSelDialog.modal('hide');

		var worksheetId, columnId;
		var textToConceptRowId = 0;
		var serviceInBuilt = true;

		function init() {

			/**
			 * Initialize what happens when we show the dialog
			 */
			dialog.on('show.bs.modal', function (e) {
				console.log("dialog displayed");
				hideError();
				// text box for url to external service
				extractionConceptsService_URL.val(scroll_service_url);
				if (serviceInBuilt) {
					$('#btnSave', dialog).click();
				}
			});

			/**
			 * Initialize handler for URL Dialog Save button
			 */
			$('#btnSave', dialog).on('click', function (e) {
				e.preventDefault();
				if (saveDialog(e)) {

					window.setTimeout(function () {
						dialog.modal('hide');
					}, 100);

					console.log("dialog hidden after save");
					conceptSelDialog.modal('show');
					console.log("User selection dialog displayed");
				}
			});

			/**
			 * Initialize handler for User Selection Dialog Save button
			 */
			$('#btnSave', conceptSelDialog).on('click', function (e) {
				e.preventDefault();
				saveUserSelDialog(e);
			});
		}

		function hideError() {
			$("div.error", dialog).hide();
		}

		function showError() {
			$("div.error", dialog).show();
		}

		function saveDialog(e) {
			console.log("Save clicked");
			let isCorrectURL = false;

			/**
			 * Initialize handler for the dropdown to select the nlp pipeline
			 */
			var strPipelinePref = "[";
			var encodedUrl = encodeURIComponent(extractionConceptsService_URL.val() + "/nlp/pipelines");
			$.ajax({
				url: "HTTPRequestHandler?" + encodedUrl,
				type: "GET",
				async: false,
				dataType: "json",
				success: function (data, textStatus, xhr) {
					var json = $.parseJSON(xhr.responseText);
					$.each(json, function (index, data) {
						strPipelinePref = strPipelinePref.concat('{\"preferences\": \"', data["name"], '\"},')
					});

					strPipelinePref = strPipelinePref.slice(0, -1);
					strPipelinePref = strPipelinePref.concat(']');

					var jsonPipelinePref = JSON.parse(strPipelinePref);
					var dialogContentPipeline = $("#userSelectionPipeline", conceptSelDialog);
					dialogContentPipeline.empty();
					$.each(jsonPipelinePref, function (index, data) {
						if (data["preferences"] === "ConceptExtractionPipeline") {
							dialogContentPipeline.append($("<option>")
								.attr("selected", "selected")
								.html(data["preferences"]))
							;
						} else {
							dialogContentPipeline.append($("<option>")
								.html(data["preferences"]))
							;
						}
					});
					isCorrectURL = true;
				},
				error: function (xhr, textStatus) {
					alert("No NLP Pipeline Found. Check the SCROLL URL in the settings");
				}
			});

			/**
			 * Initialize handler for the dropdown for Concepts to the dialog
			 */
			var strUserPreferences =
				"  [{\"preferences\": \"Must Have\"}"
				+ ",{\"preferences\": \"Nice To Have\"}"
				+ ",{\"preferences\": \"Can Ignore\"}"
				+ "]";
			var jsonRespPreferences = JSON.parse(strUserPreferences);
			var dialogContentPreferences = $("#userSelectionPreferences", conceptSelDialog);
			dialogContentPreferences.empty();
			$.each(jsonRespPreferences, function (index, data) {
				if (data["preferences"] === "Can Ignore") {
					dialogContentPreferences.append($("<option>")
						.attr("selected", "selected")
						.html(data["preferences"]))
					;
				} else {
					dialogContentPreferences.append($("<option>")
						.html(data["preferences"]))
					;
				}
			});

			/**
			 * Initialize handler for the Auto Complete for root concept
			 */
			var rootConceptRowElement = $("#rootConceptRow", conceptSelDialog);
			attachAutoComplete(rootConceptRowElement);
			$("#reset", rootConceptRowElement).click()

			/**
			 * Initialize handler for the Auto Complete for Text to Concept linking to the dialog
			 */
			var linkingTextToConceptForm = $("#linkingTextToConceptForm", conceptSelDialog);
			var linkingTextToConceptRowCopy = $('#linkingTextToConceptRow', linkingTextToConceptForm).clone();
			$('#linkingTextToConceptRow', linkingTextToConceptForm).prop("id", "linkingTextToConceptRow" + textToConceptRowId);
			var linkingTextToConceptRow1 = $("#linkingTextToConceptRow" + textToConceptRowId)
			attachAutoComplete(linkingTextToConceptRow1);
			$("#reset", linkingTextToConceptRow1).click()

			/**
			 * Initialize handler for "Add More" button for Text to concept
			 */
			$("#addNewTextToConcept", conceptSelDialog).on("click", function (e) {
				e.preventDefault();
				// detach the "Add New" Button
				var det_elem = $("#addNewTextToConceptRowButton", linkingTextToConceptForm).detach();
				textToConceptRowId++;
				// Adding new Row for Text to Concept
				linkingTextToConceptRowCopy.clone().prop("id", "linkingTextToConceptRow" + textToConceptRowId)
					.appendTo(linkingTextToConceptForm);
				var linkingTextToConceptRowX = $("#linkingTextToConceptRow" + textToConceptRowId)
				attachAutoComplete(linkingTextToConceptRowX)
				$("#reset", linkingTextToConceptRowX).click()
				// Add again the "Add New" button
				linkingTextToConceptForm.append(det_elem);
				console.log("new row added with Id: linkingTextToConceptRow" + textToConceptRowId);
			});

			/**
			 * Initialize handler for the dropdown to select the language of the input text
			 */
			AttachLanguageOptions($("#userSelectionLocale", conceptSelDialog))

			return isCorrectURL;

		}

		function saveUserSelDialog(e) {
			console.log("Save clicked");

			window.setTimeout(function () {
				conceptSelDialog.modal('hide');
			}, 100);

			var i;
			var conceptForTextArray = new Array();
			for (i = 0; i <= textToConceptRowId; i++) {
				var obj = new Object();
				obj.text = $("#textForConcept", "#linkingTextToConceptRow" + i).val();
				obj.concept = $("#linkingTextToConceptRow" + i).data("concept");
				obj.locale = $("#linkingTextToConceptRow" + i).data("locale");
				if (obj.text && obj.concept) {
					conceptForTextArray.push(obj);
				}
			}

			var info = generateInfoObject(worksheetId, columnId, "ExtractConceptsCommand");

			info["hTableId"] = "";
			info["extractionURL"] = extractionConceptsService_URL.val();
			var rootConcept = $("#rootConceptRow", conceptSelDialog).data("concept");
			info["rootConcept"] = rootConcept !== "" && typeof rootConcept !== "undefined" ? rootConcept["id"] : "";
			info["rootLocale"] = $("#userSelectionLocale option:selected", conceptSelDialog).val();
			info["preferenceLevel"] = $("#userSelectionPreferences option:selected", conceptSelDialog).val();
			info["pipeline"] = $("#userSelectionPipeline option:selected", conceptSelDialog).val();
			info["conceptForTextArray"] = JSON.stringify(conceptForTextArray);

			var newInfo = info['newInfo'];
			newInfo.push(getParamObject("extractionURL", info["extractionURL"], "other"));
			newInfo.push(getParamObject("rootConcept", info["rootConcept"], "other"));
			newInfo.push(getParamObject("rootLocale", info["rootLocale"], "other"));
			newInfo.push(getParamObject("preferenceLevel", info["preferenceLevel"], "other"));
			newInfo.push(getParamObject("pipeline", info["pipeline"], "other"));
			newInfo.push(getParamObject("conceptForTextArray", info["conceptForTextArray"], "other"));

			info["newInfo"] = JSON.stringify(newInfo);

			showLoading(info["worksheetId"]);
			$.ajax({
				url: "RequestController",
				type: "POST",
				data: info,
				dataType: "json",
				complete: function (xhr, textStatus) {
					var json = $.parseJSON(xhr.responseText);
					parse(json);
					// console.log(json);
					hideLoading(info["worksheetId"]);
				},
				error: function (xhr, textStatus) {
					alert("Error occurred with " + info['command'] + textStatus);
				}
			});
		};

		/**
		 * autoComplete functionality for Linking Text to Concept and root Concept
		 * @param rowElement
		 */
		function attachAutoComplete(rowElement) {
			/**
			 * Initialize handler for the dropdown to select the language of the input text
			 */
			$("#reset", rowElement).on("click", function (e) {
				$(rowElement).data("concept", "");
			});
			AttachLanguageOptions($("#locale", rowElement))
			$("#conceptSearch", rowElement).autocomplete({
				source: function (request, response) {
					var encodedUrl = encodeURIComponent(
						extractionConceptsService_URL.val()
						+ "/concepts?pageIndex=1&pageSize=10&knowledgeBase=1"
						+ "&considerTokens=false&excludeFirstToken=false"
						+ "&includeTimestamps=false&includeRelationsCount=false"
						+ "&wordPrefix=" + request.term.toLowerCase()
						+ "&locale=" + $("#locale", rowElement).val()
					);
					$.ajax({
						url: "HTTPRequestHandler?" + encodedUrl,
						dataType: "json",
						type: "GET",
						success: function (data) {
							response($.map(data, function (item) {
									return {
										label: item["name"][$("#locale", rowElement).val()].toUpperCase()
											+ "-" + item["id"] + " : \""
											+ item["description"][$("#locale", rowElement).val()] + "\"",
										value: item["name"][$("#locale", rowElement).val()] + "-" + item["id"],
										concept: item
									};
								})
							);
						}
					});
				},
				minLength: 2,
				appendTo: rowElement,
				change: function (event, ui) {
					$(rowElement).data("concept", ui.item != null ? ui.item.concept : "");
					$(rowElement).data("locale", $("#locale", rowElement).val());
				},
				select: function (event, ui) {
					$(rowElement).data("concept", ui.item.concept);
					$(rowElement).data("locale", $("#locale", rowElement).val());
				},
				open: function () {
					$(this).removeClass("ui-corner-all").addClass("ui-corner-top");
				},
				close: function () {
					$(this).removeClass("ui-corner-top").addClass("ui-corner-all");
				}
			});
		}

		function show(wsId, colId) {
			worksheetId = wsId;
			columnId = colId;
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
	 * a singleton to be used for concept extraction dialog.
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

function AttachLanguageOptions(dialogLocale){
	dialogLocale.empty();
	dialogLocale.append($("<option>")
		.attr("selected", "selected")
		.attr("value", "Auto Detect")
		.html('Auto Detect'))
	;
	const encodedUrl = encodeURIComponent(scroll_service_url + "/vocabularies?knowledgeBase=1&includeTimestamps=false");
	$.ajax({
		url: "HTTPRequestHandler?" + encodedUrl,
		type: "GET",
		async: false,
		dataType: "json",
		success: function (data, textStatus, xhr) {
			var response = $.parseJSON(xhr.responseText);
			$.each(response, function (index, data) {
				dialogLocale.append($("<option>")
					.attr("value", data["languageCode"])
					.html(data["displayLanguage"]))
				;
			});
		}
	});
}
