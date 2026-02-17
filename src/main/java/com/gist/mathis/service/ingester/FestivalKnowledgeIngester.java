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
	private FestivalKnowledgeIngesterProperties properties;
	
	@Autowired
	private RawKnowledgeRepository rawKnowledgeRepository;
	
	@Override
    public RawKnowledgeSourceEnum getSourceName() { 
		return RawKnowledgeSourceEnum.FESTIVAL; 
	}
	
	@Override
	public void ingest() throws InterruptedException{
		log.info("[{}][{}] Start ingestion",getSourceName(),getClass().getSimpleName());
		int count = 0;
		StringBuilder baseUrlBuilder = new StringBuilder("https://www.festivalfinder.eu/find-festival-organisations/p{page}?query=&country=&daterange=");
		for (String artDiscipline : properties.getArtDisciplines()) {
			baseUrlBuilder.append("&artDisciplines%5B%5D=").append(artDiscipline);
		}
		
		String baseUrl = baseUrlBuilder.toString();
		for (int page = 1; page <= properties.getMaxPages(); page++) {
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
				String artDisciplines = null;
				StringBuilder description = new StringBuilder();
				try {
					Document detailDoc = Jsoup.connect(detailUrl).userAgent("Mozilla/5.0").timeout(10000).get();

					// Descrizione del festival
					Element div = detailDoc.selectFirst("div.text--lead");
			        if (div != null) {
			            // Seleziona tutti i <p> dentro il div
			            Elements paragraphs = div.select("p");
			            for (Element p : paragraphs) {
			            	description.append(p.text()).append("\n");
			            }
			        }
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
					
					// Art Disciplines
					// Trova tutte le card-info
			        Elements detailsCards = detailDoc.select("div.card.card--light.card--info");
			        for (Element detailsCard : detailsCards) {
			            Element h3 = detailsCard.selectFirst("h3.spacer");
			            if (h3 != null && h3.text().equalsIgnoreCase("Art disciplines")) {
			                Element meta = detailsCard.selectFirst("div.card__meta");
			                if (meta != null) {
			                    artDisciplines = meta.text() != null ? meta.text().trim() : null;
			                    break;
			                }
			            }
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
				String uniqueName = String.format("%s (%s)",name,country);
				Optional<RawKnowledge> byName = rawKnowledgeRepository.findByNameAndSource(uniqueName, getSourceName());
				if(byName.isEmpty()) {
					festival = new RawKnowledge();
					festival.setSource(getSourceName());
					festival.setName(String.format("%s (%s)",name,country));
				} else {
					festival = byName.get();
					
					if(festival.getProcessedBy() != null) {
						log.info("Festival already processed by [%s]", festival.getProcessedBy().name());
						continue;
					}
					
					festival.getMetadata().clear();
					festival.setUpdatedAt(null);
				}

				festival.setDescription(description.toString());
				
				festival.getMetadata().put("name", name);
				festival.getMetadata().put("website", website);
				festival.getMetadata().put("country", country);
				festival.getMetadata().put("detailUrl", detailUrl);
				festival.getMetadata().put("email", email);
				festival.getMetadata().put("artDisciplines", artDisciplines);
				
				rawKnowledgeRepository.save(festival);
				count++;
				log.info(String.format("Festival saved: %d > %s", festival.getId(), festival.getName()));
				
				Thread.sleep(1000);
			}
		}
		log.info("[{}][{}] End ingestion - {} RawKnowledge(s) ingested",getSourceName(),getClass().getSimpleName(),count);
	}
	
}
