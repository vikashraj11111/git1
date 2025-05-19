var getOtpInterval;
var reduceTimeInterval;
var pleaseWait;
var fetchBalanceInterval;

$(document).ready(function(){

	var dragitemDiv = document.querySelector(".widget");
	var dragItem = document.querySelector(".widget img");
    var container = document.querySelector("body");
    var active = false;
    var currentX;
    var currentY;
    var initialX;
    var initialY;
    var xOffset = 0;
    var yOffset = 0;
	
	container.addEventListener("touchstart", dragStart, false);
    container.addEventListener("touchend", dragEnd, false);
    container.addEventListener("touchmove", drag, false);

    container.addEventListener("mousedown", dragStart, false);
    container.addEventListener("mouseup", dragEnd, false);
    container.addEventListener("mousemove", drag, false);
    
    function dragStart(e) {
      if (e.type === "touchstart") {
        initialX = e.touches[0].clientX - xOffset;
        initialY = e.touches[0].clientY - yOffset;
      } else {
        initialX = e.clientX - xOffset;
        initialY = e.clientY - yOffset;
      }
      
      if (e.target === dragItem) {
        active = true;
      }
    }

    function dragEnd(e) {
      initialX = currentX;
      initialY = currentY;
      active = false;
    }

    function drag(e) {
    
      if (active) {
        if (e.type === "touchmove") {
          currentX = e.touches[0].clientX - initialX;
          currentY = e.touches[0].clientY - initialY;
        } else {
          currentX = e.clientX - initialX;
          currentY = e.clientY - initialY;
        }

        xOffset = currentX;
        yOffset = currentY;

        setTranslate(currentX, currentY, dragitemDiv);
      }
    }

    function setTranslate(xPos, yPos, el) {
      el.style.transform = "translate3d(" + xPos + "px, " + yPos + "px, 0)";
    }
	
	$(document).on("click", ".refresh", function(){
		location.reload();
	})
	
	$(document).on('keydown', "form", function (event) {
	    if (event.which == 13) {
    	  	event.preventDefault();
	        var $this = $(event.target);
	        var index = parseFloat($this.attr('data-index'));
	        $('[data-index="' + (index + 1).toString() + '"]').focus();
	    }
	});
	
	$(document).on('click', '#telegram-link', function(){
		openTelegram();
		openTelegram2();
	})
	
	$(document).on("click", ".link-window-open", function(){
		window.open($(this).attr("data-href"), '_blank');
	})
	
})

function openTelegram(){
	window.open('https://telegram.me/tempsmsofficial', '_blank');
}

function openTelegram2(){
	window.open('https://telegram.me/tempsmsofficial', '_blank');
}

function copyNumber() {
	var node = event.target;
    selectText(node);
	document.execCommand("copy");
	setTimeout(clearSelection, 100);
	var copiedSuccess = $(node).closest(".otpDiv").find("#copied-success");
	$(copiedSuccess).fadeIn(800);
	$(copiedSuccess).fadeOut(800);
}


function selectText(node){
	var doc = document;
	if (doc.body.createTextRange) { // ms
        var range = doc.body.createTextRange();
        range.moveToElementText(node);
        range.select();
    } else if (window.getSelection) {
        var selection = window.getSelection();
        var range = doc.createRange();
        range.selectNodeContents(node);
        selection.removeAllRanges();
        selection.addRange(range);
    }

}


function clearSelection() {
 	if (window.getSelection)
 		window.getSelection().removeAllRanges();
 	else if(document.selection)
 		document.selection.empty();
}


function refreshBalance(){

	$.ajax({
		url: "/user/balance",
		type: "GET",
		success: function(data){
			$(".balance").text(data);
		},
		error: function(jqXHR, exception){
			console.log('ERROR in get balance');
		}
	})
}

function doAlert(type, data){
	$(".message").removeClass("hidden");
	$(".message").removeClass("alert-success");
  	$(".message").removeClass("alert-danger");
	if(type == "error")
		$(".message").addClass("alert-danger");
	if(type == "success")
		$(".message").addClass("alert-success");
	$(".message").text(data);
	$(".message").fadeTo(2000, 500).slideUp(500, function() {
      $(".message").slideUp(500);
      $(".message").removeClass("alert-success");
      $(".message").removeClass("alert-danger");
    });
}

function doAlertFixed(type, data){
	$(".message").removeClass("hidden");
	$(".message").removeClass("alert-success");
  	$(".message").removeClass("alert-danger");
	if(type == "error")
		$(".message").addClass("alert-danger");
	if(type == "success")
		$(".message").addClass("alert-success");
	$(".message").text(data);
	$(".message").removeClass('hidden');
}

function clearForm(){
	$(':input','#userForm')
	  .not(':button, :submit, :reset, :hidden')
	  .val('')
	  .prop('checked', false)
	  .prop('selected', false);
}
