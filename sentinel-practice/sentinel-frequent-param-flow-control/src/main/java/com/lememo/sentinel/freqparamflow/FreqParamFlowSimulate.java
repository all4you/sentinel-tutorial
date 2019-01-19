package com.lememo.sentinel.freqparamflow;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.util.TimeUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟热点参数限流
 * @author houyi
 * @date 2019-01-19
 **/
public class FreqParamFlowSimulate {

    private final String resourceName = "freqParam";

    /**
     * 4个用户的userId
     */
    private final Integer[] userIds = new Integer[]{111,222,333,444};

    /**
     * 4个用户的请求频率
     */
    private final Integer[] freqs = new Integer[]{20,80,160,300};

    /**
     * 整个过程模拟多少秒
     */
    private int simulateSeconds = 30;

    /**
     * 通过的次数
     */
    private final Map<Integer, AtomicLong> passCountMap = new ConcurrentHashMap<>();

    private volatile boolean stop = false;

    public static void main(String[] args) {
        FreqParamFlowSimulate simulate = new FreqParamFlowSimulate();
        // 初始化规则
        simulate.initHotParamFlowRules();
        // 初始化统计的map
        simulate.initPassCountMap();
        // 模拟请求
        simulate.simulateUserRequest();
        // 对请求结果进行统计
        simulate.statisticResult();
    }

    /**
     * 初始化热点限流的规则
     */
    private void initHotParamFlowRules() {
        // 设置热点参数的规则，qps 模式，阈值为5
        ParamFlowRule rule = new ParamFlowRule(resourceName)
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(5);
        // 对userId=111的参数设置例外项，可以为该用户id的值单独设置阈值
        ParamFlowItem item = new ParamFlowItem().setObject(String.valueOf(111))
                .setClassType(int.class.getName())
                .setCount(10);
        rule.setParamFlowItemList(Collections.singletonList(item));
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    /**
     * 初始化统计的map
     */
    private void initPassCountMap(){
        for (Integer userId : userIds) {
            passCountMap.putIfAbsent(userId, new AtomicLong());
        }
    }

    /**
     * 模拟用户请求
     */
    private void simulateUserRequest() {
        int userCnt = userIds.length;
        for (int i = 0; i < userCnt; i++) {
            int userId = userIds[i];
            int freq = freqs[i];
            Thread t = new Thread(new UserRequestSimulator(userId,freq));
            t.setName("simulator-" + userId);
            t.start();
        }
    }

    /**
     * 统计结果
     */
    private void statisticResult() {
        Thread timer = new Thread(new ResponseStatisticer());
        timer.setName("statistic");
        timer.start();
    }

    /**
     * 模拟用户请求的线程
     */
    final class UserRequestSimulator implements Runnable {
        /**
         * 用户id
         */
        private int userId;
        /**
         * 请求的频率，单位：ms
         */
        private int freq;

        UserRequestSimulator(int userId,int freq){
            this.userId = userId;
            this.freq = freq;
        }

        @Override
        public void run() {
            while (!stop) {
                Entry entry = null;
                try {
                    // 对参数 userId 的值进行统计，通过热点参数的规则进行限流
                    entry = SphU.entry(resourceName, EntryType.IN, 1, userId);
                    // 用户id的值为：userId的请求已经通过了
                    passCountMap.get(userId).incrementAndGet();
                } catch (BlockException e) {
                    // ignore
                } finally {
                    if (entry != null) {
                        entry.exit();
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(freq);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 用户请求结果的统计
     * 每秒钟统计一次
     */
    final class ResponseStatisticer implements Runnable {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            System.out.println("Begin to run! Go go go!");
            System.out.println("See corresponding metrics.log for accurate statistic data");
            // 上一秒通过的次数统计
            Map<Integer, Long> lastSecondPassCountMap = new HashMap<>(userIds.length);
            for (Integer userId : userIds) {
                lastSecondPassCountMap.putIfAbsent(userId, 0L);
            }
            while (!stop) {
                try {
                    // 每秒钟统计一次
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // ignore
                }
                // 这种方式的统计可能会有误差
                // 要查看实际的统计值，可以看 `metrics.log` 日志文件
                for (Integer userId : userIds) {
                    // 这一秒通过的次数
                    long thisSecondPass = passCountMap.get(userId).get();
                    // 上一秒通过的次数
                    long lastSecondPass = lastSecondPassCountMap.get(userId);
                    // 一秒钟内通过的次数
                    long oneSecondPass = thisSecondPass - lastSecondPass;
                    // 将这一秒通过的次数更新成上一秒通过的次数
                    lastSecondPassCountMap.put(userId, thisSecondPass);
                    System.out.println(String.format("[%d][%d] Hot param metrics for resource %s: "
                                    + "pass count for userId [%s] is %d",
                            simulateSeconds, TimeUtil.currentTimeMillis(), resourceName, userId, oneSecondPass));
                }
                if (simulateSeconds-- <= 0) {
                    stop = true;
                }
            }

            long cost = System.currentTimeMillis() - start;
            System.out.println("Time cost: " + cost + " ms");
            System.exit(0);
        }
    }



}
