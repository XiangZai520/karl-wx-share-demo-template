package com.karl.wx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @program: xydtech-api
 * @description: 微信token
 * @Author: Karl
 * @QQ: 1398080880
 * @WX: 825541551
 * @PersonalBelief: 没有最好，只有更好，努力创造美好未来。
 * @Date: Create in 2020-03-23 10:14:51
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Token {
    private String ticket;
    private String token;
    private long addTime;
    private long expiresIn;

}
