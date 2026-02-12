package com.gist.mathis.service.ingester;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "mathis.ingesters.festival")
public class FestivalKnowledgeIngesterProperties {
	private List<String> artDisciplines;
    private Integer maxPages;
}
