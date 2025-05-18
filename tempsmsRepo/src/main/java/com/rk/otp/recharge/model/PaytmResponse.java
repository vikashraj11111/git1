package com.rk.otp.recharge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PaytmResponse {

	@JsonProperty("TXNID")
	private String txnId;
	
	@JsonProperty("BANKTXNID")
	private String bankTxnId;
	
	@JsonProperty("ORDERID")
	private String orderId;
	
	@JsonProperty("TXNAMOUNT")
	private String txnAmount;
	
	@JsonProperty("STATUS")
	private String status;
	
	@JsonProperty("TXNTYPE")
	private String txnType;
	
	@JsonProperty("GATEWAYNAME")
	private String gatewayName;
	
	@JsonProperty("RESPCODE")
	private String respCode;
	
	@JsonProperty("RESPMSG")
	private String respMsg;
	
	@JsonProperty("BANKNAME")
	private String bankName;
	
	@JsonProperty("MID")
	private String mid;
	
	@JsonProperty("PAYMENTMODE")
	private String paymentMode;
	
	@JsonProperty("REFUNDAMT")
	private String refundAmt;
	
	@JsonProperty("TXNDATE")
	private String txnDate;

}
