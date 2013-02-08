AtTaskClaimBlame = {
	init: function(rootUrl, projectId) {
		AtTaskClaimBlame.rootUrl = rootUrl;
		AtTaskClaimBlame.projectId = projectId;

		$$('.ClaimBlameCell .status input[type=button]').each(function(it) {
			it.observe('click', AtTaskClaimBlame.onAcceptDoneButtonClicked);
		});

		$$('.ClaimBlameCell select').each(function(it) {
			it.observe('change', AtTaskClaimBlame.onUserChange);
		});
	},

	onAcceptDoneButtonClicked: function(e) {
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
				console.log('successfully updated', transport);
				button.removeAttribute('disabled');
			},
			onFailure:function (transport) {
				console.log('failed to update', transport);
				button.removeAttribute('disabled');
			}
		});
	},

	onUserChange: function(e) {
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
				console.log('successfully updated', transport);

				selectBox.removeAttribute('disabled');
			},
			onFailure:function (transport) {
				console.log('failed to update', transport);

				selectBox.removeAttribute('disabled');
			}
		});
	}
};
