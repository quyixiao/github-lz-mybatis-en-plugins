package com.lz.mybatis.plugins.interceptor.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DingTalkDto implements Serializable {
    private static final long serialVersionUID = -8106262310495544261L;

    //消息内容模板。如果太长只会部分展示
    private String text;
    //备注
    private String remark;
    //消息类型，此时固定为：text, link, markdown
    private String msgType;
    //点击消息跳转的URL
    private String messageUrl;
    //图片URL
    private String picUrl;
    //首屏会话透出的展示内容
    private String title;
    //被@人的手机号(在content里添加@人的手机号
    private String atMobiles;
    //@所有人时：1,true，否则为：2.就@值班人员
    private Integer isAtAll;
    // 每个机器人的钉 talk
    private String token;

    // 不去at所有的用户
    private String notAtAllStartTime;
    // 不去at 所有的结束时间
    private String notAtAllEndTime;


}
