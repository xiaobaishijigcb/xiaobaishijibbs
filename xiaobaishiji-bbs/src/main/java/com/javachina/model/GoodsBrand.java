package com.javachina.model;

import com.blade.jdbc.annotation.Table;

@Table(value = "t_goods_brand", PK = "bid")
public class GoodsBrand {

	private Integer bid;

	private String name;

	public GoodsBrand() {
	}

	public Integer getBid() {
		return bid;
	}

	public void setBid(Integer bid) {
		this.bid = bid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
