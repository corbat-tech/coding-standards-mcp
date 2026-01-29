package com.example.notification.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

/**
 * Configuration for coroutine dispatchers used in the notification service.
 */
@Configuration
class CoroutineConfig {

    /**
     * Default dispatcher for CPU-bound work
     */
    @Bean
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * IO dispatcher for I/O-bound work like network calls
     */
    @Bean
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Custom dispatcher for notification processing with limited parallelism
     */
    @Bean
    fun notificationDispatcher(): CoroutineDispatcher {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        ).asCoroutineDispatcher()
    }
}
