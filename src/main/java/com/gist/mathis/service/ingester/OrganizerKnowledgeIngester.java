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
		internalAiamIngestion();
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
	
	private void internalAiamIngestion() throws InterruptedException {
	    log.info("[{}][{}][{}] Start ingestion", getSourceName(), getClass().getSimpleName(), "AIAM");
	    String url = "https://www.aiam-musica.it/soci-elenco";
	    Document doc;
	    try {
	        doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
	    } catch (IOException e) {
	        log.error("{} -> {}", OrganizerKnowledgeIngester.class.getCanonicalName(), e.getMessage());
	        return;
	    }

	    Elements organizerCards = doc.select("article.qodef-e.qodef-grid-item");
	    int count = 0;
	    String country = "Italy";

	    for (Element card : organizerCards) {
	        Element titleLink = card.selectFirst("h6.qodef-e-title > a.qodef-e-title-link");
	        if (titleLink == null) continue;

	        String name = titleLink.text().trim();
	        String detailUrl = titleLink.absUrl("href");
	        Element regionEl = card.selectFirst(".qodef-e-info-category .qodef-e-category");
	        String region = (regionEl != null) ? regionEl.text().trim() : null;

	        log.info("AIAM Organizer: {} - URL: {} - Region: {}", name, detailUrl, region);

	        // --- Scarica pagina dettaglio ---
	        String description = null, address = null, city = null, email = null, website = null, tel = null, president = null, director = null;
	        String facebook = null, youtube = null, instagram = null, linkedin = null;

	        try {
	            Document detailDoc = Jsoup.connect(detailUrl).userAgent("Mozilla/5.0").timeout(10000).get();

	            // Descrizione
	            Element descrEl = detailDoc.selectFirst("div.qodef-portfolio-content > p");
	            if (descrEl != null) description = descrEl.text();

	            // Info blocchi
	            Elements infoBlocks = detailDoc.select("div.qodef-portfolio-info > div.qodef-e.qodef-info--info-items");
	            for (Element block : infoBlocks) {
	                String label = block.selectFirst(".qodef-e-title") != null
	                        ? block.selectFirst(".qodef-e-title").text().toLowerCase() : "";

	                Element valueEl = block.selectFirst(".qodef-e-info-item");
	                if (label.contains("sede") && valueEl != null) {
	                    address = valueEl.html().replaceAll("<br>", " ").trim();
	                    // Prova a separare città
	                    String[] arr = valueEl.text().split("\\d{5}");
	                    if (arr.length > 1) {
	                        city = arr[1].replaceAll("[^\\w ]", "").trim();
	                    }
	                } else if (label.contains("contatti") && valueEl != null) {
	                    // Email (può essere più di una, prendiamo la prima valida)
	                    Element emailLink = valueEl.selectFirst("a[href^=mailto:]");
	                    if (emailLink != null) {
	                        email = emailLink.text().trim();
	                    }
	                    // Tel
	                    Element telLink = valueEl.selectFirst("a[href^=tel:]");
	                    if (telLink != null) {
	                        tel = telLink.text().trim();
	                    }
	                } else if (label.contains("presidente") && valueEl != null) {
	                    president = valueEl.text().trim();
	                } else if (label.contains("direttore") && valueEl != null) {
	                    director = valueEl.text().trim();
	                } else if (label.contains("web e social") && valueEl != null) {
	                    Elements links = valueEl.select("a");
	                    for (Element l : links) {
	                        String href = l.attr("href");
	                        if (href.contains("academiamontisregalis.it") && website == null) {
	                            website = href;
	                        } else if (href.contains("facebook.com")) {
	                            facebook = href;
	                        } else if (href.contains("youtube.com") || href.contains("youtu.be")) {
	                            youtube = href;
	                        } else if (href.contains("instagram.com")) {
	                            instagram = href;
	                        } else if (href.contains("linkedin.com")) {
	                            linkedin = href;
	                        }
	                    }
	                }
	            }
	        } catch (IOException e) {
	            log.warn("Error loading detail for {}: {}", name, e.getMessage());
	        }

	        log.info("Organizer found: {} - Email: {} - Website: {} - Region: {}", name, email, website, region);

	        if (email == null || email.isEmpty()) {
	            log.info("E-mail is not present - skip raw knowledge");
	            continue;
	        }

	        RawKnowledge organizer;
	        String uniqueName = String.format("%s (%s)", name, country);
	        Optional<RawKnowledge> byName = rawKnowledgeRepository.findByNameAndSource(uniqueName, getSourceName());
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

	        organizer.setDescription("AIAM - Associato [" + name + "] " + (description != null ? description : ""));

	        organizer.getMetadata().put("name", name);
	        organizer.getMetadata().put("website", website);
	        organizer.getMetadata().put("country", country);
	        organizer.getMetadata().put("region", region);
	        organizer.getMetadata().put("city", city);
	        organizer.getMetadata().put("address", address);
	        organizer.getMetadata().put("email", email);
	        organizer.getMetadata().put("president", president);
	        organizer.getMetadata().put("director", director);
	        organizer.getMetadata().put("tel", tel);
	        organizer.getMetadata().put("facebook", facebook);
	        organizer.getMetadata().put("youtube", youtube);
	        organizer.getMetadata().put("instagram", instagram);
	        organizer.getMetadata().put("linkedin", linkedin);
	        organizer.getMetadata().put("detailUrl", detailUrl);

	        rawKnowledgeRepository.save(organizer);
	        count++;
	        log.info(String.format("Organizer saved: %d > %s", organizer.getId(), organizer.getName()));

	        Thread.sleep(1000);
	    }

	    log.info("[{}][{}][{}] End ingestion - {} RawKnowledge(s) ingested", getSourceName(),
	            getClass().getSimpleName(), "AIAM", count);
	}

}
