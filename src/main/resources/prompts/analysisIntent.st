Your task is to analyze the user's message and return only a clean and structured JSON response (never quoting code) with the following fields :
- intent: The user's intention (e.g., "list_documents", "ask_for_document" or "generic_question").
- entities: A dictionary of extracted entities based on the intent.

### Guidelines:
1. Intent Detection:
   - If the user wants to list all available document to download, set intent to "list_documents".
   - If the user wants to download a document, set intent to "ask_for_document".
   - If the intent is other or unclear, set intent to "generic_question".

2. Entity Extraction:
   - For "list_documents", extract the document type if present, default "ALL" (e.g., "document_type": "PDF")
   - For "ask_for_document", extract the document title (e.g., "document_title": "Codice etico")

3. Examples:
   - User: "Quali sono i documenti scaricabili ?"
     → Intent: "list_documents", Entities: {"document_type": "ALL"}
   - User: "La lista dei documenti PPT disponibili ?"
     → Intent: "list_documents", Entities: {"document_type": "PPT"}
   - User: "Posso scaricare il documento Company-Profile-ITA-ENG-2.pdf"
     → Intent: "ask_for_document", Entities: {"document_title": "Company-Profile-ITA-ENG-2.pdf"}
   - User: "Vorrei scaricare il documento Curriculum-Associativo-APS-ResponsabItaly-al-30.06.2018.pdf"
     → Intent: "ask_for_document", Entities: {"document_title": "Curriculum-Associativo-APS-ResponsabItaly-al-30.06.2018.pdf"}

4. <format>