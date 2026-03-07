package rj.highlink.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * 线程池配置类
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {
    // 创建异步线程池
    @Bean("asyncLogExecutor")
    public ExecutorService asyncLogExecutor() {
        return new ThreadPoolExecutor(
                5,                      // 核心线程数
                10,                     // 最大线程数
                60L,                    // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), // 任务队列容量
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("async-log-thread-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理
        );
    }
    // 指定后面创建的线程池 都为 asyncLogExecutor
    @Override
    public Executor getAsyncExecutor() {
        return asyncLogExecutor();
    }
}

