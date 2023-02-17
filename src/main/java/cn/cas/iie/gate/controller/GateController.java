package cn.cas.iie.gate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


/**
 * MainWebController 系统入口
 *
 * @author LIJIE
 * @date 2017年9月3日 下午9:51:57
 */

@Slf4j
@RestController
public class GateController {

    /**
     * 应用到所有@RequestMapper 注解方法，在其执行之前初始化数据绑定器
     *
     * @param binder
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {

    }


    @GetMapping("/find/api/{protocol}")
    public Mono<ResponseEntity<Void>> findApi(@PathVariable String protocol) {
/*		return Mono.just(new ResponseEntity<>(
                DataCenter.getProtocol(protocol),
                HttpStatus.OK));*/
        return null;
    }

}
