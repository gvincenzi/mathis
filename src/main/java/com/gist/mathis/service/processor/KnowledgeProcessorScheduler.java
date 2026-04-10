package com.gist.mathis.service.processor;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
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
public class KnowledgeProcessorScheduler implements InitializingBean {

	@Autowired
	private List<KnowledgeProcessor> processors;

	@Autowired
	private Environment env;

	@Autowired
	private MathisJobService mathisJobService;

	@Override
	public void afterPropertiesSet() {
		processors.forEach(this::scheduleIfEnabled);
	}

	private void scheduleIfEnabled(KnowledgeProcessor processor) {
		ProcessorScheduler annotation = processor.getClass().getAnnotation(ProcessorScheduler.class);
		if (annotation == null)
			return;
		
		MathisJob job = new MathisJob();
		job.setType(MathisJobTypeEnum.PROCESSOR);
		job.setId(annotation.configKey());

		String configKey = annotation.configKey();
		job.setEnabled(Boolean.parseBoolean(env.getProperty(configKey + ".enabled", "false")));
		
		String cron = env.getProperty(configKey + ".processor-cron", "0 0 * * * *");
		CronTrigger trigger = new CronTrigger(cron);
		job.setTrigger(trigger);

		log.info("Init MathisJob [{}][{}]", job.getType(), job.getClass().getCanonicalName());
		
		Runnable task = () -> {
			try {
				processor.process();
			} catch (InterruptedException e) {
				log.error("{} -> {}", KnowledgeProcessorScheduler.class.getCanonicalName(), e.getMessage());
				Thread.currentThread().interrupt();
			}
		};
		job.setTask(task);
		
		mathisJobService.register(job);
	}
}
