package com.karl.wx.service;

import com.alibaba.fastjson.JSONObject;
import com.karl.wx.model.*;
import com.karl.wx.util.CheckUtils;
import com.karl.wx.util.WxUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuweilong
 * @description
 * @date 2019/5/22 14:24
 */
@Service
public class WxServiceImpl implements WxService {
    @Autowired
    private RestTemplate restTemplate;


    private static final String ACCESS_TOKE_CACHE_KEY = "access:token";
    /**
     * 根据微信文档，获取access_token时，grant_type固定为该值
     */
    private static final String GRANT_TYPE = "client_credential";
    /**
     * 两小时过期，但是考虑到网络延迟和微信辣鸡文档，设置过期时间为一小时
     */
    private static final long EXPIRE = 3600 * 1000;
    private static final String ACCESS_TOKEN_REQ_BASE_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TICKET_BASE_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";


    // 缓存添加的时间
    public static String cacheAddTime = null;
    // token,ticket缓存
    public static Map<String, Token> TOKEN_TICKET_CACHE = new HashMap<String, Token>();
    // token对应的key
    private static final String TOKEN = "token";
    // ticket对应的key
    private static final String TICKET = "ticket";


    @Value("${wx.appId}")
    private String appId;
    @Value("${wx.appsecret}")
    private String appsecret;
    @Value("${wx.token}")
    private String token;

    @Override
    public WxConfig getWxConfig(String link) {
        String accessToken = getAccessToken();
        String ticket = getTicket(accessToken);
        return WxUtils.getConfig(ticket, link, appId);
    }

    @Override
    public String validToken(WxRequest request) {
        System.out.println("请求进入");
        System.out.println("token验证请求进入");
        if (request == null) {
            System.out.println("request参数为空");
            return "error";
        }
        String signature = request.getSignature();
        String timestamp = request.getTimestamp();
        String nonce = request.getNonce();
        String echostr = request.getEchostr();
        System.out.println("signature为 " + signature);
        System.out.println("timestamp为 " + timestamp);
        System.out.println("nonce为     " + nonce);
        System.out.println("echostr为   " + echostr);
        boolean success = CheckUtils.checkSignature(signature, timestamp, nonce, token);
        if (success) {
            return echostr;
        }
        return "";
    }

    /**
     * 根据appid和secret获取access_token，顺便缓存
     */
    private String getAccessToken() {
        String accessToken = "";
        Token tockenTicketCache = getTokenTicket(TOKEN);
        long currentTime = System.currentTimeMillis() / 1000;
        if (tockenTicketCache != null && (currentTime - tockenTicketCache.getAddTime() <= tockenTicketCache.getExpiresIn())) {// 缓存存在并且没过期
            System.out.println("==========缓存中token已获取时长为：" + (currentTime - tockenTicketCache.getAddTime()) + "秒，可以重新使用");
            accessToken = tockenTicketCache.getToken();
        }


        if (!StringUtils.isEmpty(accessToken)) {
            return accessToken;
        }
        System.out.println("==========token已过期，正在重新获取===============");
        //这个url链接地址和参数皆不能变
        //访问链接
        String accessTokenUrl = generateAccessTokenUrl();
        HttpEntity<String> requestEntity = generateHttpEntity();
        ResponseEntity<String> responseEntity = restTemplate.exchange(accessTokenUrl, HttpMethod.GET, requestEntity, String.class);
        HttpStatus statusCode = responseEntity.getStatusCode();
        if (!HttpStatus.OK.equals(statusCode)) {
            System.out.println("==========获取token异常===============");
            return "";
        }

        String body = responseEntity.getBody();
        AccessTokenResponse tokenResponse = JSONObject.parseObject(body, AccessTokenResponse.class);
        accessToken = tokenResponse.getAccess_token();
        if (!StringUtils.isEmpty(accessToken)) {
            System.out.println("==========token获取成功===============");
            System.out.println("==========缓存中token不存在或已过期===============");
            Token token = null;
            token = new Token();
            token.setToken(accessToken);
            token.setExpiresIn(tokenResponse.getExpires_in() / 2);// 正常过期时间是7200秒，此处设置3600秒读取一次
            System.out.println("==========tocket缓存过期时间为:" + token.getExpiresIn() + "秒");
            token.setAddTime(System.currentTimeMillis() / 1000);
            updateToken(TOKEN, token);


        }
        return accessToken;
    }

    private HttpEntity<String> generateHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        return new HttpEntity<>(null, headers);
    }

    private String generateAccessTokenUrl() {
        return ACCESS_TOKEN_REQ_BASE_URL
                + "?grant_type=" + GRANT_TYPE
                + "&appid=" + appId
                + "&secret=" + appsecret;
    }


    private String getTicket(String accessToken) {

        long currentTime = System.currentTimeMillis() / 1000;
        Token tockenTicketCache = getTokenTicket(TICKET);
        Token Token = null;
        if (tockenTicketCache != null && (currentTime - tockenTicketCache.getAddTime() <= tockenTicketCache.getExpiresIn())) {// 缓存中有ticket
            System.out.println("==========缓存中ticket已获取时长为：" + (currentTime - tockenTicketCache.getAddTime()) + "秒，可以重新使用");
            return tockenTicketCache.getTicket();
        }
        System.out.println("==========缓存中ticket不存在或已过期===============");


        // 这个url链接和参数不能变
        String url = generateTicketUrl(accessToken);

        HttpEntity<String> requestEntity = generateHttpEntity();
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        HttpStatus statusCode = responseEntity.getStatusCode();
        if (!HttpStatus.OK.equals(statusCode)) {
            System.out.println("==========请求ticket异常===============");
            return "";
        }
        String body = responseEntity.getBody();
        TicketResponse ticketResponse = JSONObject.parseObject(body, TicketResponse.class);
        // 如果请求成功
        if (null != ticketResponse) {
            Token = new Token();
            Token.setTicket(ticketResponse.getTicket());
            Token.setExpiresIn(ticketResponse.getExpires_in()/ 2);// 正常过期时间是7200秒，此处设置3600秒读取一次
            System.out.println("==========ticket缓存过期时间为:" + Token.getExpiresIn() + "秒");
            Token.setAddTime(currentTime);
            updateToken(TICKET, Token);
        }

        return ticketResponse.getTicket();
    }

    private String generateTicketUrl(String accessToken) {
        return TICKET_BASE_URL + "?access_token=" + accessToken + "&type=jsapi";
    }

    /**
     * 从缓存中读取token或者ticket
     *
     * @return
     */
    private static Token getTokenTicket(String key) {
        if (TOKEN_TICKET_CACHE != null && TOKEN_TICKET_CACHE.get(key) != null) {
            System.out.println("==========从缓存中获取到了" + key + "成功===============");
            return TOKEN_TICKET_CACHE.get(key);
        }
        return null;
    }

    /**
     * 更新缓存中token或者ticket
     *
     * @return
     */
    private static void updateToken(String key, Token accessTocken) {
        if (TOKEN_TICKET_CACHE != null && TOKEN_TICKET_CACHE.get(key) != null) {
            TOKEN_TICKET_CACHE.remove(key);
            System.out.println("==========从缓存中删除" + key + "成功===============");
        }
        TOKEN_TICKET_CACHE.put(key, accessTocken);
        cacheAddTime = String.valueOf(accessTocken.getAddTime());// 更新缓存修改的时间
        System.out.println("==========更新缓存中" + key + "成功===============");
    }
}
