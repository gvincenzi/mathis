package com.gist.mathis.service.ingester;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.model.repository.RawKnowledgeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@IngesterScheduler(configKey = "mathis.ingesters.festival")
@Component
public class FestivalKnowledgeIngester implements KnowledgeIngester {
	@Autowired
	private RawKnowledgeRepository repo;
	
	@Override
    public RawKnowledgeSourceEnum getSourceName() { 
		return RawKnowledgeSourceEnum.FESTIVAL; 
	}
	
	@Override
	public void ingest(){
		log.info("[{}][{}] Start ingestion",getSourceName(),getClass().getSimpleName());
		int pages = 2, count = 0;
		String baseUrl = "https://www.festivalfinder.eu/find-festival-organisations/p{page}?query=&country=&daterange=&artDisciplines%5B%5D=electronic-music";
		for (int page = 1; page <= pages; page++) {
			String url = baseUrl.replace("{page}", page+"");
			Document doc;
			try {
				doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
			} catch (IOException e) {
				log.error("{} -> {}", FestivalKnowledgeIngester.class.getCanonicalName(), e.getMessage());
				continue;
			}

			Elements festivalCards = doc.select(".grid__item");
			for (Element card : festivalCards) {
				String name = card.select(".card__title").text();
				String detailUrl = card.select(".grid__item a").attr("href");
				if (!detailUrl.startsWith("http")) {
					detailUrl = "https://www.festivalfinder.eu" + detailUrl;
				}

				// Vai sulla pagina di dettaglio
				String website = null;
				String email = null;
				String country = null;
				try {
					Document detailDoc = Jsoup.connect(detailUrl).userAgent("Mozilla/5.0").timeout(10000).get();

					// Website: cerca il link con testo "Website"
					Elements links = detailDoc.select("a");
					for (Element link : links) {
						if (link.text().toLowerCase().contains("website")) {
							website = link.attr("href");
							break;
						}
					}
					// Email: cerca nel testo della pagina
					String pageText = detailDoc.text();
					Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
					Matcher matcher = emailPattern.matcher(pageText);
					if (matcher.find()) {
						email = matcher.group();
					}
					// Country: cerca il campo
					Elements countryElements = detailDoc.select(".u-mr-xsm");
					if (!countryElements.isEmpty()) {
						country = countryElements.text();
					}
				} catch (Exception e) {
					log.error("Errore parsing pagina festival: {}",detailUrl);
				}
				
				log.info("Festival founded: {} - {} - {} - {}", name, website, email, country);
				
				if(email == null || email == "") {
					log.info("E-mail is not present - skip raw knowledge");
					continue;
				}
				
				RawKnowledge festival;
				String externalId = String.format("%s_%s", name,email);
				Optional<RawKnowledge> byExternalId = repo.findByExternalId(externalId);
				if(byExternalId.isEmpty()) {
					festival = new RawKnowledge();
					festival.setSource(getSourceName());
					festival.setExternalId(externalId);
				} else {
					festival = byExternalId.get();
					festival.getMetadata().clear();
					festival.setUpdatedAt(null);
				}

				festival.getMetadata().put("name", name);
				festival.getMetadata().put("website", website);
				festival.getMetadata().put("country", country);
				festival.getMetadata().put("detailUrl", detailUrl);
				festival.getMetadata().put("email", email);
				repo.save(festival);
				count++;
				log.info(String.format("Festival saved: %d > %s", festival.getId(), festival.getExternalId()));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.error("{} -> {}", FestivalKnowledgeIngester.class.getCanonicalName(), e.getMessage());
				}
			}
		}
		log.info("[{}][{}] End ingestion - {} RawKnowledge(s) ingested",getSourceName(),getClass().getSimpleName(),count);
	}
	
}
