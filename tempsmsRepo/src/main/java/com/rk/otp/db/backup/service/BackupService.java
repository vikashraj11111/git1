package com.rk.otp.db.backup.service;

import java.io.IOException;
import java.util.List;

public interface BackupService {

	void backupUserDetails() throws IOException;

	List<String> backupEverything();

	String backupTable(Class<?> clazz) throws IOException;

	/*******************************************************************************************
	 *********************************** RESTORE BACKUP ****************************************
	 *******************************************************************************************/
	boolean restoreBackup(String fileName);

}
