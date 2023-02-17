package cn.cas.iie.gate.filter;

import cn.cas.iie.gate.common.bean.Result;
import cn.cas.iie.gate.util.GZIPUtils;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

/**
 * 响应体转换处理
 *
 * @author heguitang
 */
@Slf4j
@Component
public class HttpResponseBodyGlobalFilter implements GlobalFilter, Ordered {


    @Override
    public int getOrder() {
        // -1 is response write filter, must be called before that
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("global filter HttpResponseBody，processing response results");

        // 这里可以增加一些业务判断条件，进行跳过处理
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus statusCode = response.getStatusCode();
        if(ObjectUtil.notEqual(statusCode,HttpStatus.OK)){
            return chain.filter(exchange);
        }
        DataBufferFactory bufferFactory = response.bufferFactory();
        // 响应装饰
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                log.info("global filter HttpResponseBody，Response processing，getStatusCode={}", getStatusCode());
                if (getStatusCode() != null && body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        // 如果响应过大，会进行截断，出现乱码，看api DefaultDataBufferFactory
                        // 有个join方法可以合并所有的流，乱码的问题解决
                        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                        DataBuffer dataBuffer = dataBufferFactory.join(dataBuffers);
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        // 释放掉内存
                        DataBufferUtils.release(dataBuffer);

                        List<String> encodingList = exchange.getResponse().getHeaders().get(HttpHeaders.CONTENT_ENCODING);
                        boolean zip = encodingList != null && encodingList.contains("gzip");
                        // responseData就是response的值，就可查看修改了
                        String result = "";
                        String responseData = getResponseData(zip, content);
                        log.info("响应结果为：{}", responseData);
                        if(!JSONUtil.isTypeJSON(responseData)){
                            response.getHeaders().setContentLength(responseData.getBytes(StandardCharsets.UTF_8).length);
                            return bufferFactory.wrap(responseData.getBytes(StandardCharsets.UTF_8));
                        }
                        JSONObject jsonObject = JSONUtil.parseObj(responseData);
                        if (!jsonObject.containsKey("data") && ObjectUtil.notEqual(jsonObject.getInt("code"),500)) {
                            // 重置返回参数
                            result=  responseConversion(jsonObject);
                        } else {
                            result=responseData;
                        }
                        byte[] uppedContent = getUppedContent(zip, result);
                        response.getHeaders().setContentLength(uppedContent.length);
                        response.setStatusCode(HttpStatus.OK);

                        return bufferFactory.wrap(uppedContent);
                    }));
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        // replace response with decorator
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private String responseConversion(JSONObject resultJson) {
        try {
            // 返回值基本数据类型、返回对象、数组的判断
            Result result = resultJson.toBean(Result.class);
            HashMap<String, Object> dataObj = new HashMap<>();
            resultJson.forEach((k,v)->{
               if(!StrUtil.equalsAny(k,"code","message")){
                   dataObj.put(k,v);
               }
            });
            result.setData(dataObj);
            return JSONUtil.toJsonStr(result);
        } catch (Exception e) {
            log.error("响应包装转换失败，异常信息为：", e);
            return resultJson.toString();
        }
    }

    private String getResponseData(boolean zip, byte[] content) {
        String responseData;
        if (zip) {
            responseData = GZIPUtils.uncompressToString(content);
        } else {
            responseData = new String(content, StandardCharsets.UTF_8);
        }
        return responseData;
    }


    private byte[] getUppedContent(boolean zip, String result) {
        byte[] uppedContent;
        if (zip) {
            uppedContent = GZIPUtils.compress(result);
        } else {
            uppedContent = result.getBytes(StandardCharsets.UTF_8);
        }
        return uppedContent;
    }

}
