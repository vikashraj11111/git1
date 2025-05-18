$(document).ready(function() {

	$(document).on("click", "#show-password", function(){
		$(this).is(":checked") ? $("#password, #repeatPassword").attr("type", "text") : $("#password, #repeatPassword").attr("type", "password");
	})
	
	$(document).on("keyup", "#repeatPassword", function(event){
		if($(this).val().length > $("#password").val().length){
			$(".error-password").text("Password does not match");
		} else {
			$(".error-password").empty();
		}
	
		if($(this).val() != $("#password").val().substring(0, $(this).val().length)){
			$("#repeatPassword").addClass("is-invalid");
		} else {
			$("#repeatPassword").removeClass("is-invalid");
		}
			
	})
	
	
	$(document).on("click", ".register-btn", function(e){
		e.preventDefault();
		clearInvalid();
		let username = $("#username").val();
		let password = $("#password").val();
		
		let repeatPassword = $("#repeatPassword").val();
		
		if(username.length < 4) {
			showToast('error', 'Please Enter Valid Username');
			$("#username").focus();
			$("#username").addClass("is-invalid");
			return;
		}
		
		if(password.length == 0){
			showToast('error', 'Please Enter Password');
			$("#password").focus();
			$("#password").addClass("is-invalid");
			return;
		}
		
		if(password != repeatPassword) {
			$("#repeatPassword").addClass("is-invalid");
			showToast('error', 'Passwords does not match');
			$("#repeatPassword").focus();
			$("#repeatPassword").addClass("is-invalid");
			return;
		}
		
		doRegister(username, password);
	})
	
});


function doRegister(username, password) {
	$.ajax({
		url: "/registration",
		type: "POST",
		data: JSON.stringify({
			username : username,
			password : password
		}),
		dataType: "json",
		contentType: "application/json; charset=utf-8",
		beforeSend: function(){
			clearForm();
			$(".register-btn").addClass("disabled");
		},
		success: function(data){
			if(data == "Success"){
				showToast("success", "Successfully registered. Redirecting to Login Page now.");
				disableRegistrationForm();
				setTimeout(redirectToLoginPage, 500);
				return;
			}
			
			if(data == "Username already exists") {
				showToast("error", data);
				return;
			}
			
			if(data == "Invalid Username") {
				showToast("error", "Invalid Username. Username should be 4 to 20 characters long. Only Alphanumeric characters and underscores allowed.")
				return;
			}
			
			if(data == "Invalid Password") {
				showToast("error", "Invalid Password. Password should be 6 to 16 characters long. Must contain at least one letter and one number.")
				return;
			}
			var msg = "Invalid ";
			$(data).each(function(index, value){
				if(index == 0)
					msg = msg.concat(value);
				else
					msg = msg.concat(", " + value);
			})
			showToast("error", msg);
		},
		error: function(jqXHR, exception){
			console.log('Error occurred ' + exception);
		}
	}).always(function(data){
		$(".register-btn").removeClass("disabled");
	})
}

function clearInvalid(){
	$("form input").each(function(){
		$(this).removeClass("is-invalid");
	})
}

function redirectToLoginPage(){
	loadUrl("/user/addEmail");
}

function disableRegistrationForm(){
	$("#userForm :input").prop("disabled", true);
}