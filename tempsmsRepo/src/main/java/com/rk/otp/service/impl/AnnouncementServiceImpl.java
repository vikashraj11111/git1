package com.rk.otp.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.Announcement;
import com.rk.app.persistence.repository.AnnouncementRepository;
import com.rk.otp.service.AnnouncementService;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

	@Autowired
	private AnnouncementRepository announcementRepository;

	@Override
	public List<Announcement> getAnnouncements(){
		List<Announcement> announcements = announcementRepository.findAll();
		return announcements.isEmpty() ? null : announcements;
	}
	
	@Override
	public Long addAnnouncement(Announcement announcement){
		try {
			announcement = announcementRepository.saveAndFlush(announcement);
		} catch (Exception e) {
			return null;
		}
		
		return announcement.getId();
	}
	
	@Override
	public boolean deleteAnnouncement(Long id) {
		try {
			announcementRepository.deleteById(id);
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
}
