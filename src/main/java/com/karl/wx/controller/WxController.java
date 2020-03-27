package com.karl.wx.controller;

import com.karl.wx.model.ShareRequest;
import com.karl.wx.model.WxConfig;
import com.karl.wx.model.WxRequest;
import com.karl.wx.service.WxService;
import com.karl.wx.util.WxConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author liuweilong
 * @description
 * @date 2019/5/22 14:15
 */
@RestController
@RequestMapping("/api/wx")
public class WxController {
    @Autowired
    private WxService wxService;

    /**
     * 验证token的时候，微信会调用这个接口
     */
    @RequestMapping(value = "/valid/token", method = RequestMethod.POST, consumes = "application/json")
    public String valid(WxRequest request) {
        return wxService.validToken(request);
    }

    /**
     * 获取签名，link表示当前页面
     */
    @RequestMapping(value = "/signature", method = RequestMethod.POST, consumes = "application/json")
    public WxConfig signature(String link) {
        System.err.println(link);
        return wxService.getWxConfig(link);
    }

    /**
     * 获取签名，link表示当前页面
     */
    @RequestMapping(value = "/wxShareSignature", method = RequestMethod.POST, consumes = "application/json")
    public Map<String, Object> wxShareSignature(@RequestBody ShareRequest shareRequest, HttpServletRequest request) {
        String urlTemp = "http://" + request.getServerName() + request.getContextPath();
        String urlpath = "http://" + request.getServerName();
        return WxConfigUtil.getSignature(shareRequest.getUrl(), urlTemp, urlpath);
    }
}
