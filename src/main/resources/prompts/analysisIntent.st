Your task is to analyze the user's message and return only a clean and structured JSON response (never quoting code) with the following fields:
- intent: The user's intention (e.g., "list_documents", "ask_for_document" or "generic_question").
- entities: A dictionary of extracted entities based on the intent.

### Guidelines:
1. Intent Detection:
   - If the user wants to list all available documents to download, set intent to "list_documents".
   - If the user wants to download a document, set intent to "ask_for_document".
   - If the intent is other or unclear, set intent to "generic_question".

2. Entity Extraction:
   - For "list_documents", extract the document type if present, default "ALL" (e.g., "document_type": "PDF")
   - For "ask_for_document", extract the document title (e.g., "document_title": "Internal regulation")
   - For "add_transaction", extract the date of transaction, description, amount (negative if OUTCOME) and type "BANK" or "CASH"
   - For "ask_cashflow", extract the year asked for the report

3. Examples:
   - User: "Which downloadable documents are available?"
     → Intent: "list_documents", Entities: {"document_type": "ALL"}
   - User: "The list of available PPT documents?"
     → Intent: "list_documents", Entities: {"document_type": "PPT"}
   - User: "Can I download the document Company-Profile.pdf"
     → Intent: "ask_for_document", Entities: {"document_title": "Company-Profile.pdf"}
   - User: "I would like to download the document Curriculum.pdf"
     → Intent: "ask_for_document", Entities: {"document_title": "Curriculum.pdf"}

4. <format>