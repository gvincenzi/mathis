# Mathis - Un agent conversationnel open source pour la gestion personnelle de la connaissance
<img src="src/main/resources/static/images/logo.jpg" width="200">
<img src="src/main/resources/static/images/mathis.png" width="200">

[![en](https://img.shields.io/badge/lang-en-green.svg)](https://github.com/gvincenzi/mathis/blob/master/README.md)
[![it](https://img.shields.io/badge/lang-it-blue.svg)](https://github.com/gvincenzi/mathis/blob/master/README.it.md)

## Description

Mathis est un projet open source écrit en Java qui implémente un agent logiciel conversationnel via Telegram, doté d'une base de données relationnelle pour la gestion des utilisateurs, d'une base documentaire de connaissances, et de fonctionnalités RAG (Retrieval-Augmented Generation) pour la recherche dans les documents.  
Le code repose sur les bibliothèques et technologies suivantes :

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Apache Tika](https://tika.apache.org/)
- [Supabase](https://supabase.com/)
- [MistralAI](https://mistral.ai/) 

L'objectif est de permettre aux utilisateurs de télécharger des documents (PDF, Excel, etc.) dans la base de connaissances et d'interroger cette base via des échanges en langage naturel avec le bot.

## Technologies et bibliothèques utilisées

Mathis est développé en **Java 17** et s’appuie sur [Spring Boot](https://spring.io/projects/spring-boot).  
L’intégration avec **Telegram** est réalisée avec les bibliothèques [`telegrambots-springboot-longpolling-starter`](https://github.com/rubenlagus/TelegramBots) et `telegrambots-client`, qui permettent la communication avec les utilisateurs via le bot.
L’interface de gestion de la base de connaissances est basée sur **Thymeleaf**.

Pour la partie LLM et RAG (Retrieval-Augmented Generation), Mathis utilise [Spring AI](https://spring.io/projects/spring-ai) avec le module `spring-ai-starter-model-mistral-ai` pour accéder aux modèles de MistralAI.  
L’archivage et la gestion vectorielle des documents sont réalisés avec **PgVector**, grâce à `spring-ai-starter-vector-store-pgvector` et `spring-ai-advisors-vector-store`, facilitant la recherche sémantique des contenus.  
L’extraction automatique du texte des documents téléchargés est gérée par **Apache Tika** via la bibliothèque `spring-ai-tika-document-reader`.

La persistance et la synchronisation des notes et documents avec **Supabase** sont assurées grâce à l'intégration avec Postgres et PgVector.

Le choix de ces technologies vise à faire de Mathis une solution robuste, évolutive et facilement extensible pour la gestion intelligente des documents et des interactions avec l’IA.

## Fonctionnalités

- **Conversation naturelle** : Interagissez avec l’agent Mathis via le chat Telegram ou directement sur la page d’accueil de la webapp.
- **Gestion des utilisateurs et mot de passe par défaut** : Mathis enregistre les utilisateurs et un mot de passe temporaire dans la base relationnelle (attention : la gestion du mot de passe n’est pas implémentée dans ce prototype), puis interagit avec le reste du système.
- **Reconnaissance de l’intention et des autorisations** : Mathis interprète l’intention de l’utilisateur et réagit selon une liste prédéfinie d’actions possibles, en vérifiant éventuellement les droits de l’utilisateur (Spring Security Authorities).
- **Téléchargement de documents** : Vous pouvez télécharger des documents via l’interface web ; Mathis les analyse avec Tika, en extrait le texte pertinent et met à jour la base de données en conséquence. Les documents et leurs liens de téléchargement sont stockés dans une table séparée (non vectorielle).
- **Indexation et stockage** : Le contenu extrait est stocké et indexé sur Supabase.
- **RAG (Retrieval-Augmented Generation)** : Lorsque vous posez des questions au bot, Mathis récupère les documents pertinents de la base de connaissances et génère des réponses contextualisées avec MistralAI.
- **Open Source** : Le projet est entièrement open source, facilement extensible et personnalisable, et est dérivé de [Mathis](https://github.com/gvincenzi/mathis).

## Architecture

<img src="src/main/resources/static/images/schema.png" width="100%">

## Fonctionnement

Mathis fonctionne selon deux principaux flux, tous deux initiés par l’utilisateur via le **Bot Telegram** ou la **WebApp Mathis**, et gérés par la **API Mathis**.

Quand un utilisateur **télécharge un document** (PDF, PowerPoint, Excel...), l’API Mathis reçoit la requête. Le document est analysé par **Apache Tika** pour en extraire le texte. Ce texte est ensuite découpé en "chunks" et converti en vecteurs numériques ("embedding") avant d’être sauvegardé dans la **base vectorielle Supabase**. Une fois le processus terminé, l’utilisateur reçoit une confirmation.

Quand un utilisateur **envoie un message texte** (question ou requête), l’API Mathis intercepte le message. Le système procède à la reconnaissance de l’intention de l’utilisateur et à la vérification de son rôle via Spring Security. L’intention guide le système vers une des actions prévues :

```java
package com.gist.mathis.service.entity;

public enum Intent {
	LIST_DOCUMENTS,
	ASK_FOR_DOCUMENT,
   NOTIFY_ADMIN,
   GENERIC_QUESTION
}
```

- Pour *LIST_DOCUMENTS*, il effectue une recherche dans la base relationnelle.
- Pour *ASK_FOR_DOCUMENT*, il effectue une recherche dans la base vectorielle.
- Pour *USER_MAIL_SENT*, l’adresse e-mail sera enregistrée dans la mémoire du chat (cf. MathisChatMemoryRepository).
- Pour *NOTIFY_ADMIN*, un message est envoyé à tous les comptes Telegram des utilisateurs ADMIN avec un résumé de la conversation sauvegardée dans la mémoire du chat (cf. MathisChatMemoryRepository) et l’e-mail reçu (dans le même message ou dans un USER_MAIL_SENT précédent).
- Pour *GENERIC_QUESTION*, il recherche dans le contenu des documents, utilise toute la chaîne RAG pour interpréter la question, enrichir le contexte et générer une réponse.

Cette recherche se fait dans la **base vectorielle Supabase**, où sont récupérées les informations les plus pertinentes pour la requête de l’utilisateur. Ces informations, avec la requête originale, servent à construire un prompt enrichi de contexte, qui est ensuite envoyé à un **Large Language Model** (comme MistralAI).  
Le modèle génère une "Réponse" basée sur le contexte fourni, qui est ensuite envoyée à l’utilisateur.

## API REST

Mathis propose aussi une API pour gérer la base de connaissances et interagir avec la fonctionnalité de chat.

### `/api/admin/knowledge` (POST)
Télécharge un nouveau document dans la base de connaissances.  
**Paramètres (query):**
- `title` (string, obligatoire) : Titre du document.
- `description` (string, obligatoire) : Description du document.
- `url` (string, obligatoire) : URL source du document.  
**Corps de la requête :**  
- `document` (binaire, obligatoire) : Fichier à télécharger (multipart/form-data).  
**Réponse :**  
- `200 OK` en cas de succès.

### `/api/admin/knowledge` (DELETE)
Supprime un document de la base de connaissances.  
**Paramètres (query):**
- `knowledgeId` (entier, obligatoire) : ID du document à supprimer.  
**Réponse :**  
- `200 OK` en cas de succès.

### `/api/chat` (POST)
Envoyez un message dans le chat et recevez une réponse.  
**Corps de la requête :**  
- Objet JSON de type `ChatMessage`, incluant :
  - `conversationId` (string) : ID de la conversation.
  - `userType` (string, enum : HUMAN, AI) : Type d’utilisateur.
  - `userAuth` (string, enum : ROLE_USER, ROLE_ADMIN) : Rôle d’autorisation de l’utilisateur.
  - `body` (string) : Contenu du message.
  - `inlineKeyboardMarkup` (objet, optionnel) : Clavier inline pour Telegram.
  - `resource` (binaire, optionnel) : Ressource jointe.  
**Réponse :**  
- `200 OK` avec un objet `ChatMessage`.

### Documentation API

La documentation complète de l’API, générée automatiquement au format OpenAPI (Swagger UI), est disponible à l’adresse :
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

*(Remarque : L’adresse `localhost:8080` est valable pour une exécution locale du service. En production, l’URL sera celle du serveur sur lequel Mathis BlocNotes est déployé.)*

## Prérequis

- Java 17+
- Un compte Telegram et un token BotFather
- Une instance Supabase configurée
- Une clé API MistralAI

## Installation

1. **Clonez le dépôt**
   ```bash
   git clone https://github.com/votre-username/mathis_blocnotes.git
   cd Mathis
   ```

2. **Configurez les variables d’environnement et le fichier YML**
   - `TELEGRAM_BOT_TOKEN`
   - `SUPABASE_URL` et `SUPABASE_KEY`
   - `MISTRAL_API_KEY`

3. **Compilez et lancez**
   ```bash
   ./mvnw spring-boot:run
   ```

## Utilisation

- **/start** : (optionnel) Démarre la conversation avec Mathis et reçoit une présentation de l’agent dans la langue du compte Telegram.

## Contribuer

Les contributions, rapports de bugs et propositions de nouvelles fonctionnalités sont les bienvenus ! Ouvrez une issue ou une pull request.

## Licence

Ce projet est distribué sous licence GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007 (GPLv3).