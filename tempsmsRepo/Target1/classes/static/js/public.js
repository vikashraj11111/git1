$(document).ready(function() {

	$(document).on("click", ".contact-us-submit", function(evt){
		if (!$("form")[0].checkValidity()) {
			$("form")[0].reportValidity();
			return;
		}
		
		evt.preventDefault();
		
		let name = $("#name").val();
		let email = $("#emailAddress").val();
		let msg = $("#message").val();
		
		$.ajax({
			url: "/public/contactus",
			type: "POST",
			data: JSON.stringify({
				name : name,
				email : email,
				msg : msg
			}),
			beforeSend: function(){
				clearForm();
				$(".submit").addClass("disabled");
			},
			contentType: "application/json; charset=utf-8",
			success: function(data){
				if(data == 'ERROR') {
					showToast('error', 'Some error occurred');
					return;
				}
				
				showToast('success', data);
			},
			error: function(jqXHR, exception){
				console.log('Error occurred ' + exception);
				showToast('error', 'Some error occurred');
			}
		}).always(function(data){
			$(".submit").removeClass("disabled");
		})
	})
})
