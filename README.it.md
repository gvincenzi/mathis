# Mathis - Un agente virtuale per il knowledge management personale, conversazionale e open source
<img src="src/main/resources/static/images/logo.jpg" width="200">
<img src="src/main/resources/static/images/mathis.png" width="200">

[![en](https://img.shields.io/badge/lang-en-green.svg)](https://github.com/gvincenzi/mathis/blob/master/README.md)
[![fr](https://img.shields.io/badge/lang-fr-red.svg)](https://github.com/gvincenzi/mathis/blob/master/README.fr.md)

## Descrizione

Mathis è un progetto open source scritto in Java che implementa un agente software conversazionale tramite Telegram, dotato di una base di dati relazionale per la gestione degli utenti, di una base documentale di conoscenza, e di funzionalità RAG (Retrieval-Augmented Generation) per la ricerca nei documenti.  
Il codice si basa sulle seguenti librerie e tecnologie:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Apache Tika](https://tika.apache.org/)
- [Supabase](https://supabase.com/)
- [MistralAI](https://mistral.ai/) 

Lo scopo è permettere agli utenti di caricare documenti (PDF, Excel, ecc.) nella base documentale, e interrogare questa base di conoscenza attraverso uno scambio di messaggi in linguaggio naturale con il bot.

## Tecnologie e librerie utilizzate

Mathis è costruito con **Java 17** e si basa su [Spring Boot](https://spring.io/projects/spring-boot).  
L'integrazione con **Telegram** è realizzata tramite le librerie [`telegrambots-springboot-longpolling-starter`](https://github.com/rubenlagus/TelegramBots) e `telegrambots-client`, che permettono la comunicazione con gli utenti tramite bot.
L'interfaccia di gestione della base di conoscenza è realizzata con **Thymeleaf**. 

Per la componente LLM e RAG (Retrieval-Augmented Generation), Mathis utilizza [Spring AI](https://spring.io/projects/spring-ai) con il modulo `spring-ai-starter-model-mistral-ai` per accedere ai modelli di MistralAI.  
L'archiviazione e la gestione vettoriale dei documenti è affidata a **PgVector**, tramite `spring-ai-starter-vector-store-pgvector` e `spring-ai-advisors-vector-store`, che facilitano la ricerca semantica dei contenuti.  
L'estrazione automatica del testo dai documenti caricati è gestita da **Apache Tika** tramite la libreria `spring-ai-tika-document-reader`.

La persistenza e la sincronizzazione delle note e dei documenti con **Supabase** vengono gestite attraverso l'integrazione con Postgres e PgVector. 

La scelta di queste tecnologie mira a rendere il progetto Mathis una soluzione robusta, scalabile e facilmente estendibile per la gestione intelligente dei documenti e delle interazioni AI.

## Funzionalità

- **Conversazione naturale**: Interazione tramite chat Telegram o direttamente nella homepage della webapp con l'agente Mathis.
- **Gestione del salvataggio degli utenti e di una passwaord di default**: Mathis stocca nella base relazionale gli utenti ed una password temporanea (in questo prototipo la gestione della password non è implementata), e solo dopo interagisce con il resto del sistema.
- **Riconoscimento dell'intento dell’utente e delle sue autorizzazioni**: Mathis interpreta l’intento dell’utente e reagisce secondo una lista predefinita di azioni possibili, verificando eventualmente se l'utente ha i diritti (Spring Security Authorities).
- **Caricamento documenti**: Puoi caricare documenti via l'interfaccia web; Mathis li analizza tramite Tika, ne estrae il testo rilevante, e aggiorna il database di conseguenza. Vengono stoccati in una tabella a parte, non vettoriale, la lista dei documenti e gli indirizzi nel web da dove poterli eventualmente scaricare.
- **Indicizzazione e storage**: I contenuti estratti vengono archiviati e indicizzati su Supabase.
- **RAG (Retrieval-Augmented Generation)**: Quando si pongono domande al bot, Mathis recupera i documenti rilevanti dalla base di conoscenza e genera risposte contestualizzate usando MistralAI.
- **Open Source**: Il progetto è completamente open, facilmente estendibile e personalizzabile ed è derivato da [Mathis](https://github.com/gvincenzi/mathis).

## Architettura

<img src="src/main/resources/static/images/schema.png" width="100%">

## Come funziona

Il funzionamento di Mathis si articola in due flussi principali, entrambi avviati dall'utente tramite il **Telegram Bot** o la **Mathis WebApp**, e gestiti centralmente dalla **Mathis API**.

Quando un utente **carica un documento** (come PDF, PowerPoint o Excel), la Mathis API riceve la richiesta. Il documento viene elaborato da **Apache Tika** per l'estrazione del testo. Questo testo viene poi sottoposto a "chunking" (suddivisione in blocchi) e "embedding" (conversione in vettori numerici) prima di essere salvato nella **base vettoriale di Supabase**. Una volta completato il salvataggio, l'utente riceve una conferma.

Quando un utente **invia un messaggio testuale** (una domanda o una richiesta), la Mathis API intercetta il messaggio. Il sistema procede con il riconoscimento dell'intento dell'utente e la verifica del suo ruolo tramite Spring Security.  L'intento guida il sistema a scegliere una delle azioni previste :

```java
package com.gist.mathis.service.entity;

public enum Intent {
	LIST_DOCUMENTS,
	ASK_FOR_DOCUMENT,
   NOTIFY_ADMIN,
   GENERIC_QUESTION
}
```

- Nel caso di *LIST_DOCUMENTS* non farà che effettuare una ricerca nel database relazionale.
- Nel caso di *ASK_FOR_DOCUMENT* farà una ricarca direttamente nel database ma di tipo vettoriale.
- Nel caso di *USER_MAIL_SENT*, l’indirizzo email sarà salvato nella memoria della chat (cfr. MathisChatMemoryRepository).
- Nel caso di *NOTIFY_ADMIN*, viene inviato un messaggio a tutti gli account Telegram degli utenti ADMIN con un riepilogo della conversazione salvata nella memoria della chat (cfr. MathisChatMemoryRepository) e l’email ricevuta (nello stesso messaggio oppure in un USER_MAIL_SENT precedente).
- Nel caso della *GENERIC_QUESTION* dovrà effettuare una ricerca nel contenuto dei documenti, e sfrutterà tutta la catena del RAG per interpretare la domanda, arricchire il contesto, e generare una risposta. 

Questa ricerca avviene nella **base vettoriale di Supabase**, dove vengono recuperate le informazioni più rilevanti per la query dell'utente. Le informazioni recuperate, insieme alla query originale, vengono utilizzate per costruire un prompt arricchito di contesto, che viene poi inviato a un **Large Language Model** (come MistralAI). 
Il modello genera una "Risposta" basata sul contesto fornito, che viene infine inviata all'utente.

## Implementazione della memoria conversazionale

Mathis dispone di un'architettura modulare per la gestione della memoria della chat, che permette di memorizzare sia la cronologia delle conversazioni sia dati contestuali aggiuntivi per ogni conversazione.

La memoria della chat è implementata tramite queste classi:

- **InMemoryMathisChatMemoryRepository**: Memorizza i messaggi delle conversazioni e oggetti arbitrari (come indirizzi email, ruoli utente) in memoria, indicizzati per ID di conversazione.
  - Offre metodi per salvare, recuperare e cancellare liste di messaggi, oltre a memorizzare e recuperare oggetti tramite chiavi personalizzate.
  - Esegue periodicamente la pulizia delle conversazioni scadute tramite la classe `ChatMemoryCleaner`, in base ai tempi di scadenza e intervalli configurabili.

- **MathisMessageWindowChatMemory**: Gestisce una finestra di memoria che conserva fino a un numero configurabile di messaggi per conversazione (default: 20).
  - Quando vengono aggiunti nuovi messaggi, li unisce a quelli già in memoria, mantiene solo i messaggi più recenti e gestisce i messaggi di sistema in modo dedicato per evitare duplicati.
  - Permette anche di memorizzare e recuperare oggetti arbitrari (es. email utente, ruolo) tramite chiavi tipizzate.

- **MathisChatMemoryObjectKeyEnum**: Enumera le chiavi per la memorizzazione di oggetti contestuali aggiuntivi, come `USER_MAIL` e `USER_ROLE`.

- **MathisChatMemoryProperties**: Gestisce parametri configurabili per la scadenza della memoria chat e la pianificazione della pulizia.

Questa architettura consente a Mathis di gestire in modo efficiente il contesto conversazionale, gli attributi utente e i metadati di sistema, abilitando funzionalità avanzate come RAG e riconoscimento delle intenzioni.

**Esempio:**
- Quando un utente invia una mail (*USER_MAIL_SENT*), l’indirizzo viene salvato nella memoria della chat.
- Quando gli admin sono notificati (*NOTIFY_ADMIN*), il riepilogo della conversazione e i dati rilevanti (email, ecc.) vengono recuperati dalla memoria e inviati.

## REST API

Mathis mette a disposizione anche una API per gestire la la base di conoscenza e interagire con la funzionalità di chat.

### `/api/admin/knowledge` (POST)
Carica un nuovo documento nella knowledge base.  
**Parametri (query):**
- `title` (string, obbligatorio): Titolo del documento.
- `description` (string, obbligatorio): Descrizione del documento.
- `url` (string, obbligatorio): URL di origine del documento.  
**Request Body:**  
- `document` (binary, obbligatorio): Il file da caricare (multipart/form-data).  
**Risposta:**  
- `200 OK` in caso di successo.

### `/api/admin/knowledge` (DELETE)
Elimina un documento dalla knowledge base.  
**Parametri (query):**
- `knowledgeId` (integer, obbligatorio): ID del documento da eliminare.  
**Risposta:**  
- `200 OK` in caso di successo.

### `/api/chat` (POST)
Invia un messaggio in chat e ricevi una risposta.  
**Request Body:**  
- Oggetto JSON di tipo `ChatMessage`, che include:
  - `conversationId` (string): ID della conversazione.
  - `userType` (string, enum: HUMAN, AI): Tipo di utente.
  - `userAuth` (string, enum: ROLE_USER, ROLE_ADMIN): Ruolo di autorizzazione dell'utente.
  - `body` (string): Contenuto del messaggio.
  - `inlineKeyboardMarkup` (oggetto, opzionale): Inline keyboard per Telegram.
  - `resource` (binary, opzionale): Risorsa allegata.  
**Risposta:**  
- `200 OK` con un oggetto `ChatMessage`.


### Documentazione API

La documentazione completa dell'API, generata automaticamente in formato OpenAPI (Swagger UI), è disponibile all'indirizzo:
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

*(Nota: L'indirizzo `localhost:8080` si riferisce all'esecuzione locale del servizio. In un ambiente di produzione, l'URL sarà quello del server su cui è deployato Mathis BlocNotes.)*

## Requisiti

- Java 17+
- Un account Telegram e token BotFather
- Un'istanza Supabase configurata
- API key MistralAI

## Installazione

1. **Clona il repository**
   ```bash
   git clone https://github.com/tuo-username/mathis_blocnotes.git
   cd Mathis
   ```

2. **Configura le variabili ambiente e il file di configurazione YML**
   - `TELEGRAM_BOT_TOKEN`
   - `SUPABASE_URL` e `SUPABASE_KEY`
   - `MISTRAL_API_KEY`

3. **Compila ed esegui**
   ```bash
   ./mvnw spring-boot:run
   ```

## Utilizzo

- **/start**: (opzionale) Avvia la conversazione con Mathis ottenendo una presentazione dell’agente nella lingua dell’account Telegram.

## Contribuire

Contribuzioni, segnalazioni di bug e proposte di nuove funzionalità sono benvenute! Apri una issue o un pull request.

## Licenza

Questo progetto è distribuito sotto licenza GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007 (GPLv3).