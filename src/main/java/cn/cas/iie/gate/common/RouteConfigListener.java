package cn.cas.iie.gate.common;
 
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Executor;
 
/**
 * nacos监听器
 *
 * @author wanghongjie
 */
@Component
@Slf4j
public class RouteConfigListener {
 
    private String dataId = "gateway-json-routes";
    @Autowired

    private NacosConfigProperties nacosConfigProperties;
    @Autowired
    private NacosConfigManager nacosConfigManager;
    @Autowired
    private RouteOperator routeOperator;
 
    @PostConstruct
    public void dynamicRouteByNacosListener() throws NacosException {
        //log.info("gateway-json-routes dynamicRouteByNacosListener config serverAddr is {} namespace is {} group is {}", serverAddr, namespace, group);
        ConfigService configService = nacosConfigManager.getConfigService();
        // 添加监听，nacos上的配置变更后会执行
        configService.addListener(dataId, nacosConfigProperties.getGroup(), new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 解析和处理都交给RouteOperator完成
                routeOperator.refreshAll(configInfo);
            }
 
            @Override
            public Executor getExecutor() {
                return null;
            }
        });
        // 获取当前的配置
        String initConfig = configService.getConfig(dataId, nacosConfigProperties.getGroup(), 5000);
        // 立即更新
        routeOperator.refreshAll(initConfig);
    }
}