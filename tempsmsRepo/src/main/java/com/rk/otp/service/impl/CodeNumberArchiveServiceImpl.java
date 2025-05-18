package com.rk.otp.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.CodeNumberArchive;
import com.rk.app.persistence.entity.projection.ProfitEntity;
import com.rk.app.persistence.repository.CodeNumberArchiveRespository;
import com.rk.otp.service.CodeNumberArchiveService;

@Service
public class CodeNumberArchiveServiceImpl implements CodeNumberArchiveService {

	@Autowired
	CodeNumberArchiveRespository codeNumberArchiveRespository;
	
	@Override
	public void saveAll(List<CodeNumberArchive> codeNumberArchiveList) {
		codeNumberArchiveRespository.saveAllAndFlush(codeNumberArchiveList);
	}
	
	@Override
	public boolean archiveCodeNumberList(List<CodeNumber> codeNumberList) {
		List<CodeNumberArchive> codeNumberArchives = new ArrayList<>();
		codeNumberList.forEach(c -> {
			CodeNumberArchive codeNumberArchive = new CodeNumberArchive();
			codeNumberArchive.setCode(c.getCode());
			codeNumberArchive.setNumber(c.getNumber());
			codeNumberArchive.setPrice(c.getPrice());
			codeNumberArchive.setServer(c.getServer());
			codeNumberArchive.setService(c.getService());
			codeNumberArchive.setUser(c.getUser());
			codeNumberArchive.setActualPrice(c.getActualPrice());
			codeNumberArchives.add(codeNumberArchive);
			codeNumberArchive.setArchiveTime(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
		});
		
		saveAll(codeNumberArchives);
		
		return true;
	}
	
	
	@Override
	public boolean archiveCodeNumber(Optional<CodeNumber> optionalCodeNumber) {
		if(optionalCodeNumber.isEmpty())
			return false;
		CodeNumber codeNumber = optionalCodeNumber.get();
		CodeNumberArchive codeNumberArchive = new CodeNumberArchive();
		codeNumberArchive.setCode(codeNumber.getCode());
		codeNumberArchive.setNumber(codeNumber.getNumber());
		codeNumberArchive.setPrice(codeNumber.getPrice());
		codeNumberArchive.setServer(codeNumber.getServer());
		codeNumberArchive.setService(codeNumber.getService());
		codeNumberArchive.setUser(codeNumber.getUser());
		codeNumberArchive.setActualPrice(codeNumber.getActualPrice());
		codeNumberArchive.setArchiveTime(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
		
		codeNumberArchiveRespository.save(codeNumberArchive);
		return true;
	}

	@Override
	public void saveAllAndFlush(List<CodeNumberArchive> codeNumberArchives) {
		codeNumberArchiveRespository.saveAllAndFlush(codeNumberArchives);
	}
	
	@Override
	public List<ProfitEntity> getTodaysProfit(){
		return codeNumberArchiveRespository.findTodaysProfit().orElse(null);
	}
	
	@Override
	public List<ProfitEntity> getLast30DaysProfit(){
		return codeNumberArchiveRespository.findLast30DaysProfit().orElse(null);
	}
	
}
