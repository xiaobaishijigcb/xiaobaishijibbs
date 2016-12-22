package com.javachina.model;

import com.blade.jdbc.annotation.Table;

@Table(value = "t_goods_type", PK = "tid")
public class GoodsType {

	private Integer tid;

	private String name;

	public GoodsType() {
	}

	public Integer getTid() {
		return tid;
	}

	public void setTid(Integer tid) {
		this.tid = tid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
