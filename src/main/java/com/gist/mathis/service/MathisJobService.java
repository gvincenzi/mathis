package com.gist.mathis.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.gist.mathis.service.entity.MathisJob;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MathisJobService {
	@Autowired
	private TaskScheduler taskScheduler;
	
	private Map<String, MathisJob> registeredJobs = new HashMap<String, MathisJob>();
	private Map<String, ScheduledFuture<?>> scheduledJobs = new HashMap<String, ScheduledFuture<?>>();
	
	public void register(MathisJob mathisJob) {
		if(!registeredJobs.containsKey(mathisJob.getId())){
			registeredJobs.put(mathisJob.getId(), mathisJob);
			if(mathisJob.getEnabled()) {
				enable(mathisJob.getId());
			}
		}
	}
	
	public void enable(String jobId) {
		if(registeredJobs.containsKey(jobId)){
			ScheduledFuture<?> futureTask = taskScheduler.schedule(registeredJobs.get(jobId).getTask(), registeredJobs.get(jobId).getTrigger());
			scheduledJobs.put(jobId, futureTask);
			
			registeredJobs.get(jobId).setEnabled(Boolean.TRUE);
			log.info("MathisJob enabled [{}][{}][{}]", registeredJobs.get(jobId).getType().name(), registeredJobs.get(jobId).getClass().getCanonicalName(), registeredJobs.get(jobId).getTrigger());
		}
	}
	
	public void disable(String jobId) {
		if(scheduledJobs.containsKey(jobId)){
			scheduledJobs.get(jobId).cancel(false);
			
			registeredJobs.get(jobId).setEnabled(Boolean.FALSE);
			log.info("MathisJob disabled [{}][{}][{}]", registeredJobs.get(jobId).getType().name(), registeredJobs.get(jobId).getClass().getCanonicalName(), registeredJobs.get(jobId).getTrigger());
		}
	}
	
	public Map<String, MathisJob> getRegisteredJobs() {
		return registeredJobs;
	}
}
