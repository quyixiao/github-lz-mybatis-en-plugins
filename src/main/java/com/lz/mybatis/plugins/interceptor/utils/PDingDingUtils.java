package com.lz.mybatis.plugins.interceptor.utils;

import ch.qos.logback.classic.Logger;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.lz.mybatis.plugins.interceptor.entity.DingTalkDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;


@Slf4j
public class PDingDingUtils {

    protected static String dingTalkUrlPre = "https://oapi.dingtalk.com/robot/send?access_token=f69b43f939ea75555a673b2b51b4977839efc580342cccc80ad788d1e687b6e4";

    public static void sendText(String text) {
        try {
            String content = text + "\n";
            content = "加密插件异常\n机器 IP：" + Logger.host + ":" + Logger.port + " \n " + content;
            content = " \n 项目路径：" + readJarFile()
                    + "\n" + content;
            DingTalkDto dingTalk = new DingTalkDto();
            dingTalk.setToken("");
            dingTalk.setText(content);
            dingTalk.setTitle("数据库加密插件异常");
            dingTalk.setMsgType("text");
            process(dingTalk);
        } catch (Exception e) {
        } finally {
        }

    }


    private static OapiRobotSendResponse process(DingTalkDto dingTalk) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient(dingTalkUrlPre);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");

        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(dingTalk.getText());
        request.setText(text);
        // 被@人的手机号(在text内容里要有@手机号)
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();


        String[] mobiles = "18458195149".split(",");
        at.setAtMobiles(Arrays.asList(mobiles));

        request.setAt(at);
        OapiRobotSendResponse response = client.execute(request);
        return response;
    }

    public static String readJarFile() {
        try {
            Resource resource = new ClassPathResource("");
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


}
