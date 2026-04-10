package com.gist.mathis.service.entity;

import org.springframework.scheduling.support.CronTrigger;

import lombok.Data;

@Data
public class MathisJob {
	String id;
	MathisJobTypeEnum type;
	CronTrigger trigger;
	Boolean enabled;
	Runnable task;
}