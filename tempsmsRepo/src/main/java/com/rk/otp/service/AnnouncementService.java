package com.rk.otp.service;

import java.util.List;

import com.rk.app.persistence.entity.Announcement;

public interface AnnouncementService {

	List<Announcement> getAnnouncements();

	Long addAnnouncement(Announcement announcement);

	boolean deleteAnnouncement(Long id);

}
