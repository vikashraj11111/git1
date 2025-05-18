$(document).ready(function(){
	
	$(document).on("keyup", ".search-users-input", function(){
		let filter = $(".search-input").val().toUpperCase();
		
		searchUser(filter, function(rtnMsg){
			if(rtnMsg != null) {
				$("tbody").empty();
				$(rtnMsg).each(function(){
					let row = constructUsersRow(this.username, this.balance, this.insertTimestamp.split('T')[0], this.enabled);
					$("tbody").append(row);
				})
			}
		})
		
	})
	
	$(document).on("keyup", ".search-orders", function(){
		let filter = $(".search-input").val().toUpperCase();
		
		searchOrders(filter, function(rtnMsg){
			if(rtnMsg != null) {
				$("tbody").empty();
				$(rtnMsg).each(function(){
					let row = constructOrdersRow(this);
					$("tbody").append(row);
				})
			}
		})
		
	})
	
	$(document).on("keyup", ".search-input:not(.search-users-input, .search-orders)", function(){
		let filter = $(".search-input").val().toUpperCase();
		$(".table tbody tr").each(function(){
			let textValue = $(this).find("td:first").find("span").text().toUpperCase();
			if(textValue.indexOf(filter) == -1)
				$(this).hide();
			else
				$(this).show();
	  	});
	})
	
	$(document).on("click", ".select-box-admin .option", function() {
		let selectBox = $(this).closest(".select-box");
		$(selectBox).find(".selected").text($(this).text().trim());
		$(selectBox).find(".selected").attr("data-value", $(this).find("div").attr("id"));
		$(selectBox).find(".options-container").removeClass("active");
	})
	
	$(document).on("click", ".select-box-admin .selected", function(){
		const optionContainer = $(this).prev();
		$(".options-container").not(optionContainer).removeClass("active");
		optionContainer.toggleClass("active");
		if(optionContainer.hasClass("active")) {
			$(this).next().find("input").focus();
			$(this).next().find("input").val('');
			filterListAdmin('', this);
		}
	})
	
	$(document).on("keyup", ".select-box-admin input", function(e){
		filterListAdmin(e.target.value, this);
	})
	
	$(document).click(function(e){
		if(!$(e.target).closest(".select-box").length){
			$(".options-container").removeClass("active");
		}
	})
	
	$(document).on('click', '.otp-history-username', function(){
		let username = $(this).text();
		loadUrl("/admin/otpHistory/" + username);
	})
	
	$(document).on('click', '.otp-history-user span', function(evt){
		let username = $(this).closest("tr").find(".otp-history-username").text();
		loadUrl("/admin/otpHistory/" + username);
	})
	
	$(document).on("submit", "#manualRechargeForm", function(evt) {
		evt.preventDefault();
		let utr = $("#utr").val();
		let username = $("#username").val();
		let paymentMode = $("#payment-mode").find(":selected").val();
		$(".submit").attr('disabled', true);
		clearForm();
		$("#payment-mode").prop("selectedIndex", 0);
		doManualRecharge(utr, username, paymentMode, function(data) {
			if(data == "NOT_EXIST") {
				showToast('error', 'User does not exist')
			} else if(data == "ALREADY_USED") {
				showToast('error', 'Recharge Already Done');
			} else if(data == "BEFORE_16_JULY") {
				showToast('error', 'Payment done Before 16th July');
			} else if(data == "NOT_FOUND") {
				showToast('error', 'Transaction not found with this UTR');
			} else if(data == "FAILED") {
				showToast('error', 'Transcation was failed');
			} else if(data == "ERROR") {
				showToast('error', 'Some Issue Occurred');
			} else {
				showToast('success', 'Updated Balance is : ' + data);
			}
			
			$(".submit").removeAttr('disabled')
		});
	})
	
	$(document).on('click', '.disable-server', function(){
		let server = $(this).closest("tr").find(".server-name").text();
		disableServer(server, function(rtnMsg){
			if(rtnMsg == 'success') {
				loadUrl("/admin/serverList");
			} else {
				alert('failure');
			}
		});
	})
	
	$(document).on('click', '.enable-server', function(){
		let server = $(this).closest("tr").find(".server-name").text();
		enableServer(server, function(rtnMsg){
			if(rtnMsg == 'success') {
				loadUrl("/admin/serverList");
			} else {
				alert('failure');
			}
		});
	})
	
	$(document).on('click', '#restart-app button', function(){
		let service = $(this).closest("tr").find(".server-name").text();
		let id = $(this).attr('id');
		restartApp(id, function(rtnMsg){
			if(rtnMsg == 'SUCCESS') {
				showToast('success', 'Restarted ' + service);
			} else {
				showToast('error', 'Some Error Ocurred');
			}
		});
	})
	
	/*******************************************************/
	
	$(document).on("submit", "#retrieve-order-by-utr-form", function(evt) {
		evt.preventDefault();
		$(".message").addClass("hidden");
		emptyOrderDetails();
		let utr = $("#utr").val();
		if(!utr) {
			showToast('error', 'Enter transaction id');
			return;
		}
		$(".submit").attr('disabled', true);
		
		getOrderByUtr(utr, function(order) {
			if(!order) {
				showToast('error', 'This Transaction id does not exist')
			} else {
				$("#order-txid").text(order.paymentId);
				$("#order-amount").text(order.amount);
				$("#order-username").text(order.username);
				$("#order-status").text(order.status);
				$("#order-created-time").text(order.createTime);
				$("#order-payment-method").text(order.paymentMethod);
				$("#order-details").show();
			}
			
			$(".submit").removeAttr('disabled')
			
		});
	})
	
	/********************* A N N O U N C E M E N T ****************************/
	$(document).on("click", ".delete-announcement", function(){
		let parent = $(this).parent();
		let id = $(parent).attr("id");
		deleteAnnouncement(id, function(data){
			if(data == "ERROR") {
				showToast('error', 'Some Error Occurred')
			} else {
				$(parent).fadeOut("slow", function (){
					$(parent).remove();
				});
			}
		})
	})
	
	$(document).on("submit", "#add-announcement", function(evt) {
		evt.preventDefault();
		let announcement = $("#announcement").val();
		$(".submit").attr('disabled', true);
		
		addAnnouncement(announcement, function(data) {
			if(data == "ERROR") {
				showToast('error', 'Some Error Occurred')
			} else {
				$(".list-group").append('<li class="list-group-item bg-dark text-white bg-gradient" id="' 
						+ data + '">' + announcement + '<span class="close delete-announcement">x</span></li>')
			}
			
			$(".submit").removeAttr('disabled');
		});
	})
	
})

const filterListAdmin = (searchTerm, e) => {
	searchTerm = searchTerm.toLowerCase();
	$(e).closest(".select-box-admin").find(".option").each(function(){
		if($(this).first().text().toLowerCase().indexOf(searchTerm) != -1) {
			$(this).show();
		} else {
			$(this).hide();
		}
	})
}

/************************** SEARCH USERS ***********************************/

function searchUser(keyword, callBack){
	$.ajax({
		url: "/admin/searchUser",
		type: "POST",
		data: JSON.stringify({
			keyword : keyword
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'error';
			return callBack(msg);
		}
	})
}

function constructUsersRow(username, balance, joiningDate, enabled) {
	let row = '<tr><td><span class="otp-history-username">' + username + '</span></td>'
				+ '<td><span>' + balance + '</span></td>'
				+ '<td><span>' + joiningDate + '</span></td>'
				/*+ '<td><span>' + enabled + '</span></td><tr>' */
				+ '<td><a href="/admin/rechargeHistory/' + username + '" class="btn btn-sm btn-primary rec-his-btn">View</a></td>'
	
	return row;
}

/************************** SEARCH ORDERS ***********************************/

function searchOrders(keyword, callBack){
	$.ajax({
		url: "/admin/searchOrders",
		type: "POST",
		data: JSON.stringify({
			keyword : keyword
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'error';
			return callBack(msg);
		}
	})
}


function constructOrdersRow(order) {
	let row = '<tr>'
				+ '<td><span>' + order.paymentId + '</span></td>'
				+ '<td><span>' + order.amount + '</span></td>'
				+ '<td><span>' + order.paymentMethod + '</span></td>'
				+ '<td><span>' + order.username + '</span></td>'
				+ '<td><span>' + order.createTime.split('T')[0] + ' | ' + order.createTime.split('T')[1] + '</span></td>'
				+ '<td><span>' + order.status + '</span></td>'
	return row;
}


/************************** ADD BALANCE ***********************************/

function addBalance(balance, username, callBack){
	$.ajax({
		url: "/admin/updateUser",
		type: "PUT",
		data: JSON.stringify({
			username : username,
			balance : balance
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'error';
			return callBack(msg);
		}
	})
}


/************************** DEDUCT BALANCE ***********************************/

function deductBalance(balance, username, callBack) {
	$.ajax({
		url: "/admin/deductUserBalance",
		type: "PUT",
		data: JSON.stringify({
			username : username,
			balance : balance
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'error';
			return callBack(msg);
		}
	})
}


/************************** Manual Recharge ***********************************/

function doManualRecharge(utr, username, paymentMode, callBack) {
	$.ajax({
		url: "/admin/manualRecharge",
		type: "POST",
		data: JSON.stringify({
			utr : utr,
			username : username,
			paymentMode: paymentMode
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}

/************************** Get OrderDetails By UTR ***********************************/

function getOrderByUtr(utr, callBack) {
	$.ajax({
		url: "/admin/getOrderByUtr",
		type: "POST",
		data: JSON.stringify({
			utr : utr
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}

function emptyOrderDetails(){
	$("#order-details").find("span").empty();
	$("#order-details").hide();
}

/****************************** CLEAR FORM **********************************/

function clearForm() {
	$(':input','form')
	  .not(':button, :submit, :reset, :hidden')
	  .val('')
	  .prop('checked', false)
	  .prop('selected', false);
}


/************************** ADD SERVICE ***********************************/

function addService(server, serviceCode, serviceName, servicePrice, serviceId, callBack){
	$.ajax({
		url: "/admin/addService",
		type: "POST",
		data: JSON.stringify({
			server : server,
			serviceCode : serviceCode,
			serviceName : serviceName,
			price : servicePrice,
			serviceId : serviceId
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'error';
			return callBack(msg);
		}
	})
}

/************************** DISABLE SERVER ***********************************/
function disableServer(server, callBack) {
	$.ajax({
		url: "/admin/disableServer",
		type: "POST",
		data: JSON.stringify({
			server : server
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}


/************************** ENABLE SERVER ***********************************/
function enableServer(server, callBack) {
	$.ajax({
		url: "/admin/enableServer",
		type: "POST",
		data: JSON.stringify({
			server : server
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}

/************************** BACKUP DOWNLOAD ***********************************/

$(document).on("click", ".download-service button", function() {
	let id = $(this).attr("id");
	console.log(id);
	$.ajax({
		url: "/admin/createBackup",
		type: "POST",
		data: JSON.stringify({
			service: id
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			if(id != 'everything')
				window.open(data, "_blank");
			else
				alert("All backup files are creating. Check you mail for files.")
		},
		error: function(jqXHR, exception){
			alert('error occurred');
		}
	})
	
})

/************************** RESTORE BACKUP ***********************************/

$(document).on("click", "button.restore-backup-file", function() {
	let file = $("#backup-file");
	
	console.log(file);
	
	let file2 = $(file)[0].files[0];
    let upload = new Upload(file2);
    // execute upload
    upload.doUpload();
	
})


var Upload = function (file) {
    this.file = file;
};

Upload.prototype.getType = function() {
    return this.file.type;
};
Upload.prototype.getSize = function() {
    return this.file.size;
};
Upload.prototype.getName = function() {
    return this.file.name;
};
Upload.prototype.doUpload = function () {
    var that = this;
    var formData = new FormData();

    // add assoc key values, this will be posts values
    formData.append("file", this.file, this.getName());
    formData.append("upload_file", true);

    $.ajax({
        type: "POST",
        url: "/admin/restoreBackup",
        beforeSend: function() {
        	$("#progress-wrp").removeClass("hidden");
        },
        xhr: function () {
            var myXhr = $.ajaxSettings.xhr();
            if (myXhr.upload) {
                myXhr.upload.addEventListener('progress', that.progressHandling, false);
            }
            return myXhr;
        },
        success: function (data) {
            alert(data);
        },
        error: function (error) {
            // handle error
        },
        async: true,
        data: formData,
        cache: false,
        contentType: false,
        processData: false,
        timeout: 60000
    });
};

Upload.prototype.progressHandling = function (event) {
    var percent = 0;
    var position = event.loaded || event.position;
    var total = event.total;
    var progress_bar_id = "#progress-wrp";
    if (event.lengthComputable) {
        percent = Math.ceil(position / total * 100);
    }
    // update progressbars classes so it fits your code
    $(progress_bar_id + " .progress-bar").css("width", +percent + "%");
    $(progress_bar_id + " .status").text(percent + "%");
};


/************************** VIEW/DOWNLOAD LOG FILE ***********************************/

$(document).on("click", ".view-log", function() {
	let id = $(this).attr("id");
	window.open("/admin/viewLog/" + id, "_blank");
})

$(document).on("click", ".download-log", function() {
	let id = $(this).attr("id");
	window.open("/admin/downloadLog/" + id, "_blank");
})


/************************** RESTART APPLICATION ***********************************/

function restartApp(id, callBack) {
	$.ajax({
		url: "/admin/restartApp",
		type: "POST",
		data: JSON.stringify({
			appId : id
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
		
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}
	
/************************** A N N O U N C E M E N T ****************************/

function addAnnouncementDeleteButton(){
	$(".announcement-list li").each(function(){
		let text = '<span class="close delete-announcement">x</span>';
		$(this).append(text);
	})
}
	
function deleteAnnouncement(id, callBack) {
	$.ajax({
		url: "/admin/deleteAnnouncement",
		type: "POST",
		data: JSON.stringify({
			id : id
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}

function addAnnouncement(announcement, callBack) {
	$.ajax({
		url: "/admin/addAnnouncement",
		type: "POST",
		data: JSON.stringify({
			msg : announcement
		}),
		contentType: "application/json; charset=utf-8",
		success: function(data){
			
			return callBack(data);
		},
		error: function(jqXHR, exception){
			let msg = 'ERROR';
			return callBack(msg);
		}
	})
}
