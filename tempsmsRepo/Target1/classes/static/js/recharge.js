let paymentStatusInterval;
let remaining;
let paymentDone = false;
$(document).ready(function() {

	$(document).on("click", ".upi-id, .paytm-number, .trx-deposit-id", function(){
        var node = event.target;
        selectText(node);
        document.execCommand("copy");
        setTimeout(clearSelection, 100);
        
        var copiedSuccess = $(event.target).closest("div").find("#copied-success");
    	$(copiedSuccess).fadeIn(800);
    	$(copiedSuccess).fadeOut(800);
	})
	
	/******************************************************/
	
	$(document).on("submit", ".recharge-amount-bharatpe", function(e){
		e.preventDefault();
		let amount = $("#amount").val();
		$.ajax({
			url: "/recharge/createOrder",
			type: "POST",
			data: JSON.stringify({
				amount : amount,
				paymentMethod: "BHARATPE"
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$(".submit").addClass("disabled");
			},
			success: function(data){
				$("#payment-amount").text(data.amount);
				$("#payment-upi-link").attr("data-href", data.qrLink);
				$("#payment-upi-link").removeClass("hidden");
				$("#payment-completed-header").addClass("hidden");
				$("#qr-code-img").attr("src", "data:image/png;base64," + data.qrCode);
				remaining = data.remainingTime;
				paymentDone = false;
				initiateCounter();
				paymentStatusInterval = setInterval(function(){
					checkPaymentStatus(data.orderId);
				}, 3000);
				showModal();
			},
			error: function(jqXHR, exception){
				doAlert("error", "Please Enter Different amount or try again later");
				$(".submit").removeClass("disabled");
			},
			async: true
		})
	})
	
	$(document).on("click", ".rk-modal-close", function(e){
		hideModal();
		resetTimers();
		clearInterval(paymentStatusInterval);
	})
	
	/******************************************************/
	
	$(document).on("submit", ".recharge-form-bharatpe", function(e){
		e.preventDefault();
		let utr = $("#utr").val();
		if(utr.length < 6){
			showToast('error', 'Please Enter Valid Transaction ID');
			$("#utr").focus();
			$("#utr").addClass("is-invalid");
			return;
		}
		$.ajax({
			url: "/recharge/bharatpe",
			type: "POST",
			data: JSON.stringify({
				utr : utr
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$("#utr").val("");
				$(".submit").addClass("disabled");
				$("#utr").attr("disabled", true);
				$(".submit").text('Please Wait');
				callPleaseWait();
				
			},
			success: function(data){
				return callBack(data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
				stopWaiting();
			},
			async: true
		})
	})
	
	/******************************************************/
	
	$(document).on("submit", ".recharge-form-paytm", function(e){
		e.preventDefault();
		let utr = $("#utr").val();
		if(utr.length < 6){
			showToast('error', 'Please Enter Valid Transaction ID');
			$("#utr").focus();
			$("#utr").addClass("is-invalid");
			return;
		}
		$.ajax({
			url: "/recharge/paytm",
			type: "POST",
			data: JSON.stringify({
				utr : utr
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$("#utr").val("");
				$(".submit").addClass("disabled");
				$("#utr").attr("disabled", true);
				$(".submit").text('Please Wait');
				callPleaseWait();
				
			},
			success: function(data){
				return callBack(data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
				stopWaiting();
			},
			async: true
		})
	})
	
	/******************************************************/
	
	$(document).on("click", "#currency", function(){
		if($(this).is(":checked")){
			$("#payment_amount_trx").attr("data-cur", "INR");
			$("#payment_amount_trx").attr("placeholder", "Enter Recharge Amount in INR");
			$("#trx-note").hide();
		} else {
			$("#payment_amount_trx").attr("data-cur", "TRX");
			$("#payment_amount_trx").attr("placeholder", "Enter Recharge Amount in TRX");
			$("#trx-note").show();
		}
	})
	
	$(document).on("submit", ".recharge-form-crypto", function(e){
		e.preventDefault();
		let amount = $("#payment_amount_trx").val();
		let currency = $("#payment_amount_trx").attr("data-cur");
		
		if(currency == 'INR' && amount < 250){
			showToast('error', 'Minimum Amount is Rs. 250');
			$("#payment_amount_trx").focus();
			$("#payment_amount_trx").addClass("is-invalid");
			return;
		} else if(currency == 'TRX' && amount < 15){
			showToast('error', 'Minimum deposit amount is 15 TRX');
			$("#payment_amount_trx").focus();
			$("#payment_amount_trx").addClass("is-invalid");
			return;
		}
		$.ajax({
			url: "/recharge/createPayment",
			type: "POST",
			data: JSON.stringify({
				amount : amount,
				currency: currency
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$("#payment_amount_trx").val("");
				$(".submit").addClass("disabled");
				$("#payment_amount_trx").attr("disabled", true);
				$(".submit").text('Please Wait');
				callPleaseWait();
				
			},
			success: function(data){
				return processResponse(data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
				stopWaiting();
			},
			async: true
		})
	})
	
	/*****************************************************/
	
	$(document).on("click", ".recharge-help", function(){
		window.open('/public/tg/admin/1', '_blank');
	})
	
	$(document).on("click", ".recharge-options > button, .recharge-options > input", function(){
		$(".recharge-options button").each(function(){
			$(this).removeClass("recharge-option-selected");
		})
		$(this).toggleClass("recharge-option-selected");
		let amt = $(this).attr("data-amount");
		let cashback = amt * 0.1;
		$(".proceed-payment").show();
	})
	
})
	
function callBack(data) {
	stopWaiting();
	
	if(data == "ALREADY_USED")
		showToast("error", "This UTR/TxId has already been used. Please contact admin if recharge not received.")
	else if(data == "NOT_FOUND")
		showToast("error", "Transaction ID not found")
	else if(data == "MORE_THAN_ONE_DAY")
		showToast("error", "Transaction is done more than 1 day before. Contact Admin for manual verification.")
	else if(data == "MORE_THAN_500")
		showToast("error", "Transaction amount is more than Rs 500. Contact Admin for manual verification.")
	else if(data == "FAILED")
		showToast("error", "Failed")
	else if(data == "BEFORE_16_JULY")
		showToast("error", "Unable to Process. Contact Admin")
	else if(data == "BEFORE_19_OCT")
		showToast("error", "Unable to Process. Contact Admin")
	else if(data == "CONTACT_ADMIN")
		showToast("error", "Please Retry or Contact Admin")
	else {
		showToast("success", "Recharge Successful! Updated Balance is " + data)
		refreshBalance()
	}
	return;
}

function processResponse(data) {
	stopWaiting();
	
	$(".crypto-details").removeClass("hidden");
	$(".trx-deposit-id").text(data.pay_address);
	$("#payment_id").val(data.payment_id);
	$(".trx-deposit-amount").text(data.pay_amount);
	
	return;
}

function stopWaiting(){
	$(".submit").removeClass("disabled");
	$("#payment_amount_trx").removeAttr("disabled");
	$("#utr").removeAttr("disabled");
	clearInterval(pleaseWait);
	$(".submit").text('Submit');
}

function callPleaseWait(){
	let count = 0;
	pleaseWait = setInterval(function(){
		if(count == 3) {
			count = 0;
			$(".submit").text('Please Wait');
			return;
		}
		count++;
		$(".submit").append(' .');
	}, 800);
}

function showModal() {
	$(".rk-modal").show();
}

function hideModal() {
	$(".rk-modal").hide();
}

function resetTimers(){
	remaining = 0;
}

function initiateCounter(){
	if(paymentDone){
		return;
	}
	if(remaining <= 0){
		$("#status").text("Order Expired");
		$("#timer").text("0:00");
		$(".rk-modal-footer").addClass("col-red");
		$("#qr-code-img").attr("src", "/images/failed.png");
		$("#payment-status-body").text("Payment Failed");
		$("#payment-status-body").toggleClass("hidden failed");
		$("#payment-upi-link").addClass("hidden");
		return;
	}
	let minutes = Math.floor(remaining/60);
	let seconds = remaining % 60;
	$("#timer").text(minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
	remaining--;
	setTimeout(function(){
		initiateCounter();
	}, 1000);
}

function checkPaymentStatus(orderId){
	if(remaining < 1) {
		clearInterval(paymentStatusInterval);
		return;
	}
	
	$.ajax({
		url: "/recharge/check-status",
		type: "POST",
		data: JSON.stringify({
			orderId : orderId,
			paymentMethod: "BHARATPE"
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			if(data.isSuccess){
				$("#qr-code-img").attr("src", "/images/payment-done.png");
				$("#payment-status-body").text("Payment Completed");
				$("#payment-status-body").toggleClass("hidden success");
				$("#payment-upi-link").addClass("hidden");
				$("#payment-amount-header").addClass("hidden");
				$("#payment-completed-header").removeClass("hidden");
				paymentDone = true;
				refreshBalance();
				clearInterval(paymentStatusInterval);
			} else {
				remaining = data.remainingTime;
			}
			
		},
		error: function(jqXHR, exception){
			console.log('Error occurred ' + exception);
		},
		async: true
	})
	
}
