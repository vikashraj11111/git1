package com.rk.otp.service;

import java.util.List;
import java.util.Optional;

import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.CodeNumberArchive;
import com.rk.app.persistence.entity.projection.ProfitEntity;

public interface CodeNumberArchiveService {

	void saveAll(List<CodeNumberArchive> codeNumberArchiveList);

	boolean archiveCodeNumberList(List<CodeNumber> codeNumberList);

	boolean archiveCodeNumber(Optional<CodeNumber> optional);

	void saveAllAndFlush(List<CodeNumberArchive> codeNumberArchives);

	List<ProfitEntity> getLast30DaysProfit();

	List<ProfitEntity> getTodaysProfit();

}
