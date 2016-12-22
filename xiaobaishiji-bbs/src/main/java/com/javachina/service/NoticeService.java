package com.javachina.service;

import java.util.Map;

import com.blade.jdbc.Page;

public interface NoticeService {
	
	boolean save(String type, Long uid,Long to_uid, Long event_id);
	
	boolean read(Long to_uid);
	
	Page<Map<String, Object>> getNoticePage(Long to_uid, Integer page, Integer count);

	Long getNotices(Long to_uid);
	
}
