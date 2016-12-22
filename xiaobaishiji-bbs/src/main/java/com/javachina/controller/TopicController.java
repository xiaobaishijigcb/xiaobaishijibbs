package com.javachina.controller;

import blade.kit.StringKit;
import com.blade.ioc.annotation.Inject;
import com.blade.jdbc.Page;
import com.blade.jdbc.QueryParam;
import com.blade.route.annotation.Path;
import com.blade.route.annotation.PathVariable;
import com.blade.route.annotation.Route;
import com.blade.view.ModelAndView;
import com.blade.web.http.HttpMethod;
import com.blade.web.http.Request;
import com.blade.web.http.Response;
import com.blade.web.http.wrapper.ServletRequest;
import com.blade.web.multipart.FileItem;
import com.blade.web.multipart.MultipartException;
import com.javachina.Actions;
import com.javachina.Constant;
import com.javachina.Types;
import com.javachina.kit.DateKit;
import com.javachina.kit.SessionKit;
import com.javachina.model.LoginUser;
import com.javachina.model.Topic;
import com.javachina.service.*;

import javax.servlet.ServletException;
import java.io.*;
import java.util.List;
import java.util.Map;

@Path("/")
public class TopicController extends BaseController {
	// 上传文件存储目录
	private static final String UPLOAD_DIRECTORY = "upload";

	// 上传配置
	private static final int MEMORY_THRESHOLD = 1024 * 1024 * 3; // 3MB
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 50; // 40MB
	private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 60; // 50MB
	@Inject
	private TopicService topicService;

	@Inject
	private TopicCountService topicCountService;

	@Inject
	private NodeService nodeService;

	@Inject
	private CommentService commentService;

	@Inject
	private SettingsService settingsService;

	@Inject
	private FavoriteService favoriteService;

	@Inject
	private UserService userService;

	@Inject
	private UserlogService userlogService;

	@Inject
	private TopicCountService typeCountService;

	/**
	 * 发布帖子页面
	 */
	@Route(value = "/topic/add", method = HttpMethod.GET)
	public ModelAndView show_add_topic(Request request, Response response) {
		LoginUser user = SessionKit.getLoginUser();
		if (null == user) {
			response.go("/");
			return null;
		}
		this.putData(request);
		Long pid = request.queryAsLong("pid");
		request.attribute("pid", pid);
		return this.getView("topic_add");
	}

	/**
	 * 编辑帖子页面
	 */
	@Route(value = "/topic/edit/:tid", method = HttpMethod.GET)
	public ModelAndView show_ediot_topic(@PathVariable("tid") Long tid, Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();
		if (null == user) {
			response.go("/");
			return null;
		}

		Topic topic = topicService.getTopic(tid);
		if (null == topic) {
			request.attribute(this.ERROR, "不存在该帖子");
			return this.getView("info");
		}

		if (!topic.getUid().equals(user.getUid())) {
			request.attribute(this.ERROR, "您无权限编辑该帖");
			return this.getView("info");
		}

		// 超过300秒
		if ((DateKit.getCurrentUnixTime() - topic.getCreate_time()) > 300) {
			request.attribute(this.ERROR, "发帖已经超过300秒，不允许编辑");
			return this.getView("info");
		}

		this.putData(request);
		request.attribute("topic", topic);

		return this.getView("topic_edit");
	}

	/**
	 * 编辑帖子操作
	 */
	@Route(value = "/topic/edit", method = HttpMethod.POST)
	public void edit_topic(Request request, Response response){
		Long tid = request.queryAsLong("tid");
		String title = request.query("title");
		String content = request.query("content");
		Long nid = request.queryAsLong("nid");
		
		LoginUser user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}
		
		if(null == tid){
			this.error(response, "不存在该帖子");
			return;
		}
		
		// 不存在该帖子
		Topic topic = topicService.getTopic(tid);
		if(null == topic){
			this.error(response, "不存在该帖子");
			return;
		}
		
		// 无权限操作
		if(!topic.getUid().equals(user.getUid())){
			this.error(response, "无权限操作该帖");
			return;
		}
		
		// 超过300秒
		if( (DateKit.getCurrentUnixTime() - topic.getCreate_time()) > 300 ){
			this.error(response, "超过300秒禁止编辑");
			return;
		}
		
		if(StringKit.isBlank(title) || StringKit.isBlank(content) || null == nid){
			this.error(response, "部分内容未输入");
			return;
		}
		
		if(title.length() < 4 || title.length() > 50){
			this.error(response, "标题长度在4-50个字符哦");
			return;
		}
		
		if(content.length() < 5){
			this.error(response, "您真是一字值千金啊。");
			return;
		}
		
		if(content.length() > 10000){
			this.error(response, "内容太长了，试试少吐点口水");
			return;
		}
		
		Long last_time = topicService.getLastUpdateTime(user.getUid());
		if(null != last_time && (DateKit.getCurrentUnixTime() - last_time) < 10 ){
			this.error(response, "您操作频率太快，过一会儿操作吧！");
			return;
		}
		
		try {
			// 编辑帖子
			topicService.update(tid, nid, title, content);
			userlogService.save(user.getUid(), Actions.UPDATE_TOPIC, content);
			
			this.success(response, "帖子编辑成功");
		} catch (Exception e) {
			e.printStackTrace();
			this.error(response, "帖子编辑失败");
			return;
		}
	}

	/**
	 * 发布帖子操作
	 *
	 * @throws IOException

	 * @throws ServletException
	 * @throws MultipartException
	 */
	@Route(value = "/topic/add", method = HttpMethod.POST)
	public ModelAndView add_topic(Request request, Response response) throws Exception {
//		response.raw().setCharacterEncoding("UTF-8");
//		response.contentType("multipart/form-data;charset=utf-8");
//		request.encoding("UTF-8");
		String title = new String(request.query("title").getBytes(),"utf-8");
		String content = new String(request.query("content").getBytes(),"utf-8");
//		String title = request.query("title");
//		String content = request.query("content");
		Long nid = request.queryAsLong("nid");
		ServletRequest req = (ServletRequest) request;

		this.putData(request);
		Long pid = request.queryAsLong("pid");
		request.attribute("pid", pid);


		LoginUser user = SessionKit.getLoginUser();

		if (null == user) {
			this.nosignin(response);

			return this.getView("topic_add");
		}

		if (StringKit.isBlank(title) || StringKit.isBlank(content) || null == nid) {
			request.attribute(this.ERROR, "部分内容未输入");
			return this.getView("topic_add");

		}

		if ( title.length() > 50) {
			request.attribute(this.ERROR,"标题长度在4-50个字符哦");
			return this.getView("topic_add");
		}

		if (content.length() < 2) {
			request.attribute(this.ERROR, "您真是一字值千金啊。");
			return this.getView("topic_add");
		}

		if (content.length() > 10000) {
			request.attribute(this.ERROR, "内容太长了，试试少吐点口水");
			return this.getView("topic_add");
		}

		Long last_time = topicService.getLastCreateTime(user.getUid());
		if (null != last_time && (DateKit.getCurrentUnixTime() - last_time) < 10) {
			request.attribute(this.ERROR, "您操作频率太快，过一会儿操作吧！");
			return this.getView("topic_add");
		}
		FileItem[] items = (FileItem[]) req.files();
		String videoPath = "";
		String picPath = "";
		if (items.length > 0 && items != null&&items[0].getFileName()!=null&&!"".equals(items[0].getFileName())) {
			String uploadPath = req.context().getRealPath("./") + File.separator + "assets/upload/";
			// 如果目录不存在则创建

			File uploadDir = new File(uploadPath);
			if (!uploadDir.exists()) {
				uploadDir.mkdir();
			}
			for (int i = 0; i < items.length; i++) {
				FileItem item = items[i];
				String fileName = item.getFileName();
				if("".equals(fileName)||null==fileName){
					continue;
				}
				String filePath = uploadPath + File.separator + fileName;
				// filePath = req.contextPath()+"\\WEB-INF\\upload\\"+fileName;
				// filePath =
				// this.getClass().getClassLoader().getResourceAsStream("/WEB-INF/upload").toString()+fileName;
				InputStream is = new FileInputStream(item.getFile());
				OutputStream os = new FileOutputStream(new File(filePath));
				byte[] b = new byte[1024 * 1024];
				int len = 1;
				while ((len = is.read(b)) != -1) {
					os.write(b, 0, len);
				}
				String[] video = {"mp4"};
				String pic = "bmp,jpg,jpeg,tiff,gif,pcx,png,tga,exif,fpx,svg,psd,cdr,pcd,dxf,ufo,eps,ai,raw,wmf";

				String format = fileName.substring(fileName.lastIndexOf(".")+1);
				if(fileName.endsWith(".mp4")||fileName.endsWith(".flv")){
					if(i==2){
						request.attribute(this.ERROR, "请放一个是视频,不要放两个");
						return this.getView("topic_add");
					}
					videoPath = "assets/upload/" + fileName;
				}
				if(pic.contains(format.toLowerCase())){
					picPath += "assets/upload/"+fileName+";";
				}
				os.flush();
				os.close();
				is.close();

			}
		}
		// 发布帖子
		try {
			Long tid = topicService.save(user.getUid(), nid, title, content, 0, videoPath, picPath);
			if (null != tid) {
				Constant.SYS_INFO = settingsService.getSystemInfo();
				Constant.VIEW_CONTEXT.set("sys_info", Constant.SYS_INFO);

				userlogService.save(user.getUid(), Actions.ADD_TOPIC, content);
//				this.success(response, tid);
//				this.success(response, "/topic/"+tid);
				response.go("/topic/"+tid);
			} else {
				request.attribute(this.ERROR, "帖子发布失败");
				return this.getView("topic_add");
			}
		} catch (Exception e) {
			e.printStackTrace();
			request.attribute(this.ERROR, "帖子发布失败");
		}
		return this.getView("topic_add");
	}

	private void putData(Request request) {
		List<Map<String, Object>> nodes = nodeService.getNodeList();
		request.attribute("nodes", nodes);
	}

	/**
	 * 帖子详情页面
	 */
	@Route(value = "/topic/:tid", method = HttpMethod.GET)
	public ModelAndView show_topic(@PathVariable("tid") Long tid, Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();

		Long uid = null;
		if (null != user) {
			uid = user.getUid();
		} else {
			SessionKit.setCookie(response, Constant.JC_REFERRER_COOKIE, request.url());
		}

		Topic topic = topicService.getTopic(tid);
		if (null == topic) {
			response.go("/");
			return null;
		}

		this.putDetail(request, response, uid, topic);

		// 刷新浏览数
		typeCountService.update(Types.views.toString(), tid, 1);
		return this.getView("topic_detail");
	}

	private void putDetail(Request request, Response response, Long uid, Topic topic) {

		Integer page = request.queryAsInt("p");
		if (null == page || page < 1) {
			page = 1;
		}

		// 帖子详情
		Map<String, Object> topicMap = topicService.getTopicMap(topic, true);
		request.attribute("topic", topicMap);

		// 是否收藏
		boolean is_favorite = favoriteService.isFavorite(Types.topic.toString(), uid, topic.getTid());
		request.attribute("is_favorite", is_favorite);

		// 是否点赞
		boolean is_love = favoriteService.isFavorite(Types.love.toString(), uid, topic.getTid());
		request.attribute("is_love", is_love);

		QueryParam cp = QueryParam.me();
		cp.eq("tid", topic.getTid()).orderby("cid asc").page(page, 20);
		Page<Map<String, Object>> commentPage = commentService.getPageListMap(cp);
		request.attribute("commentPage", commentPage);
	}

	/**
	 * 评论帖子操作
	 */
	@Route(value = "/comment/add", method = HttpMethod.POST)
	public void add_comment(Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}

		Long uid = user.getUid();

		String content = request.query("content");
		Long tid = request.queryAsLong("tid");
		Topic topic = topicService.getTopic(tid);
		if(null == topic){
			response.go("/");
			return;
		}

		if(null == tid || StringKit.isBlank(content)){
			this.error(response, "骚年，有些东西木有填哎！");
			return;
		}

		if(content.length() > 5000){
			this.error(response, "内容太长了，试试少吐点口水。");
			return;
		}

		Long last_time = topicService.getLastUpdateTime(user.getUid());
		if(null != last_time && (DateKit.getCurrentUnixTime() - last_time) < 10 ){
			this.error(response, "您操作频率太快，过一会儿操作吧！");
			return;
		}

		// 评论帖子
		try {

			String ua = request.userAgent();

			boolean flag = topicService.comment(uid, topic.getUid(), tid, content, ua);
			if(flag){
				Constant.SYS_INFO = settingsService.getSystemInfo();
				Constant.VIEW_CONTEXT.set("sys_info", Constant.SYS_INFO);
				userlogService.save(user.getUid(), Actions.ADD_COMMENT, content);
				
//				this.success(response, tid);
//				return;
				response.go( "/topic/"+tid);
			} else {
				this.error(response, "帖子评论失败");
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.error(response, "帖子评论失败");
		}

	}

	/**
	 * 加精和取消加精
	 */
	@Route(value = "/essence", method = HttpMethod.POST)
	public ModelAndView essence(Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();
		if (null == user) {
			this.nosignin(response);
			return this.getView("topic_detail");
		}

		if (user.getRole_id() > 3) {
			request.attribute(this.ERROR, "您无权限操作");
			return this.getView("topic_detail");
		}

		Long tid = request.queryAsLong("tid");
		if (null == tid || tid == 0) {
			return this.getView("topic_detail");
		}

		Topic topic = topicService.getTopic(tid);
		if (null == topic) {
			request.attribute(this.ERROR, "不存在该帖子");
			return this.getView("topic_detail");
		}

		try {
			Integer count = topic.getIs_essence() == 1 ? 0 : 1;
			topicService.essence(tid, count);
			userlogService.save(user.getUid(), Actions.ESSENCE, tid + ":" + count);

			response.go("/topic/"+tid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 帖子下沉
	 */
	@Route(value = "/sink", method = HttpMethod.POST)
	public void sink(Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();
		if (null == user) {
			this.nosignin(response);
			return;
		}

		Long tid = request.queryAsLong("tid");
		if (null == tid || tid == 0) {
			return;
		}

		try {
			boolean isFavorite = favoriteService.isFavorite(Types.sinks.toString(), user.getUid(), tid);
			if (!isFavorite) {
				favoriteService.update(Types.sinks.toString(), user.getUid(), tid);
				topicCountService.update(Types.sinks.toString(), tid, 1);
				topicService.updateWeight(tid);
				userlogService.save(user.getUid(), Actions.SINK, tid + "");
			}
			this.success(response, tid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除帖子
	 */
	@Route(value = "/delete", method = HttpMethod.POST)
	public void delete(Request request, Response response) {

		LoginUser user = SessionKit.getLoginUser();
		if (null == user) {
			this.nosignin(response);
			return;
		}

		Long tid = request.queryAsLong("tid");
		if (null == tid || tid == 0 || user.getRole_id() > 2) {
			return;
		}

		try {
			topicService.delete(tid);
			this.success(response, tid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 精华帖页面
	 */
	@Route(value = "/essence", method = HttpMethod.GET)
	public ModelAndView essencePage(Request request, Response response) {

		// 帖子
		QueryParam tp = QueryParam.me();
		Integer page = request.queryAsInt("p");

		if (null == page || page < 1) {
			page = 1;
		}

		tp.eq("status", 1).eq("is_essence", 1).orderby("create_time desc, update_time desc").page(page, 15);
		Page<Map<String, Object>> topicPage = topicService.getPageList(tp);
		request.attribute("topicPage", topicPage);

		return this.getView("essence");
	}

}
