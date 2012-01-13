var currentPath = new Array();
var formatPath = "";
var PREVIOUS_KEY = "<b>previous</b>";
var keywords = new Array();

/*  Utility functions */
function formatSynset(synset) {
	var synsetSplit = synset.split(".");
		  return synsetSplit[0];
}


function formatCurrentPath() {
	var resultString = "";
	for (var i = 0; i < currentPath.length; ++i) {
			  if (i == currentPath.length - 1) {
				  resultString += "<b>" + formatSynset(currentPath[i]) + "</b>";
				  break;
			  } 
			  resultString += formatSynset(currentPath[i]) + "<b> > </b>";
		  }
	
		  return resultString;
}

/*
  Functions dealing with the menu
*/
function clearFiles() {
	$("#files").html("");
}

function clearMenu() {
	$("#menu").html("");
	addMenuItem(PREVIOUS_KEY, "..");
	
}

function refreshMenu() {
	$("#menu-nav").html("");
	$("#menu").listnav();
}

function addMenuItem(name, link) {
	$("#menu").append("<li><a href='javascript:navigateTo(\"" + name + "\");'>" + formatSynset(name) + "</a></li>");
}

		function addFileItem(filepath) {
			//			alert("adding " +filepath + " with keywords = " + keywords.toString());
			try {
				$.get("autosummary.do",
				      { file:filepath,
						      selectedKeywords:keywords.toString() },
				      function (data) {
					      var summaryData = jQuery.parseJSON(data);
					      var fileHtml = "<div><br><span class=\"heading\">" +
						                   summaryData.filepath + "</span></br><span class=\"keywords\"><b>keywords: </b>" + 
            					         summaryData.selectedKeywords + "</span><br><span class=\"content\">" + summaryData.summary + "</span></br></div>"
						      $("#files").append(fileHtml);
					      
				      });
			} catch(e) {
				alert(e);
			}
			
		}



/*
  Functions dealing with interacting with the server
*/
function handleResponse(data) {
	var response = jQuery.parseJSON(data);
	for (i in response.children) {
		addMenuItem(response.children[i], "");
	}
	
	clearFiles();
	for (i in response.files) {
		addFileItem(response.files[i]);
	}
	
	keywords = [];
	for (i in response.keywords) {
		keywords.push(formatSynset(response.keywords[i]));
	}
	
	refreshMenu();
	
}

function test() {
	try {
		gotoPath("");
	} catch(e) {
		alert(e);
	}
}

function updatePathDisplay() {
	$("#path").html(formatCurrentPath());
    }

function gotoPath(path) {
	try {
		$.post("castanet.do",
		       { pathway:path },
		       function(data) {

			       clearMenu();
			       handleResponse(data);
		       });
	} catch(e) {
		alert(e);
	}
}


function navigateTo(next) {
	try {
		if (next == PREVIOUS_KEY) {
			currentPath.pop();
		} else {
  			  currentPath.push(next);
		}
		    
		updatePathDisplay();
		gotoPath(currentPath.toString());
	} catch(e) {
		alert(e);
	}
}

$(document).ready(function() {
		$("#menu").listnav();
		gotoPath("");
	});
