package com.rk.otp.recharge.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.rk.app.persistence.repository.MetaDataRepo;
import com.rk.otp.constants.AppConstants;
import com.rk.otp.recharge.model.OrderResponse;
import com.rk.otp.recharge.repo.OrderDetailsRepo;
import com.rk.otp.recharge.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

	private Random rand = new Random();

	private final OrderDetailsRepo orderDetailsRepo;

	private final MetaDataRepo metaDataRepo;

	public PaymentServiceImpl(OrderDetailsRepo orderDetailsRepo, MetaDataRepo metaDataRepo) {
		this.orderDetailsRepo = orderDetailsRepo;
		this.metaDataRepo = metaDataRepo;
	}

	@Override
	public double getUniqueAmount(double amount) throws RuntimeException {

		double newAmount = 0;
		boolean isToProceed = false;
		/*int iteration = 0;
		do {
			newAmount = amount;
			newAmount += rand.nextInt(0, 9);
			isToProceed = orderDetailsRepo.findByAmountAndStatus(newAmount, AppConstants.STATUS_IN_PROGRESS).isEmpty();
			iteration++;
		} while (!isToProceed && iteration < 10);
		*/
		//if(iteration == 10) {
			do {
				newAmount = amount;
				newAmount += ((double) rand.nextInt(1, 99)) / 100;
				isToProceed = orderDetailsRepo.findByAmountAndStatus(newAmount, AppConstants.STATUS_IN_PROGRESS).isEmpty();
			} while (!isToProceed);
		//}

		return newAmount;
	}

	@Override
	public void generateQrCode(Long userId, OrderResponse orderResponse) {
		String upiId = metaDataRepo.findValueByAttribute("bharatpe.upi.id");
		
		//String text = "upi://pay?pa=%s&pn=Tempsms&am=%s";
		//String text = "upi://pay?pa=%s&pn=SANDEEP SINGH&cu=INR&tn=Pay To SANDEEP SINGH&tr=WHATSAPP_QR&am=%s";
		String text = "upi://pay?pa=%s&am=%s";
		String qrText = String.format(text, upiId, orderResponse.getAmount()); // Format QR - add amount in place of %s in the url

		ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
		BitMatrix matrix;
		try {
			matrix = new MultiFormatWriter().encode(qrText, BarcodeFormat.QR_CODE, 200, 200);
			MatrixToImageWriter.writeToStream(matrix, "PNG", qrOut);
		} catch (WriterException | IOException e) {
			e.printStackTrace();
		}
		String encodedQr = Base64.getEncoder().encodeToString(qrOut.toByteArray());
		orderResponse.setQrCode(encodedQr);
		orderResponse.setQrLink(qrText);

	}

}
