package com.gist.mathis.service.entity;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Data;

@Data
public abstract class MathisJob implements InitializingBean {
	String id;
	MathisJobTypeEnum type;
	CronTrigger trigger;
	Boolean enabled;
	Runnable task;
}