$(document).ready(function() {

	$(document).on("click", ".select-box-otp .selected", function(){
		const optionContainer = $(".options-container");
		$(optionContainer).toggleClass("active");
		if(optionContainer.hasClass("active")) {
			$(".select-box-otp input").focus();
			$(".select-box-otp input").val('');
			filterList('');
		}
	})
	
	$(document).click(function(e){
		if(!$(e.target).closest(".select-box").length){
			$(".options-container").removeClass("active");
		}
	})
	
	$(document).on("click", ".select-box-otp .option", function(evt) {
		let service = $(this).closest(".option").find(".service");
		$(".selected").text($(service).text());
		$(".options-container").removeClass("active");
		populateServers($(service).prop("id"));
	})
	
	$(document).on("keyup", ".select-box-otp input", function(e){
		filterList(e.target.value);
	})
	
})


const filterList = searchTerm => {
	searchTerm = searchTerm.toLowerCase();
	$(".option").each(function(){
		if($(this).find(".service").text().toLowerCase().indexOf(searchTerm) != -1) {
			$(this).show();
		} else {
			$(this).hide();
		}
	})
}

function populateServers(id) {
	$(".servers-container").find("div").each(function(){
		$(this).removeClass("active");
	})
	
	$("#active-number").val("false");
	$(".servers-container").removeClass("hidden");
	$("#servers_" + id).addClass("active");
	
	let val = $("#servers_" + id).find("button").prop("id");
	$("#servers_" + id + " button").first().addClass("disabled");
	
	loadOtpPage(val);
}


function loadOtpPage(id){
	$("#otp-content").empty();
	let url = "/otp/" + id;
	
	$("#otp-content").load(url);
	$("#otp-content").show();
}
