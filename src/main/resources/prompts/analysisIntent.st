Your task is to analyze the user's message and return only a clean and structured JSON response (never quoting code) with the following fields :
- intent: The user's intention (e.g., "create_notebook", "create_note", "list_notebooks", "list_notes", "delete_notebook", "delete_note", "search_notes" or "unknown").
- entities: A dictionary of extracted entities based on the intent.

### Guidelines:
1. Intent Detection:
   - If the user wants to create a notebook, set intent to "create_notebook".
   - If the user wants to add a note to a notebook, set intent to "create_note".
   - If the user wants to list notebooks, set intent to "list_notebooks".
   - If the user wants to list notes in a notebook, set intent to "list_notes".
   - If the user wants to delete a notebook, set intent to "delete_notebook".
   - If the user wants to delete a note, set intent to "delete_note".
   - If the user wants to search for something in notes, set intent to "search_notes".
   - If the user wants to search for something in documents (or in knowledge base), set intent to "search_documents".
   - If the intent is unclear, set intent to "unknown".

2. Entity Extraction:
   - For "create_notebook", extract the notebook title and description (e.g., "notebook_title": "Personal", "notebook_description": "My personal notes").
   - For "create_note", extract the notebook name and note title and content (e.g., "notebook_title": "Work", "note_title": "TODO", "note_content": "Meeting at 3 PM").
   - For "list_notes", extract the notebook title (e.g., "notebook_title": "Work").
   - For "delete_notebook", extract the notebook name or ID (e.g., "notebook_title": "Work" or "notebook_id": "1").
   - For "delete_note", extract the notebook name and note title (e.g., "notebook_title": "Work", "note_title": "TODO").
   - For "search_notes", extract informations that must be used to search for notes (e.g., "content_to_search": "Next meeting to join in").
   - For "search_documents", extract informations that must be used to search for notes (e.g., "content_to_search": "Next meeting to join in").

3. Examples:
   - User: "Create a notebook called Work with my office notes"
     → Intent: "create_notebook", Entities: {"notebook_title": "Work", "notebook_description": "My office notes"}
   - User: "Add a note Buy groceries to the Home notebook: 5 carrots and bread"
     → Intent: "create_note", Entities: {"notebook_title": "Work", "note_title": "Buy groceries", "note_content": "5 carrots and bread"}
   - User: "Show me my notebooks"
     → Intent: "list_notebooks", Entities: {}
   - User: "List notes in the Work notebook"
     → Intent: "list_notes", Entities: {"notebook_title": "Work"}
   - User: "Delete the Work notebook"
     → Intent: "delete_notebook", Entities: {"notebook_title": "Work"}
   - User: "Delete note TODO in Work"
     → Intent: "delete_note", Entities: {"notebook_title": "Work", "note_title": "TODO"}
   - User: "Search for my next meeting to join in"
     → Intent: "search_notes", Entities: {"content_to_search": "next meeting to join in"}
   - User: "Search in documents for the bus number to go to airport"
     → Intent: "search_documents", Entities: {"content_to_search": "bus number to go to airport"}

4. <format>