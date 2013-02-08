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
		debugger;
		var button = e.target;
		button.setAttribute('disabled', true);
		var cell = button.up('.ClaimBlameCell');
		var testName = cell.getAttribute('name');

		var url = AtTaskClaimBlame.rootUrl + '/' + 'claimBlame/updateStatus';
		//testNames
		//status
		//projectId

		new Ajax.Request(url, {
			method:'post',
			parameters: {
				testNames: testName,
				status: button.getAttribute('name'),
				projectId: AtTaskClaimBlame.projectId
			},
			onSuccess:function (transport) {
				console.log('successfully updated');
				button.removeAttribute('disabled');
			},
			onFailure:function () {
				console.log('failed to update');
				button.removeAttribute('disabled');
			}
		});
	},

	onUserChange: function(e) {
		debugger;
		var selectBox = e.target;
		selectBox.setAttribute('disabled', true);

		var selectedBoxes = $$('.ClaimBlameCell .bulkSelect[checked]');
		var parameters;
		if(selectedBoxes.length > 0) {
			//bulk
			parameters = {};
		} else {
			//single
			var cell = selectBox.up('.ClaimBlameCell');
			var testName = cell.getAttribute('name');

			var selectedUser = selectBox.value;
			parameters = {
				testNames: testName,
				userId: selectedUser,
				projectId: AtTaskClaimBlame.projectId,
				notifyBlamed: true
			};
		}
		console.log(e);

		var url = AtTaskClaimBlame.rootUrl + '/' + 'claimBlame/blame';
		new Ajax.Request(url, {
			method:'post',
			parameters: parameters,
			onSuccess:function (transport) {
				console.log('successfully updated');

				selectBox.removeAttribute('disabled');
			},
			onFailure:function () {
				console.log('failed to update');

				selectBox.removeAttribute('disabled');
			}
		});
	}
};
