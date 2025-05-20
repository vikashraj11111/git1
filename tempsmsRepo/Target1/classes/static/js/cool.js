var originalPotion = false;
$(document).ready(function(){

	$.ajaxSetup({
        // Disable caching of AJAX responses
        cache: false,
        headers: {
	        'app_name' : 'tempsms'
	    }
    });
    
    $(document).on("click", "a.nav__link", function(e){
		$(".active-link").removeClass("active-link");
		$(this).addClass("active-link");
	})
    
	$(document).on("click", "a", function(e){
		if(e.target.nodeName === 'A'){
			e.preventDefault();
			let link = $(e.target).attr("href");
			if(!$(e.target).attr("disabled"))
				loadUrl(link);
		}
		else if($(e.target).parent().get(0).nodeName === 'A') {
			e.preventDefault();
			let link = $(e.target).closest("a").attr("href");
			if(!$(e.target).attr("disabled"))
				loadUrl(link);
		}
	})
	
    if (originalPotion === false) originalPotion = $(window).width() + $(window).height();
	
	$(document).on('focus blur', 'select, textarea, input', function(e){
	    var $obj = $(this);
	    var nowWithKeyboard = (e.type == 'focusin');
	    let os = getMobileOperatingSystem();
		if (os != "" && os != "ios") {
		    $('body').toggleClass('view-withKeyboard', nowWithKeyboard);
		    onKeyboardOnOff(nowWithKeyboard);
	    }
	});
	
	$(window).on('resize orientationchange', function(){
	    applyAfterResize();
	});
	
})

function loadUrl(link) {
	$('#overlay').show();
	clearIntervals();
	
	if(link.includes('/admin')) {
		$(".active-link").removeClass("active-link");
		$(".h-admin").addClass("active-link");
	} else if(link.includes('/dashboard')) {
		$(".active-link").removeClass("active-link");
		$(".h-dashboard").addClass("active-link");
	} else if(link.includes('/otp')) {
		$(".active-link").removeClass("active-link");
		$(".h-otp").addClass("active-link");
	} else if(link.includes('/register')) {
		$(".active-link").removeClass("active-link");
		$(".h-register").addClass("active-link");
	} else if(link.includes('/login')) {
		$(".active-link").removeClass("active-link");
		$(".h-login").addClass("active-link");
	} else if(link.includes('/user/profile')) {
		$(".active-link").removeClass("active-link");
		$(".h-profile").addClass("active-link");
	}
	
	if(link == '/')
		window.location.href = '/';
		
	if(link == '/loginRedirect'){
		loadUrl("/")
	}
	//$(".main-app").load(link, function(){
	//	$("#overlay").hide();
	//})
	if(link.includes('?')){
		link = link + "&loadUrl"
	} else {
		link = link + "?loadUrl"
	}
	$.ajax({
	  url: link,
	  headers : {app_name : "tempsms"}       
  	}).done(function(data) {
     	$('.main-app').html(data);
	 	$("#overlay").hide();
	}).fail(function(data) {
		loadUrl('/');
	});
}

function clearIntervals() {
	clearInterval(reduceTimeInterval);
	clearInterval(getOtpInterval);
	clearInterval(pleaseWait);
	clearInterval(fetchBalanceInterval);
}


/**
 * Determine the mobile operating system.
 * This function returns one of 'iOS', 'Android', 'Windows Phone', or 'unknown'.
 *
 * @returns {String}
 */
function getMobileOperatingSystem() {
    var userAgent = navigator.userAgent || navigator.vendor || window.opera;

      // Windows Phone must come first because its UA also contains "Android"
    if (/windows phone/i.test(userAgent)) {
        return "winphone";
    }

    if (/android/i.test(userAgent)) {
        return "android";
    }

    // iOS detection from: http://stackoverflow.com/a/9039885/177710
    if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
        return "ios";
    }

    return "";
}

function applyAfterResize() {
	let os = getMobileOperatingSystem();
    if (os != "" && os != "ios") {
        if (originalPotion !== false) {
            var wasWithKeyboard = $('body').hasClass('view-withKeyboard');
            var nowWithKeyboard = false;

                var diff = Math.abs(originalPotion - ($(window).width() + $(window).height()));
                if (diff > 100) nowWithKeyboard = true;

            $('body').toggleClass('view-withKeyboard', nowWithKeyboard);
            if (wasWithKeyboard != nowWithKeyboard) {
                onKeyboardOnOff(nowWithKeyboard);
            }
        }
    }
}

function onKeyboardOnOff(isOpen) {
    if (isOpen) {
        $(".nav__menu").addClass("nav-hidden");
    } else {
        $(".nav__menu").removeClass("nav-hidden");
    }
}

/************** TOAST ******************/

function showToast(status, text) {
	$("#snackbar").text(text);
	if(status == 'error') {
		$("#snackbar").addClass("error");
	} else {
		$("#snackbar").addClass("success");
	}
  	$("#snackbar").addClass("show");
  	setTimeout(hideToast, 3000);
}

function hideToast() {
	$("#snackbar").removeClass("show error success");
	$("#snackbar").text("");
}