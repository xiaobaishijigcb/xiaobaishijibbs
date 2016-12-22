package com.javachina.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blade.ioc.annotation.Inject;
import com.blade.ioc.annotation.Service;
import com.blade.jdbc.AR;
import com.blade.jdbc.Page;
import com.blade.jdbc.QueryParam;
import com.javachina.Types;
import com.javachina.kit.DateKit;
import com.javachina.kit.Utils;
import com.javachina.model.Comment;
import com.javachina.model.Notice;
import com.javachina.model.Topic;
import com.javachina.model.User;
import com.javachina.service.CommentService;
import com.javachina.service.NoticeService;
import com.javachina.service.TopicService;
import com.javachina.service.UserService;

import blade.kit.StringKit;

@Service
public class NoticeServiceImpl implements NoticeService {
	
	@Inject
	private TopicService topicService;
	
	@Inject
	private UserService userService;
	
	@Inject
	private CommentService commentService;
	
	@Override
	public boolean save(String type,Long uid, Long to_uid, Long event_id) {
		if(StringKit.isNotBlank(type) && null != to_uid && null != event_id){
			AR.update("insert into t_notice(type,uid, to_uid, event_id, create_time) values(?, ?,?, ?, ?)", type,uid,
					to_uid, event_id, DateKit.getCurrentUnixTime()).executeUpdate();
			return true;
		}
		return false;
	}
	
	@Override
	public Page<Map<String, Object>> getNoticePage(Long uid, Integer page, Integer count) {
		if(null != uid){
			if(null == page || page < 1){
				page = 1;
			}
			if(null == count || count < 1){
				count = 10;
			}
			QueryParam queryParam = QueryParam.me();
			queryParam.eq("to_uid", uid).orderby("id desc").page(page, count);
			Page<Notice> noticePage = AR.find(queryParam).page(Notice.class);
			return this.getNoticePageMap(noticePage);
		}
		return null;
	}
	
	private Page<Map<String, Object>> getNoticePageMap(Page<Notice> noticePage){
		long totalCount = noticePage.getTotalCount();
		int page = noticePage.getPage();
		int pageSize = noticePage.getPageSize();
		Page<Map<String, Object>> pageResult = new Page<Map<String,Object>>(totalCount, page, pageSize);
		
		List<Notice> notices = noticePage.getResults();
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		if(null != notices){
			for(Notice notice : notices){
				Map<String, Object> map = this.getNotice(notice);
				if(null != map && !map.isEmpty()){
					result.add(map);
				}
			}
		}
		pageResult.setResults(result);
		
		return pageResult;
	}
	
	private Map<String, Object> getNotice(Notice notice){
		if(null == notice){
			return null;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		Long uid = notice.getTo_uid();
		User user = userService.getUser(uid);
		if(null == user){
			return null;
		}
		map.put("id", notice.getId());
		map.put("type", notice.getType());
		map.put("create_time", notice.getCreate_time());
		map.put("user_name", user.getLogin_name());
		map.put("uid", uid);
		if(notice.getType().equals(Types.comment_at.toString()) || notice.getType().equals(Types.comment.toString())){
			Comment comment = commentService.getComment(notice.getEvent_id());
			if(null != comment){
				Topic topic = topicService.getTopic(comment.getTid());
				if(null != topic){
					String title = topic.getTitle();
					String content = Utils.markdown2html(comment.getContent());
					map.put("title", title);
					map.put("content", content);
					map.put("tid", topic.getTid());
				}
			}
		}
		
		if(notice.getType().equals(Types.topic_at.toString())){
			Topic topic = topicService.getTopic(notice.getEvent_id());
			if(null != topic){
				String title = topic.getTitle();
				String content = Utils.markdown2html(topic.getContent());
				
				map.put("title", title);
				map.put("content", content);
				map.put("tid", topic.getTid());
			}
		}
		
		return map;
	}

	@Override
	public boolean read(Long to_uid) {
		if(null != to_uid){
			// 删除
			try {
				AR.update("update t_notice set is_read = 1 where to_uid = ?", to_uid).executeUpdate(true);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public Long getNotices(Long uid) {
		if(null != uid){
			return AR.find("select count(1) from t_notice where to_uid = ? and is_read = 0", uid).first(Long.class);
		}
		return 0L;
	}

}
