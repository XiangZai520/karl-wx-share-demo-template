package com.karl.wx.model;

import lombok.Data;

/**
 * @author liuweilong
 * @description
 * @date 2019/5/21 17:29
 */
@Data
public class WxConfig {
    /**
     * 随机字符串
     */
    private String nonceStr;
    private String timestamp;
    private String signature;
    private String appId;
}
