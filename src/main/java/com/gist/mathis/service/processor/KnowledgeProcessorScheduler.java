package com.gist.mathis.service.processor;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KnowledgeProcessorScheduler implements InitializingBean {

    @Autowired
    private List<KnowledgeProcessor> processors;

    @Autowired
    private Environment env;

    @Override
    public void afterPropertiesSet() {
    	processors.forEach(this::scheduleIfEnabled);
    }

    private void scheduleIfEnabled(KnowledgeProcessor processor) {
        ProcessorScheduler annotation = processor.getClass().getAnnotation(ProcessorScheduler.class);
        if (annotation == null) return;

        String configKey = annotation.configKey();
        boolean enabled = Boolean.parseBoolean(env.getProperty(configKey + ".enabled", "false"));
        String cron = env.getProperty(configKey + ".processor-cron", "0 0 * * * *");

        if (enabled) {
        	log.info("KnowledgeProcessor enabling [{}]",processor.getClass().getSimpleName());
            CronTrigger trigger = new CronTrigger(cron);
            Runnable task = () -> {
            	try {
					processor.process();
				} catch (InterruptedException e) {
					log.error("{} -> {}", KnowledgeProcessorScheduler.class.getCanonicalName(), e.getMessage());
				}
            };
            TaskScheduler springScheduler = new ThreadPoolTaskScheduler();
            ((ThreadPoolTaskScheduler) springScheduler).initialize();
            springScheduler.schedule(task, trigger);
        }
    }
}

