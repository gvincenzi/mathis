package com.gist.mathis.service.ingester;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KnowledgeIngesterScheduler implements InitializingBean {

    @Autowired
    private List<KnowledgeIngester> ingesters;

    @Autowired
    private Environment env;

    @Autowired
    private TaskScheduler taskScheduler;

    @Override
    public void afterPropertiesSet() {
        ingesters.forEach(this::scheduleIfEnabled);
    }

    private void scheduleIfEnabled(KnowledgeIngester ingester) {
        IngesterScheduler annotation = ingester.getClass().getAnnotation(IngesterScheduler.class);
        if (annotation == null) return;

        String configKey = annotation.configKey();
        boolean enabled = Boolean.parseBoolean(env.getProperty(configKey + ".enabled", "false"));
        String cron = env.getProperty(configKey + ".ingestion-cron", "0 0 * * * *");

        if (enabled) {
            log.info("KnowledgeIngester enabling [{}][{}]", ingester.getSourceName(), ingester.getClass().getSimpleName());
            CronTrigger trigger = new CronTrigger(cron);
            Runnable task = () -> {
                try {
                    ingester.ingest();
                } catch (InterruptedException e) {
                    log.error("{} -> {}", KnowledgeIngesterScheduler.class.getCanonicalName(), e.getMessage());
                    Thread.currentThread().interrupt();
                }
            };
            taskScheduler.schedule(task, trigger);
        }
    }
}

