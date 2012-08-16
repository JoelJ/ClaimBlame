if(!window.claim) {
	window.claim = function (url, select) {
		var user = select[select.selectedIndex].value;

		new Ajax.Request(url+'/blame/blame', {
			method: 'post',
			parameters: {userID: user},
			onSuccess: function(transport) {
				var resultJson = eval('(' + transport.responseText + ')');
				var statusSpan = $(select).up("#BlameActionBadge").down("span.status")
				reloadStatusSpan(statusSpan, resultJson.status, resultJson.isYou);
			},
			onFailure: function() {
				console.log('Blame went wrong...');
			}
		});
	};
}

if(!window.changeStatus) {
	window.changeStatus = function(url, button) {
		new Ajax.Request(url+'/blame/status', {
			method: 'post',
			parameters: {status: button.name},
			onSuccess: function(transport) {
				var resultJson = eval('(' + transport.responseText + ')');
				var statusSpan = $(button).up("#BlameActionBadge").down("span.status")
				reloadStatusSpan(statusSpan, resultJson.status, resultJson.isYou)
			},
			onFailure: function() {
				console.log('Change status went wrong...');
			}
		});
	}
}

if(!window.changeUserPageStatus) {
    window.changeUserPageStatus = function(url, select) {
        new Ajax.Request(url+'/blame/status', {
            method: 'post',
            parameters: {status: select.value},
            onSuccess: function(transport) {
            },
            onFailure: function() {
                console.log('Change User status went wrong...');
            }
        });
    }
}

function reloadStatusSpan(statusSpan, newStatus, isCulprit) {
	statusSpan.setAttribute('class', "status " + newStatus + (isCulprit ? " isCulprit" : ""));
	statusSpan.down('span').innerText = newStatus;

	var button = statusSpan.down('input');
	if(newStatus == 'NotAccepted') {
		button.value = 'Accept';
		button.name = 'Accepted';
	} else if(newStatus == 'Accepted') {
		button.value = 'Done';
		button.name = 'Done';
	}
}
if(!window.acceptAll){
    window.acceptAll= function(){
        var elementsByClassName = document.getElementsByClassName("status");

        for(var i=0;i<elementsByClassName.length;i++){
            var value = elementsByClassName[i].options[elementsByClassName[i].selectedIndex].value;
            if(value!='Accepted' && value!='Done'){
                elementsByClassName[i].value='Accepted';
                elementsByClassName[i].onchange();
            }
        }
    }
}

if(!window.doneAll){
    window.doneAll= function(){
        var elementsByClassName = document.getElementsByClassName("status");

        for(var i=0;i<elementsByClassName.length;i++){
            var value = elementsByClassName[i].options[elementsByClassName[i].selectedIndex].value;
            if(value=='Accepted'){
                elementsByClassName[i].value='Done';
                elementsByClassName[i].onchange();
            }
        }
    }
}