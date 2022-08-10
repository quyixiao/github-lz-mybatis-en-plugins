package com.test;

import com.lz.mybatis.plugins.interceptor.utils.PStringUtils;

public class TestSql3 {

    public static void main(String[] args) {
        String sql = "select * from user where user_id IN (?,?)";
        int i = PStringUtils.getStringCountKey(sql.toLowerCase(), "in");
        System.out.println(i);
    }
}
