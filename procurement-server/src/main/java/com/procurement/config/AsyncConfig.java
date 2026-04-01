package com.procurement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * <p>
 * 专用于库存预警通知的小型固定线程池，避免推送 IO 阻塞业务主流程。
 * 拒绝策略：队列满时记录警告日志并丢弃（通知允许偶发丢失，9 点定时任务兜底）。
 * </p>
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "notifyExecutor")
    public Executor notifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notify-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler((r, e) ->
                log.warn("[Async] 库存预警通知队列已满，本次通知已丢弃（9 点定时任务将兜底补发）"));
        executor.initialize();
        return executor;
    }

    /**
     * 捕获 @Async 方法内未处理的异常并记录日志，防止静默吞掉错误。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[Async] 异步任务异常 method={} params={}", method.getName(), params, ex);
    }
}
