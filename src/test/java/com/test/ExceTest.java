package com.test;

import ch.qos.logback.classic.Logger;
import com.lz.mybatis.plugins.interceptor.utils.PDingDingUtils;
import com.lz.mybatis.plugins.interceptor.utils.PExceptionUtils;

public class ExceTest {

    public static void main(String[] args) throws Exception {
        Logger.inheritableThreadLocalNo.set("tr939283929382893289");

        try {
            int i = 0 ;
            int j = 0;
            int z = i / j ;
        } catch (Exception e) {
            e.printStackTrace();
            PDingDingUtils.sendText("异常编号 =" + Logger.inheritableThreadLocalNo.get()
                    + "\n mapperId =" + "TestUserMapper.selectUserName"
                    + "\n sql = " + "select * from t_user where username= ?"
                    + "\n 异常堆栈 = " + PExceptionUtils.dealException(e));
        }
    }
}
