package com.lememo.sentinel.freqparamflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动时加参数：
 * -Dproject.name=xxx -Dcsp.sentinel.dashboard.server=consoleIp:port
 * 可以在对应的 sentinel 的 dashboard 中查看效果
 * @author houyi
 * @date 2019-01-19
 */
@SpringBootApplication(scanBasePackages = {"com.lememo.sentinel"})
public class FreqParamFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FreqParamFlowApplication.class, args);
    }
}
