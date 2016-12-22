package com.javachina;

import blade.kit.EncrypKit;

/**
 * Created by Administrator on 2016/12/14.
 */
public class PasswdTest {
    public static void main(String[] args) {
        String loginName = "ceshi1";
        String passWord = "111111";
        String pwd = EncrypKit.md5(loginName + passWord);
        System.out.println(pwd);
    }
}
