package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@Configuration
@ComponentScan( { "com.example" } )
@EnableScheduling
public class SpringConfig extends AnnotationConfigWebApplicationContext implements AsyncConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger( SpringConfig.class );

    @Bean
    public TaskScheduler poolScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix( "poolScheduler" );
        scheduler.setPoolSize( 10 );
        scheduler.setErrorHandler( t -> LOG.error( "Error in scheduled task", t ) );
        return scheduler;
    }

}
