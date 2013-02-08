AtTaskClaimBlame = {
	init: function(rootUrl, projectId, currentUserId) {
		AtTaskClaimBlame.rootUrl = rootUrl;
		AtTaskClaimBlame.projectId = projectId;
		AtTaskClaimBlame.currentUserId = currentUserId;
		debugger
		AtTaskClaimBlame.isUpdating = false; //Used to determine if the change/click events should fire

		$$('.ClaimBlameCell .status input[type=button]').each(function(it) {
			it.observe('click', AtTaskClaimBlame.onAcceptDoneButtonClicked);
		});

		$$('.ClaimBlameCell select').each(function(it) {
			it.observe('change', AtTaskClaimBlame.onUserChange);
		});
	},

	onAcceptDoneButtonClicked: function(e) {
		if(AtTaskClaimBlame.isUpdating) {
			return;
		}
		var button = e.target;
		button.setAttribute('disabled', true);

		var parameters = {
			testNames: [],
			status: button.getAttribute('name'),
			projectId: AtTaskClaimBlame.projectId
		};

		var checkboxes = $$(".ClaimBlameCell input[type='checkbox']");
		checkboxes.each(function(it) {
			if(it.checked) {
				var cell = it.up('.ClaimBlameCell');
				var testName = cell.getAttribute('name');
				parameters.testNames.push(testName);
			}
		});

		if(parameters.testNames.length <= 0) {
			//No checkboxes are checked
			var cell = button.up('.ClaimBlameCell');
			var testName = cell.getAttribute('name');
			parameters.testNames.push(testName);
		}

		var url = AtTaskClaimBlame.rootUrl + '/' + 'claimBlame/updateStatus';
		new Ajax.Request(url, {
			method:'post',
			parameters: parameters,
			onSuccess:function (transport) {
				AtTaskClaimBlame.onSuccessfulChange(transport.responseText);
				button.removeAttribute('disabled');
			},
			onFailure:function (transport) {
				console.log('failed to update', transport);
				button.removeAttribute('disabled');
			}
		});
	},

	onUserChange: function(e) {
		if(AtTaskClaimBlame.isUpdating) {
			return;
		}

		var selectBox = e.target;
		selectBox.setAttribute('disabled', true);

		var checkboxes = $$(".ClaimBlameCell input[type='checkbox']");
		var selectedUser = selectBox.value;
		var parameters = {
			userId: selectedUser,
			projectId: AtTaskClaimBlame.projectId,
			notifyBlamed: true,
			testNames: []
		};

		checkboxes.each(function(it) {
			if(it.checked) {
				var cell = it.up('.ClaimBlameCell');
				var testName = cell.getAttribute('name');
				parameters.testNames.push(testName);
			}
		});

		if(parameters.testNames <= 0) {
			//no checkboxes are checked
			var cell = selectBox.up('.ClaimBlameCell');
			var testName = cell.getAttribute('name');
			parameters.testNames.push(testName);
		}

		var url = AtTaskClaimBlame.rootUrl + '/' + 'claimBlame/blame';
		new Ajax.Request(url, {
			method:'post',
			parameters: parameters,
			onSuccess:function (transport) {
				AtTaskClaimBlame.onSuccessfulChange(transport.responseText);
				selectBox.removeAttribute('disabled');
			},
			onFailure:function (transport) {
				console.log('failed to update', transport);

				selectBox.removeAttribute('disabled');
			}
		});
	},

	onSuccessfulChange: function(jsonText) {
		AtTaskClaimBlame.isUpdating = true; //Prevent change and click events from re-firing

		var responseObject = eval('('+jsonText+')');
		Object.keys(responseObject).each(function(testName) {
			var value = responseObject[testName];
			if(value) {
				if(value.status) {
					var statusElement = $$(".ClaimBlameCell[name='"+testName+"'] .status").first();
					statusElement.setAttribute('class', 'status ' + value.status);
					var button = statusElement.down('input');
					if(value.status != 'Accepted') {
						button.setAttribute('name', 'Accepted');
						button.setAttribute('value', 'Accept');
					} else {
						button.setAttribute('value', 'Done');
						button.setAttribute('name', 'Done');
					}

					var span = statusElement.down('span');
					span.innerText = value.status;
				}
				if(value.culprit) {
					var select = $$(".ClaimBlameCell[name='"+testName+"'] select").first();
					for(var i = select.options.length - 1; i >= 0; i--) {
						if(select.options[i].value == value.culprit) {
							select.selectedIndex = i;
							break;
						}
					}

					var cell = $$(".ClaimBlameCell[name='"+testName+"']").first();
					if(value.culprit != '{null}' && value.culprit == AtTaskClaimBlame.currentUserId) {
						if(!cell.hasClassName('sameUser')) {
							cell.addClassName('sameUser');
						}
					} else if(cell.hasClassName('sameUser')) {
						cell.removeClassName('sameUser');
					}
				}
			}
		});

		AtTaskClaimBlame.isUpdating = false;
	}
};
