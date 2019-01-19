package com.lememo.sentinel.dashboard;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author houyi.wh
 * @since 2019-01-01
 */
@Controller
public class FlowController {

    @GetMapping("/testSentinel")
    public @ResponseBody
    String testSentinel() {
        // 定义资源，具体的规则通过 dashboard 在页面中配置
        String resourceName = "testSentinel";
        Entry entry = null;
        String retVal;
        try{
            entry = SphU.entry(resourceName, EntryType.IN);
            retVal = "passed";
        }catch(BlockException e){
            retVal = "blocked";
        }finally {
            if(entry!=null){
                entry.exit();
            }
        }
        return retVal;
    }

}