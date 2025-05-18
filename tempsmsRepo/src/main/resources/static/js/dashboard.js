var isCancelled = false;
var rechargeUrl;

$(document).ready(function(){

	setRechargeUrl();

	$(document).on("click", ".cancel", function(){
		$(this).attr('disabled', true);
		let code = $(this).prev().val();
		let tr = $(this).closest("tr");
		let otpSpan = $(this).closest("tr").find(".otp").text();
		cancel(code, function(rtnMsg){
			isCancelled = true;
			$(tr).remove();
			location.reload();
		});
	})
	
	$(document).on("click", ".cancel-all-active", function(){
		cancelActiveNumber(function(rtnMsg){
			if(rtnMsg == 'cancelled') {
				location.reload();
			}
		})
	})
	
	$(document).on("click", ".recharge-site-open:not(.disabled)", function(){
		window.open(rechargeUrl, "_blank");
	})
	
})

function cancel(code, callback) {
	$.ajax({
		url: "/otp/cancel/" + code,
		type: "GET",
		success: function(data){
			
			return callback('success');
		},
		error: function(jqXHR, exception){
			return callback('error');
		}
	})

}

function setRechargeUrl(){

	$.ajax({
		url: "/recharge/getRechargeUrl",
		type: "GET",
		success: function (data){
			rechargeUrl = data;
		},
		error: function(jqXHR, exception){
			console.log('Error in getting recharge URL ' + exception);
		}
	})
}


function getOtpDashboard(code, appName, callBack) {
	let otp = "";
	let codeAvailable = true;
	if(!isCancelled || code !== undefined && code != '' && code != null) {
		$.ajax({
			url: "/otp/getotp/" + code,
			type: "GET",
			success: function(data){
				if(data == "CODE_NOT_FOUND" || data == "CANCEL" || "BAD_KEY") {
					otp = "Number expired";
					codeAvailable = false;
				}
				otp = data;
				return callBack(otp);
			},
			error: function(jqXHR, exception){
				console.log('Error in getOtp ' + exception);
			}
		})
	}
}

function requestAnotherSMS(code, callback) {
	if(code !== undefined && code != '' && code != null) {
		$.ajax({
			url: "/otp/resend/" + code,
			type: "GET",
			success: function(data){
				if(!data)
					return callback('failure');
					
				return callback('success');
			},
			error: function(jqXHR, exception){
				console.log('Error in resendOtp ' + exception);
				return callback('failure');
			}
		})
	}
}

function startCountDown(ele) {
	reduceTimeInterval = setInterval(function(){
		if(isCancelled)
			clearInterval(reduceTimeInterval);
		var t1 = $(ele).next().text();
		reduceTime(t1, ele);
	}, 1000);
}

function reduceTime(t1, ele) {
	if(t1 == 0) {
		isCancelled = true;
		return;
	}
	$(ele).next().text(t1 - 1);
}