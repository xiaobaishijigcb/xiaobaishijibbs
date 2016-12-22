package com.javachina.model;

import java.util.Date;

import com.blade.jdbc.annotation.Table;
@Table(value = "t_news", PK = "newsid")
public class News {

	private Integer newsid;

	private String title;

	private String content;

	private Date date;

	private String author;

	private String keyword;

	private String prepaer1;

	private String prepaer2;

	private String prepaer3;

	public News() {
	}

	public Integer getNewsid() {
		return newsid;
	}

	public void setNewsid(Integer newsid) {
		this.newsid = newsid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getPrepaer1() {
		return prepaer1;
	}

	public void setPrepaer1(String prepaer1) {
		this.prepaer1 = prepaer1;
	}

	public String getPrepaer2() {
		return prepaer2;
	}

	public void setPrepaer2(String prepaer2) {
		this.prepaer2 = prepaer2;
	}

	public String getPrepaer3() {
		return prepaer3;
	}

	public void setPrepaer3(String prepaer3) {
		this.prepaer3 = prepaer3;
	}

}
