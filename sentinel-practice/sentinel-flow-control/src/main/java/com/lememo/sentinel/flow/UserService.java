package com.lememo.sentinel.flow;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * @author houyi.wh
 * @since 2019-01-01
 */
@Service
public class UserService {

    public static final String USER_RES = "userResource";

    public UserService(){
        // 定义热点限流的规则，对第一个参数设置 qps 限流模式，阈值为5
        FlowRule rule = new FlowRule();
        rule.setResource(USER_RES);
        // 限流类型，qps
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 设置阈值
        rule.setCount(5);
        // 限制哪个调用方
        rule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        // 基于调用关系的流量控制
        rule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        // 流控策略
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    /**
     * 根据uid获取用户信息
     * @param uid uid
     * @return 用户信息
     */
    public User getUser(Long uid){
        Entry entry = null;
        try {
            // 流控
            entry = SphU.entry(USER_RES);
            // 业务代码
            User user = new User();
            user.setUid(uid);
            user.setName("user-" + uid);
            return user;
        }catch(BlockException e){
            // 被限流了
            System.out.println("[getUser] has been protected! Time="+System.currentTimeMillis());
        }finally {
            if(entry!=null){
                entry.exit();
            }
        }
        return null;
    }


    public static class User {
        private Long uid;
        private String name;

        public Long getUid() {
            return uid;
        }

        public void setUid(Long uid) {
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}