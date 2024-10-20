package com.aeterna.friendmall.gateway.config;

import com.aeterna.common.exception.BizCodeEnum;
import com.aeterna.common.utils.R;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.gateway.config
 * @ClassName : .java
 * @createTime : 2024/8/30 22:10
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Configuration
public class SentinelGatewayConfig {

    // TODO 学习响应式编程，天然支持高并发
    // GatewayCallbackManager  定义回调
    public SentinelGatewayConfig(){
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            // 网关限流请求的话会调用此回调、

            /**
             * Mono（返回0个或一个数据）、Flux（返回0个或多个数据） 都是Spring5引入的新特性，响应式编程
             */
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
                R error = R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
                String errJson = JSON.toJSONString(error);

                Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(errJson), String.class);

                return body;
            }
        });
    }
}
