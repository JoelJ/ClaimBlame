if (!window.claim) {
    window.claim = function (url, select) {
        var user = select[select.selectedIndex].value;
        var testNames = [];
		var badges = $$('#BlameActionBadge');
		badges.each(function (it) {
			var checkbox=it.down('.bulkAssign');
            if (checkbox.checked) {
                testNames.push(it.getAttribute("testName"));
                var culprit = it.down('select');
                culprit.value = user;
            }
        });
        if (testNames.length > 0) {
            new Ajax.Request(url + "/blame/bulkBlame", {
                method:'post',
                parameters:{userID:user, testNames:testNames},
                onSuccess:function (transport) {

                    var resultJson = eval('(' + transport.responseText + ')');
                    badges.each(function (it) {
						var checkbox=it.down('.bulkAssign');
						if (checkbox.checked) {
                            var testNameAttr = it.getAttribute("testName");
                            var testJson=resultJson[testNameAttr];
							var statusSpan = $(it).down("span.status");
                            reloadStatusSpan(statusSpan, testJson.status, testJson.isYou);
                        }
                    });
                },
                onFailure:function () {
                    console.log('BulkBlame went wrong...');
                }
            });
        }
        else {
            new Ajax.Request(url + '/blame/blame', {
                method:'post',
                parameters:{userID:user},
                onSuccess:function (transport) {
                    var resultJson = eval('(' + transport.responseText + ')');
                    var statusSpan = $(select).up("#BlameActionBadge").down("span.status");
                    reloadStatusSpan(statusSpan, resultJson.status, resultJson.isYou);
                },
                onFailure:function () {
                    console.log('Blame went wrong...');
                }
            });
        }
        select.value = user;

    };
}

if(!window.initializedClaimBlame) {
    window.changeStatus = function (url, button) {
        new Ajax.Request(url + '/blame/status', {
            method:'post',
            parameters:{status:button.name},
            onSuccess:function (transport) {
                var resultJson = eval('(' + transport.responseText + ')');
                var statusSpan = $(button).up("#BlameActionBadge").down("span.status");
                reloadStatusSpan(statusSpan, resultJson.status, resultJson.isYou)
            },
            onFailure:function () {
                console.log(url);
                console.log(button);
                console.log(this);
                console.log('Change status went wrong...');
            }
        });
    };

    window.changeUserPageStatus = function (event) {
        var select = $(event.target);
        var url = select.getAttribute('url');
        new Ajax.Request(url + '/blame/status', {
            method:'post',
            parameters:{status:select.value},
            onSuccess:function (transport) {
            },
            onFailure:function () {
                console.log('Change User status went wrong...');
            }
        });
    };

    Event.observe(window, 'load', function(event) {
       $$('select[url]').each(function(it) {
           it.observe('change',changeUserPageStatus);
       });
    });

    window.initializedClaimBlame = true;
}

function reloadStatusSpan(statusSpan, newStatus, isCulprit) {
    statusSpan.setAttribute('class', "status " + newStatus + (isCulprit=='true' ? " isCulprit" : ""));
	var status = statusSpan.down('span');
	status.innerText = newStatus;

    var button = statusSpan.down('input');
    if (newStatus == 'NotAccepted') {
        button.value = 'Accept';
        button.name = 'Accepted';
    } else if (newStatus == 'Accepted') {
        button.value = 'Done';
        button.name = 'Done';
        button.style.display='block';
        status.style.display='none';
    } else if (newStatus == 'Done') {
        button.style.display='none';
        status.style.display='block';
    }
//	else if(newStatus == 'Unassigned'){
//		button.style.display='none';
//		status.style.display='block';
//	}
}

if (!window.userBulkStatus) {
    window.userBulkStatus = function (status) {
		var elementsByClassName = document.getElementsByClassName("status");
		var testNames=[];
		for (var i = 0; i < elementsByClassName.length; i++) {
			var element = elementsByClassName[i];
			var value = element.options[element.selectedIndex].value;
			var testName = element.up('tr').down('a').className;
			if(status =='Done'){
				if(value=='Accepted'){
					testNames.push(testName);
				}
			}else if(status =='Accepted'){
				if (value != 'Accepted' && value != 'Done') {
					testNames.push(testName);
				}
			}
		}
		if(testNames.length>0){
			new Ajax.Request(document.getElementById("testUrl").value + '/blame/bulkStatus', {
				method:'post',
				parameters:{status:status,testNames:testNames},
				onSuccess:function (transport) {
					var resultJson = eval('(' + transport.responseText + ')');
					for (var i = 0; i < elementsByClassName.length; i++) {
						var element = elementsByClassName[i];
						var testName=element.getAttribute("testName");
						var testJson=resultJson[testName];

						if(testJson != undefined && testJson!=""){
							element.value=testJson.status;
						}
					}

				},
				onFailure:function () {
					console.log('Change User status went wrong...');
				}
			});
		}


    }
}

acceptAll=function(){
	var user=document.getElementById("userName").value;
	if(user !=""){
		var testNames=[];
		var badges= $$('#BlameActionBadge');
		badges.each(function (it) {
			var selectValue=it.down('select').value;
			if(selectValue==user){
				var statusSpan = it.down('span.status');
				var status=statusSpan.down('span');
				var statusValue=status.innerText;
				if(statusValue!='Accepted' && statusValue!='Done'){
					testNames.push(it.getAttribute("testName"));
				}
			}
		});
		if (testNames.length > 0) {
			var url = document.getElementById("testUrl").value;
			new Ajax.Request(url + "/blame/bulkBlame", {
				method:'post',
				parameters:{userID:user, testNames:testNames},
				onSuccess:function (transport) {
					var resultJson = eval('(' + transport.responseText + ')');
					badges.each(function (it) {
						var testNameAttr = it.getAttribute("testName");
						var testJson=resultJson[testNameAttr];
						if(testJson != undefined && testJson!=""){
							var statusSpan = it.down("span.status");
							reloadStatusSpan(statusSpan, testJson.status, testJson.isYou);
						}
					});
				},
				onFailure:function () {
					console.log('BulkBlame went wrong...');
				}
			});
		}
	}
};

doneAll=function(){
	var user=document.getElementById("userName").value;
	var testNames=[];
	if(user !=""){
		var badges = $$('#BlameActionBadge');
		badges.each(function (it) {
			var selectValue=it.down('select').value;
			if(selectValue == user){
				var statusSpan = it.down('span.status');
				var status=statusSpan.down('span');
				var statusValue=status.innerText;
				if(statusValue=='Accepted'){
					testNames.push(it.getAttribute("testName"));
				}
			}
		});
		if (testNames.length > 0) {
			var url = document.getElementById("testUrl").value;
			new Ajax.Request(url + "/blame/bulkDone", {
				method:'post',
				parameters:{userID:user, testNames:testNames},
				onSuccess:function (transport) {
					var resultJson = eval('(' + transport.responseText + ')');
					badges.each(function (it) {
						var testNameAttr = it.getAttribute("testName");
						var testJson=resultJson[testNameAttr];
						if(testJson != undefined && testJson!=""){
							var statusSpan = it.down("span.status");
							reloadStatusSpan(statusSpan, testJson.status, testJson.isYou);
						}
					});
				},
				onFailure:function () {
					console.log('BulkBlame went wrong...');
				}
			});
		}
	}
};

acceptWithAge=function(){
	var user=document.getElementById("userName").value;
	if(user !=""){
		var ageInput=document.getElementById("ageField").value;
		var testNames=[];
		var badges = $$('#BlameActionBadge');
		badges.each(function(it){
			var testRow=it.up('.pane').up('tr').down(".pane",2);
			var testAge=testRow.innerText;
			if(testAge==ageInput){
				var select = it.down('select');
				select.value=user;
				testNames.push(it.getAttribute("testName"));
			}
		});
		if (testNames.length > 0) {
			var url = document.getElementById("testUrl").value;
			new Ajax.Request(url + "/blame/bulkBlame", {
				method:'post',
				parameters:{userID:user, testNames:testNames},
				onSuccess:function (transport) {
					var resultJson = eval('(' + transport.responseText + ')');
					badges.each(function (it) {
						var testNameAttr = it.getAttribute("testName");
						var testJson=resultJson[testNameAttr];
						if(testJson != undefined && testJson!=""){
							var statusSpan = $(it).down("span.status");
							reloadStatusSpan(statusSpan, testJson.status, testJson.isYou);
						}
					});
				},
				onFailure:function () {
					console.log('BulkBlame went wrong...');
				}
			});
		}

	}
};

Event.observe(window,"load",function(){
    var userName = document.getElementById("userName");
    if(userName != null) {
        if(document.getElementsByClassName("claimActions").length<1){
            var user=userName.value;
            if(user !=""){
                var claimActionsDiv = document.createElement('div');
                claimActionsDiv.className="claimActions";
                var acceptAllInput=document.createElement('input');
                acceptAllInput.type="button";
                acceptAllInput.value="Accept All Assigned to Me";
                acceptAllInput.onclick=acceptAll;

                var acceptAllWithAgeInput=document.createElement('input');
                acceptAllWithAgeInput.type="button";
                acceptAllWithAgeInput.value="Accept All with Age";
                acceptAllWithAgeInput.onclick=acceptWithAge;
                var acceptAllWithAgeInputTextEntry=document.createElement('input');
                acceptAllWithAgeInputTextEntry.id="ageField";
                acceptAllWithAgeInputTextEntry.type="text";
                acceptAllWithAgeInputTextEntry.value="1";
                acceptAllWithAgeInputTextEntry.title="Accept tests with age \"x\"";

                var doneAllInput=document.createElement('input');
                doneAllInput.type="button";
                doneAllInput.value="Done with All";
                doneAllInput.onclick=doneAll;

                claimActionsDiv.appendChild(doneAllInput);
                claimActionsDiv.appendChild(acceptAllInput);
                claimActionsDiv.appendChild(acceptAllWithAgeInput);
                claimActionsDiv.appendChild(acceptAllWithAgeInputTextEntry);

                var mainPanel=$('main-panel');
                var targetElement=$$('#main-panel h2')[0];
                mainPanel.insertBefore(claimActionsDiv,targetElement);
            }
        }
    }
});
