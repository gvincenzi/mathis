package com.gist.mathis.service.ingester;

import java.io.IOException;
import java.util.Optional;

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
@IngesterScheduler(configKey = "mathis.ingesters.organizer")
@Component
public class OrganizerKnowledgeIngester implements KnowledgeIngester {
	@Autowired
	private RawKnowledgeRepository rawKnowledgeRepository;

	@Override
	public RawKnowledgeSourceEnum getSourceName() {
		return RawKnowledgeSourceEnum.ORGANIZER;
	}

	@Override
	public void ingest() throws InterruptedException {
		internalAssomusicaIngestion();
		internalAssoconcertiIngestion();
	}

	private void internalAssomusicaIngestion() throws InterruptedException {
		log.info("[{}][{}][{}] Start ingestion", getSourceName(), getClass().getSimpleName(), "ASSOMUSICA");
		String url = "https://assomusica.org/it/i-nostri-associati/le-aziende.html";
		Document doc;
		try {
			doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
		} catch (IOException e) {
			log.error("{} -> {}", OrganizerKnowledgeIngester.class.getCanonicalName(), e.getMessage());
			return;
		}

		// Seleziona tutti gli elementi <li> che sono figli di un <ul> con le classi
		// specificate
		Elements associateCards = doc.select("ul.category-module.mod-list li");
		String baseUrl = "https://assomusica.org"; // URL base per costruire i link completi
		String country = "Italy";
		int count = 0;
		for (Element card : associateCards) {
			// All'interno di ogni <li>, cerca il tag <a> con la classe
			// 'mod-articles-category-title'
			Element linkElement = card.selectFirst("a.mod-articles-category-title");

			if (linkElement != null) {
				String name = linkElement.text().trim(); // Estrae il testo e rimuove spazi bianchi
				String relativeLink = linkElement.attr("href"); // Estrae l'attributo href

				// Costruisci l'URL completo
				String detailUrl = baseUrl + relativeLink;

				log.info("Associate: {} - URL: {}", name, detailUrl);

				String address = null, email = null, website = null;

				// Scarica la pagina di dettaglio dell'azienda
				try {
					Document detailDoc = Jsoup.connect(detailUrl).userAgent("Mozilla/5.0").timeout(10000).get();

					Elements blocks = detailDoc
							.select("span[itemprop=articleBody] > div, span[itemprop=articleBody] > p");
					for (Element block : blocks) {
						// Cerca indirizzo (contiene solitamente un CAP)
						String text = block.text();
						if (address == null && text.matches(".*\\d{4,5}.*")) {
							address = text;
						}
						// Cerca email
						Element mailLink = block.selectFirst("a[href^=mailto:]");
						if (mailLink != null) {
							email = mailLink.text();
						}
						// Cerca sito web
						Element webLink = block.selectFirst("a[href^=http]:not([href^=mailto:])");
						if (webLink != null) {
							website = webLink.attr("href");
						}
					}
				} catch (IOException e) {
					log.warn("Error for {}: {}", name, e.getMessage());
				}

				log.info("Organizer founded: {} - {} - {} - {}", name, website, email, country);

				if (email == null || email == "") {
					log.info("E-mail is not present - skip raw knowledge");
					continue;
				}

				RawKnowledge organizer;
				String uniqueName = String.format("%s (%s)", name, country);
				Optional<RawKnowledge> byName = rawKnowledgeRepository.findByNameAndSource(uniqueName, getSourceName());
				if (byName.isEmpty()) {
					organizer = new RawKnowledge();
					organizer.setSource(getSourceName());
					organizer.setName(String.format("%s (%s)", name, country));
				} else {
					organizer = byName.get();

					if (organizer.getProcessedBy() != null) {
						log.info("Festival already processed by [%s]", organizer.getProcessedBy().name());
						continue;
					}

					organizer.getMetadata().clear();
					organizer.setUpdatedAt(null);
				}

				organizer.setDescription(String.format("AssoMusica - Associato [%s][%s]", name, address));

				organizer.getMetadata().put("name", name);
				organizer.getMetadata().put("website", website);
				organizer.getMetadata().put("country", country);
				organizer.getMetadata().put("detailUrl", detailUrl);
				organizer.getMetadata().put("email", email);

				rawKnowledgeRepository.save(organizer);
				count++;
				log.info(String.format("Organizer saved: %d > %s", organizer.getId(), organizer.getName()));

				Thread.sleep(1000);
			}
		}

		log.info("[{}][{}][{}] End ingestion - {} RawKnowledge(s) ingested", getSourceName(),
				getClass().getSimpleName(), "ASSOMUSICA", count);
	}

	private void internalAssoconcertiIngestion() throws InterruptedException {
		log.info("[{}][{}][{}] Start ingestion", getSourceName(), getClass().getSimpleName(), "ASSOCONCERTI");
		String url = "https://assoconcerti.it/gli-associati/";
		Document doc;
		try {
			doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
		} catch (IOException e) {
			log.error("{} -> {}", OrganizerKnowledgeIngester.class.getCanonicalName(), e.getMessage());
			return;
		}

		// Seleziona tutte le regioni dentro l'accordion
		Elements accordionItems = doc.select("ul.grve-accordion-wrapper > li");
		int count = 0;
		String country = "Italy";

		for (Element regionItem : accordionItems) {
			// Trova il nome della regione
			String region = regionItem.selectFirst("h3.grve-title.grve-h6").text().trim();

			// Trova il blocco degli associati di questa regione
			Element content = regionItem
					.selectFirst("div.grve-accordion-content > div.grve-element.grve-text.grve-subtitle");
			if (content == null)
				continue;

			// Trova tutti i blocchi <h3> (nome) e <p> (dettagli) alternati
			Elements children = content.children();
			String name = null;
			String address = null;
			String email = null;
			String website = null;

			for (Element child : children) {
				if (child.tagName().equals("h3")) {
					// Inizia una nuova azienda: reset variabili
					name = child.text().replaceAll("^\\s*\\*+\\s*", "")
							.replaceAll("^(\\s*<strong>)|(</strong>\\s*)$", "").trim();
					address = null;
					email = null;
					website = null;
				} else if (child.tagName().equals("p")) {
					String text = child.text().trim();

					// Estrai indirizzo (tutto il testo prima di eventuali email/sito)
					address = text.split("–")[0].trim(); // Prova a prendere solo la prima parte (indirizzo)
					// Se indirizzo contiene il CAP (XXXX o XXXXX)
					if (!address.matches(".*\\d{4,5}.*")) {
						address = text;
					}

					// Estrai email
					Element mailLink = child.selectFirst("a[href^=mailto:]");
					if (mailLink != null) {
						email = mailLink.text().trim();
					} else {
						// Cerca email nel testo
						String mail = IngesterUtil.extractEmail(text);
						if (mail != null)
							email = mail.trim();
					}

					// Estrai sito web
					Element webLink = child.selectFirst("a[href^=http]:not([href^=mailto:])");
					if (webLink != null) {
						website = webLink.attr("href").trim();
					} else {
						// Cerca "www." nel testo
						String site = IngesterUtil.extractWebsite(text);
						if (site != null)
							website = site.trim();
					}

					log.info("Associate: {} - Email: {} - Website: {} - Region: {}", name, email, website, region);

					// Solo se c'è una email, salva
					if (email == null || email.isEmpty()) {
						log.info("E-mail is not present - skip raw knowledge");
						continue;
					}

					RawKnowledge organizer;
					String uniqueName = String.format("%s (%s)", name, country);
					Optional<RawKnowledge> byName = rawKnowledgeRepository.findByNameAndSource(uniqueName,
							getSourceName());
					if (byName.isEmpty()) {
						organizer = new RawKnowledge();
						organizer.setSource(getSourceName());
						organizer.setName(uniqueName);
					} else {
						organizer = byName.get();
						if (organizer.getProcessedBy() != null) {
							log.info("Organizer already processed by [{}]", organizer.getProcessedBy().name());
							continue;
						}
						organizer.getMetadata().clear();
						organizer.setUpdatedAt(null);
					}

					organizer.setDescription(
							String.format("AssoConcerti - Associato [%s][%s][%s]", name, region, address));

					organizer.getMetadata().put("name", name);
					organizer.getMetadata().put("website", website);
					organizer.getMetadata().put("country", country);
					organizer.getMetadata().put("region", region);
					organizer.getMetadata().put("address", address);
					organizer.getMetadata().put("email", email);

					rawKnowledgeRepository.save(organizer);
					count++;
					log.info(String.format("Organizer saved: %d > %s", organizer.getId(), organizer.getName()));

					Thread.sleep(1000);
				}
			}
		}

		log.info("[{}][{}][{}] End ingestion - {} RawKnowledge(s) ingested", getSourceName(),
				getClass().getSimpleName(), "ASSOCONCERTI", count);
	}
}
