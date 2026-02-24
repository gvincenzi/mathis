package com.gist.mathis.service.ingester;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.gist.mathis.service.MathisJobService;
import com.gist.mathis.service.entity.MathisJob;
import com.gist.mathis.service.entity.MathisJobTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KnowledgeIngesterScheduler extends MathisJob {

	@Autowired
	private List<KnowledgeIngester> ingesters;

	@Autowired
	private Environment env;

	@Autowired
	private MathisJobService mathisJobService;

	@Override
	public void afterPropertiesSet() {
		ingesters.forEach(this::init);
	}

	private void init(KnowledgeIngester ingester) {
		IngesterScheduler annotation = ingester.getClass().getAnnotation(IngesterScheduler.class);
		if (annotation == null)
			return;

		this.setType(MathisJobTypeEnum.INGESTER);
		this.setId(annotation.configKey());
		
		String configKey = annotation.configKey();
		this.setEnabled(Boolean.parseBoolean(env.getProperty(configKey + ".enabled", "false")));

		String cron = env.getProperty(configKey + ".ingestion-cron", "0 0 * * * *");
		CronTrigger trigger = new CronTrigger(cron);
		this.setTrigger(trigger);

		log.info("Init MathisJob [%s][%s]", this.getType(), this.getClass().getCanonicalName());
		
		Runnable task = () -> {
			try {
				ingester.ingest();
			} catch (InterruptedException e) {
				log.error("{} -> {}", KnowledgeIngesterScheduler.class.getCanonicalName(), e.getMessage());
				Thread.currentThread().interrupt();
			}
		};
		this.setTask(task);
		
		mathisJobService.register(this);
	}
}
