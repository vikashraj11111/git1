$(document).ready(function() {

	$(document).on("click", ".skip", function(e){
		e.preventDefault();
		location.reload();
	})
	
	$(document).on("click", ".add-email", function(e){
		e.preventDefault();
		let email = $("#email").val();
		$.ajax({
			url: "/user/addEmail",
			type: "POST",
			data: JSON.stringify({
				email : email
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$(".add-email").addClass("disabled");
			},
			success: function(data){
				if(data == 'EXISTS')
					showToast("error", "Email already registered.");
				else
					showToast("success", "Check your Email for Verification Link.");
			},
			error: function(jqXHR, exception){
				showToast("error", "Something wrong happened");
			},
			complete: function(){
				$(".add-email").removeClass("disabled");
			}
		})
	})
	
	$(document).on("submit", ".add-email-form", function(e){
		e.preventDefault();
		let username = $("#username").val();
		if(username.length == 0){
			showToast('error', 'Please Enter Valid Username');
			$("#username").focus();
			$("#username").addClass("is-invalid");
			return;
		}
		$.ajax({
			url: "/user/resendVerificationEmail",
			type: "POST",
			data: JSON.stringify({
				username : username,
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$(".submit").addClass("disabled");
				$("#username").attr("disabled", true);
			},
			success: function(data){
				if(data == 'success')
					showToast('success', 'Check your email for verification mail.');
				else
					showToast('error', data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
			},
			complete: function(){
				$(".submit").removeClass("disabled");
				$("#username").removeAttr("disabled");
			}
		})
	})
	
	// Send Password Reset Link
	$(document).on("click", ".send-reset-password-link", function(e){
		e.preventDefault();
		let email = $("#email").val();
		$.ajax({
			url: "/user/sendPasswordResetLink",
			type: "POST",
			data: JSON.stringify({
				email : email
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$(".send-reset-password-link").addClass("disabled");
			},
			success: function(data){
				if(data == 'SENT')
					showToast("success", "Check your Inbox/Junk Folder for verification link to reset your password.");
				else
					showToast("error", "This email is not associated with any tempsms user. Please check and try again.");
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
				showToast("error", "Something wrong happened");
			},
			complete: function(){
				$("#email").empty();
				$(".send-reset-password-link").removeClass("disabled");
			}
		})
	})
	
	$(document).on("keyup", "#new-password-repeat", function(event){
		if($(this).val().length > $("#new-password").val().length){
			$(".error-password").text("Password does not match");
		} else {
			$(".error-password").empty();
		}
	
		if($(this).val() != $("#new-password").val().substring(0, $(this).val().length)){
			$("#new-password-repeat").addClass("is-invalid");
		} else {
			$("#new-password-repeat").removeClass("is-invalid");
		}
			
	})
	
	$(document).on('keydown', '.reset-password-form input', function (event) {
	    if (event.which == 13) {
    	  	event.preventDefault();
	        var $this = $(event.target);
	        var index = parseFloat($this.attr('data-index'));
	        $('[data-index="' + (index + 1).toString() + '"]').focus();
	    }
	});
	
	$(document).on("submit", ".reset-password-form", function(evt){
		evt.preventDefault();
		let password = $("#new-password").val();
		let repeatPassword = $("#new-password-repeat").val();
		let token = $("#token").val();
		
		if(!password) {
			$("#new-password").addClass("is-invalid");
			showToast('error', 'Please enter new password');
			$("#new-password").focus();
			return;
		}
		
		if(password != repeatPassword) {
			$("#new-password-repeat").addClass("is-invalid");
			showToast('error', 'Passwords does not match');
			$("#new-password-repeat").focus();
			return;
		}
		
		$.ajax({
			url: "/user/resetPassword",
			type: "POST",
			data: JSON.stringify({
				password : password,
				token: token
			}),
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				$(".reset-password-form").find("button, input").prop('disabled', 'disabled');
			},
			success: function(data){
				if(!data) {
					showToast('error', 'Could not change password');
					return;
				}
				if(data == "SUCCESS"){
					showToast('success', 'Password changed successfully. You will now be redirected to login page.');
					setTimeout(function(){
						window.location.href = '/'
					}, 1000);
				} else
					showToast('error', data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
			}
		}).always(function(){
			$(".reset-password-form").find("button, input").removeAttr('disabled');
		})
	})
	
});