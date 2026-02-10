# Mathis - A virtual conversational agent for Personal Knowledge Management
<img src="src/main/resources/static/images/logo.jpg" width="200">
<img src="src/main/resources/static/images/mathis.png" width="200">

[![it](https://img.shields.io/badge/lang-it-blue.svg)](https://github.com/gvincenzi/mathis/blob/master/README.it.md)
[![fr](https://img.shields.io/badge/lang-fr-red.svg)](https://github.com/gvincenzi/mathis/blob/master/README.fr.md)

## Description

Mathis is an open source project written in Java that implements a conversational software agent via Telegram, featuring a relational user database, a document-based knowledge base, and Retrieval-Augmented Generation (RAG) capabilities for searching documents.  
The code is based on the following libraries and technologies:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Apache Tika](https://tika.apache.org/)
- [Supabase](https://supabase.com/)
- [MistralAI](https://mistral.ai/) 

The goal is to allow users to upload documents (PDF, Excel, etc.) to the knowledge base and query it through natural language exchanges with the bot.

## Technologies and Libraries Used

Mathis is built with **Java 17** and based on [Spring Boot](https://spring.io/projects/spring-boot).  
Integration with **Telegram** is handled using the [`telegrambots-springboot-longpolling-starter`](https://github.com/rubenlagus/TelegramBots) and `telegrambots-client` libraries, enabling user communication via bot.
The knowledge base management interface is built with **Thymeleaf**.

For LLM and RAG (Retrieval-Augmented Generation), Mathis uses [Spring AI](https://spring.io/projects/spring-ai) with the `spring-ai-starter-model-mistral-ai` module to access MistralAI models.  
Document storage and vector management are handled via **PgVector**, using `spring-ai-starter-vector-store-pgvector` and `spring-ai-advisors-vector-store`, which enable semantic search on content.  
Automatic text extraction from uploaded documents is managed by **Apache Tika** through the `spring-ai-tika-document-reader` library.

Persistence and synchronization of notes and documents with **Supabase** are managed via integration with Postgres and PgVector.

The choice of these technologies aims to make Mathis a robust, scalable, and easily extensible solution for intelligent document and AI interaction management.

## Features

- **Natural Conversation**: Interact with the Mathis agent via Telegram chat or directly on the webapp homepage.
- **User Storage and Default Password Management**: Mathis stores users and a temporary password in the relational database (note: password management is not implemented in this prototype); only after registration does interaction with the system proceed.
- **Intent Recognition and Authorization**: Mathis interprets user intent and responds based on a predefined list of possible actions, optionally verifying user permissions (Spring Security Authorities).
- **Document Upload**: Upload documents through the web interface; Mathis analyzes them using Tika, extracts relevant text, and updates the database accordingly. Documents and their web download links are stored in a separate (non-vector) table.
- **Indexing and Storage**: Extracted content is stored and indexed on Supabase.
- **RAG (Retrieval-Augmented Generation)**: When you ask questions to the bot, Mathis retrieves relevant documents from the knowledge base and generates contextualized answers using MistralAI.
- **Open Source**: The project is fully open, easily extensible and customizable, and is derived from [Mathis](https://github.com/gvincenzi/mathis).

## Architecture

<img src="src/main/resources/static/images/schema.png" width="100%">

## How It Works

Mathis operates through two main workflows, both initiated by the user via the **Telegram Bot** or the **Mathis WebApp**, and managed centrally by the **Mathis API**.

When a user **uploads a document** (PDF, PowerPoint, Excel, etc.), the Mathis API receives the request. The document is processed by **Apache Tika** for text extraction. This text is then chunked and embedded (converted into numeric vectors) before being saved in the **Supabase vector store**. Once the process is complete, the user receives a confirmation.

When a user **sends a text message** (question or request), the Mathis API intercepts the message. The system performs intent recognition and user role verification using Spring Security. The intent guides the system to select one of the predefined actions:

```java
package com.gist.mathis.service.entity;

public enum Intent {
	LIST_DOCUMENTS,
	ASK_FOR_DOCUMENT,
	USER_MAIL_SENT,
	NOTIFY_ADMIN,
   GENERIC_QUESTION
}
```

- For *LIST_DOCUMENTS*, a search is performed in the relational database.
- For *ASK_FOR_DOCUMENT*, a search is performed directly in the vector database.
- For *ASK_FOR_DOCUMENT*, a search is performed directly in the vector database.
- For *USER_MAIL_SENT*, the email address will be saved in chat memory (cf. MathisChatMemoryRepository).
- For *NOTIFY_ADMIN*, a message is sent to all ADMIN user's Telegram accounts with a summary of the conversation saved in chat memory (cf. MathisChatMemoryRepository) and the email received (in the same message or in an USER_MAIL_SENT before).
- For *GENERIC_QUESTION*, a search is performed within the content of documents, leveraging the full RAG pipeline to interpret the query, enrich the context, and generate an answer.

This search takes place in the **Supabase vector store**, where the most relevant information is retrieved for the user's query. The retrieved information, together with the original query, is used to construct a context-enriched prompt, which is then sent to a **Large Language Model** (such as MistralAI).  
The model generates a "Response" based on the provided context, which is then sent back to the user.

## Chat Memory Implementation

Mathis features a modular chat memory architecture that allows the system to manage both the conversational history and additional contextual data for each conversation.

The chat memory is implemented via the following classes:

- **InMemoryMathisChatMemoryRepository**: Stores conversation messages and arbitrary objects (such as email addresses, user roles) in memory, indexed by conversation ID.  
  - Offers methods for saving, retrieving, and deleting message lists, as well as storing and retrieving objects with custom keys.
  - Periodically purges expired conversations using the `ChatMemoryCleaner` class, based on configurable expiration times and cleanup intervals.

- **MathisMessageWindowChatMemory**: Provides a windowed memory mechanism that stores up to a configurable number of messages per conversation (default: 20).
  - When new messages are added, it merges them with existing memory, ensures only the most recent messages are retained, and handles system messages specially to avoid duplications.
  - Also allows storing and retrieving arbitrary objects (e.g., user email, role) via typed keys.

- **MathisChatMemoryObjectKeyEnum**: Enumerates keys for storing additional contextual objects in memory, such as `USER_MAIL` and `USER_ROLE`.

- **MathisChatMemoryProperties**: Provides configurable parameters for chat memory expiration and cleanup scheduling.

This architecture ensures that Mathis can efficiently manage conversational context, user attributes, and system metadata, supporting advanced features like RAG and intent recognition.

**Example:**
- When a user sends an email (*USER_MAIL_SENT*), the email address is saved in chat memory.
- When admins are notified (*NOTIFY_ADMIN*), a summary of the conversation and relevant data (email, etc.) are retrieved from chat memory and sent.

## REST API

Mathis also provides an API for managing the knowledge base and interacting with the chat functionality.

### `/api/admin/knowledge` (POST)
Upload a new document to the knowledge base.  
**Query Parameters:**
- `title` (string, required): Document title.
- `description` (string, required): Document description.
- `url` (string, required): Document source URL.  
**Request Body:**  
- `document` (binary, required): The file to upload (multipart/form-data).  
**Response:**  
- `200 OK` on success.

### `/api/admin/knowledge` (DELETE)
Delete a document from the knowledge base.  
**Query Parameters:**
- `knowledgeId` (integer, required): ID of the document to delete.  
**Response:**  
- `200 OK` on success.

### `/api/chat` (POST)
Send a chat message and receive a response.  
**Request Body:**  
- JSON object of type `ChatMessage`, including:
  - `conversationId` (string): Conversation ID.
  - `userType` (string, enum: HUMAN, AI): User type.
  - `userAuth` (string, enum: ROLE_USER, ROLE_ADMIN): User authorization role.
  - `body` (string): Message content.
  - `inlineKeyboardMarkup` (object, optional): Inline keyboard for Telegram.
  - `resource` (binary, optional): Attached resource.  
**Response:**  
- `200 OK` with a `ChatMessage` object.


### API Documentation

The full API documentation, automatically generated in OpenAPI (Swagger UI) format, is available at:
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

*(Note: The `localhost:8080` address refers to local service execution. In a production environment, the URL will be that of the server where Mathis BlocNotes is deployed.)*

## Requirements

- Java 17+
- Telegram account and BotFather token
- Configured Supabase instance
- MistralAI API key

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/mathis_blocnotes.git
   cd Mathis
   ```

2. **Configure environment variables and YML configuration file**
   - `TELEGRAM_BOT_TOKEN`
   - `SUPABASE_URL` and `SUPABASE_KEY`
   - `MISTRAL_API_KEY`

3. **Build and run**
   ```bash
   ./mvnw spring-boot:run
   ```

## Usage

- **/start**: (optional) Starts a conversation with Mathis, receiving an introduction in the language of your Telegram account.

## Contributing

Contributions, bug reports, and feature requests are welcome! Open an issue or pull request.

## License

This project is distributed under the GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007 (GPLv3).
