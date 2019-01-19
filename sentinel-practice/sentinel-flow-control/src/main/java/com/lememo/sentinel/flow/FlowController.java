package com.lememo.sentinel.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author houyi.wh
 * @since 2019-01-01
 */
@Controller
public class FlowController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/getUser")
    public @ResponseBody
    UserService.User getUser(@RequestParam("uid") Long uid) {
        return userService.getUser(uid);
    }

}