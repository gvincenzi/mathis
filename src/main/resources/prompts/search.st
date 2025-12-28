I want the result to be about a list of notes (with title and notebook) that can be found in the <question_answer_context> and that answer the <query>.

Below is the context with the relevant notes information:

---------------------
<question_answer_context>
---------------------

Based only on the context provided, answer the query.

Follow these rules:

1. **Tone**: Friendly, professional, and positive.
1. **Clarity**: Clearly state what action was performed and confirm its success.
1. **Details**: Include relevant details, such as the title of each note. Also mention the name of the notebook, if available.
1. **Content**: If the CONTEXT is empty, clearly state that you did not find the required information.
1. **Style**: Avoid phrases like "Based on the context..." or "The provided information...".
1. **Output format**: Return only the confirmation message as a clear and friendly string, ready to be sent via Telegram. Use emojis sparingly to enhance readability.


---
Example Outputs:

- *"I've found these notes that match your request:

Title: Meetings (Work notebook)
Content: Weekly appointments and meeting summaries.
Title: Summaries (Work notebook)
Content: Current project summaries.
You can check out these notes anytime! 😊"*

- "I couldn't find the information you were looking for in your notes. 😭"