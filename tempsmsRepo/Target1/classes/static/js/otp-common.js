var isReady = false;
var app;
var count = 0;
var baseUrl = "/otp/";
var activeService;
var activeNumber;

$(document).ready(function() {

	$(document).on("click", ".number", copyNumber);

	$(document).on("click", "#changenumber", function(){
		let code = $(".code").text();
		let otpText = $(".otp").text();
		
		cancel(code, function(rtnMsg){
			if(rtnMsg == 'cancelled')
				setTimeout(getNumber, 1000);
			else if(rtnMsg == 'failed') {
				console.log('failed')
				showToast('error', 'Cancel after 2 minutes');
			}
		})
	})
	
	$(document).on("click", "#nextNumber", function(){
		setTimeout(getNumber, 1000);
	})
	
	$(document).on("click", "#cancelNumber", function(){
		let code = $(".code").text();
		let otpText = $(".otp").text();
		
		cancel(code, function(rtnMsg){
			if(rtnMsg == 'cancelled')
				showToast('success', 'Cancelled');
			else if(rtnMsg == 'failed') {
				showToast('error', 'Cancel after 2 minutes');
			}
				
		})
	})
	
	$(document).on("click", ".cancel-active-number", function(){
		cancelActiveNumber(function(rtnMsg){
			if(rtnMsg == 'cancelled') {
				$(".otpDiv").show();
				$(".messageDiv").hide();
				setTimeout(getNumber, 1000);
			}
		})
	})
	
	$(document).on("click", ".resend", function(){
		let code = $(".code").text();
		requestAnotherSMS(code);
	})
	
	$(document).on("click", ".go-back", function(){
		$("#otp-content").empty();
	})
	
	$(document).on("click", "#retry-get-number", function(){
		getNumber();
	})
	
});

function getNumber() {
	app = $("#app").val();
	if(!app)
		return;
	$.ajax({
		url: baseUrl + app + "/getnumber",
		type: "GET",
		beforeSend: function(){
			reset();
			$(".number").addClass("lds-ellipsis");
			for (let i = 0; i < 4; i++) {
			  	$(".number").append("<div></div>");
			}
		},
		success: function(data){
			if(!data) {
				$(".number").text("ERROR");
				return;
			}
			if(data == 'ALREADY_ACTIVE'){
				$(".otpDiv").hide();
				$(".message").html('Cancel any active number from <a href="/user/dashboard">dashboard</a> to get new number.');
				$(".message").show();
				$(".messageDiv").show();
				isReady = false;
				return;
			}
			if(data == 'NO_BALANCE'){
				$(".number").text(data);
				return;
			}
			if(data == 'ERROR') {
				$(".number").text("SERVICE NOT AVAILABLE RIGHT NOW");
				$("#retry-div").show();
				return;
			}
			
			if(data == 'NO_NUMBER') {
				$(".number").text("Number not available right now. Try again");
				$("#retry-div").show();
				return;
			}
			let d = data.split(':');
			if(d[2] == '0') {
				$(".number").text("Some Error Occurred");
				$("#retry-div").show();
				return;
			}
			$(".number").text(d[0]);
			$(".code").text(d[1]);
			refreshBalance();
			isReady = true;
			validateRegistration(function(rtnMsg){
				if(rtnMsg) {
					$("#resend-cancel-div").show();
				} else {
					setTimeout(function(){
						$("#resend-cancel-div").show();
					}, 5000);
				}
				getOtp();
			});
		},
		error: function(jqXHR, exception){
			console.log('ERROR in getNumber');
			$(".number").text("ERROR");
		},
		complete: function(){
			$(".number").removeClass("lds-ellipsis");
		}
	})
}

function getOtp() {
	if(isReady && !isRunning) {
		isRunning = true;
		let code = $(".code").text();
		if(code !== undefined && code != '' && code != null) {
			$.ajax({
				url: baseUrl + "getotp/" + code,
				type: "GET",
				beforeSend: function(request) {
			    	request.setRequestHeader("app_name", 'tempsms');
			  	},
				success: function(data){
					if(data == 'CANCEL') {
						isReady = false;
						$(".number").text('CANCELLED...');
						return;
					}
					
					if(data == 'CODE_NOT_FOUND'){
						isReady = false;
						$(".otp").empty();
						return;
					}
					
					if(data == 'ERROR2'){
						return;
					}
					
					if(data == 'WAITING'){
						$(".otpDiv").toggleClass('box-shadow-1 box-shadow-2');
						$(".otp").text('waiting');
						return;
					}
					
					if(data.includes('<!DOCTYPE html>')) {
						window.location.reload();
					}
					
					$(".otpDiv").toggleClass('box-shadow-1 box-shadow-2');
					$(".otp").text(data);
				},
				error: function(jqXHR, exception){
					console.log('Error in getOtp ' + exception);
					setTimeout(getOtp, 1000);
					isRunning = false;
				}
			}).always(function(){
				setTimeout(getOtp, 1000);
				isRunning = false;
			})
		}
	} else if(!isRunning) {
		setTimeout(getOtp, 1000);
		isRunning = false;
	}
}


function cancel(code, callback) {
	$.ajax({
		url: baseUrl + "cancel/" + code,
		type: "GET",
		success: function(data){
			if(!data){
				return callback('failed')
			}
			reset();
			$(".number").text('Cancelled');
			refreshBalance();
			return callback('cancelled');
		},
		error: function(jqXHR, exception){
			console.log('ERROR in get balance');
			return callback('failed');
		}
	})

}

function cancelActiveNumber(callback){
	$.ajax({
		url: baseUrl + "cancelActive",
		type: "GET",
		success: function(data){
			refreshBalance();
			return callback('cancelled');
		},
		error: function(jqXHR, exception){
			console.log('ERROR in get balance');
			return callback('failed');
		}
	})
}

function requestAnotherSMS(code) {
	if(isReady) {
		if(code !== undefined && code != '' && code != null) {
			$.ajax({
				url: baseUrl + "resend/" + code,
				type: "GET",
				success: function(data){
					if(!data) {
						//isReady = false;
						//$(".number").text('Error Occurred');
						//alert('Resend works only after getting first otp')
						showToast('error', 'Resend works only after getting first otp');
						return;
					}
					isReady = true;
					console.log('waiting for new otp');
					getOtp();
					showToast('succcess', 'waiting for new otp now');
				},
				error: function(jqXHR, exception){
					isReady = false;
					console.log('Error in resendOtp ' + exception);
					showToast('error', 'Failure');
				}
			})
		}
	}
}


/****************************************************************/

function reset() {
	isReady = false;
	$(".code").empty();
	$(".otp").empty();
	$(".number").empty();
	$("#resend-cancel-div").hide();
	$("#retry-div").hide();
	$(".validate-registration").hide();
}


function validateRegistration(callback) {
	//$(".registration-status").text("Checking Registration Status");
	$(".registration-status").show();
	
	let isValidation = $("#checkRegistration").val();
	let appName = $("#appName").val().toLowerCase().replace(/ /g,'');
	let number = $(".number").text();
	if(isValidation == "true") {
		$.ajax({
			url: "/verify/" + appName + "/" + number,
			type: "GET",
			success: function(data){
				$(".validate-registration").show();
				if(data) {
					$(".registration-status").text("Number Already registered. You can cancel and get New Number");
					return callback(true);
				}
				$(".registration-status").text("Not registered");
				return callback(false);
			},
			error: function(jqXHR, exception){
				console.log('Error in validateRegistration ' + exception);
				$(".validate-registration").hide();
			}
		})
	} else if (appName == 'whatsapp') {
		$(".validate-registration").show();
		$(".registration-status").empty();
		$(".registration-status").append("<button class='btn btn-outline-success btn-sm' onclick='openWhatsapp(" + number + ")'> Click here to check if number already registered or not </button>");
	} else if(appName == 'myntra') {
		$(".validate-registration").show();
		$(".registration-status").empty();
		
		$(".registration-status").append("<button class='btn btn-outline-success btn-sm' onclick='openMyntra(" + number + ")'> Click here to check if number already registered or not </button>");
	}
	return callback(false);
}

function openWhatsapp(number) {
	window.open("http://wa.me/+91" + number, "_blank");
}

function openMyntra(number) {
	let win = window.open("https://www.myntra.com", '1366002941508','width=500,height=200,left=375,top=330');
	setTimeout(function () { win.close();}, 500);
	window.open("https://www.myntra.com/gateway/v1/auth/recovery/options/" + number, "_blank");
}

function openUrl(url) {
	window.open(url, "_blank");
}
