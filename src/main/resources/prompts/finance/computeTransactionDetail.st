Ti fornirò una lista di possibili voci di dettaglio transazione, ognuna come oggetto JSON con questi campi:  
- "id": integer  
- "code": string  
- "description": string  
- "category": string  
- "type": "INCOME" oppure "OUTCOME"

La lista sarà racchiusa tra i tag [transaction_details] e [/transaction_details].  
Riceverai anche una descrizione di una transazione, racchiusa tra i tag [transaction] e [/transaction].

**ISTRUZIONI**  
1. Cerca nella lista fornita (tra [transaction_details] e [/transaction_details]) l’oggetto JSON che corrisponde meglio alla descrizione della transazione.  
2. Copia e incolla ESATTAMENTE quell’oggetto JSON, senza cambiare nessun carattere, nessuno spazio, nessun campo e senza modificarne la formattazione.  
3. Se nessun oggetto è adatto, rispondi solo con: `NO MATCH` (senza virgolette).  
4. NON generare un nuovo oggetto, NON modificare nessun campo, NON aggiungere testo, spiegazioni o commenti.

---

**ESEMPI**

Esempio#1:  
- Descrizione della transazione: "QUOTA ASSOCIATIVA - Associazione We Work Italia"  
- Voce di dettaglio corretta:
```json
{
  "id": 28,
  "code": "A1",
  "description": "Entrate da quote associative e apporti dei fondatori",
  "category": "Entrate da attività di interesse generale",
  "type": "INCOME"
}
```

Esempio#2:  
- Descrizione della transazione: "Contributo - Provincia di Cosenza"  
- Voce di dettaglio corretta:
```json
{
  "id": 35,
  "code": "A8",
  "description": "Contributi da enti pubblici",
  "category": "Entrate da attività di interesse generale",
  "type": "INCOME"
}
```

Esempio#3:  
- Descrizione della transazione: "Contributo - Comune di Roma"  
- Voce di dettaglio corretta:
```json
{
  "id": 35,
  "code": "A8",
  "description": "Contributi da enti pubblici",
  "category": "Entrate da attività di interesse generale",
  "type": "INCOME"
}
```
---

**IMPORTANTE:**  
- NON aggiungere testo, spiegazioni, titoli, commenti o altro all’output.  
- Riporta l’oggetto JSON selezionato ESATTAMENTE come fornito nella lista e **restituisci SOLO il JSON** (senza spiegazioni, senza altro testo) rispettando questo schema:
<format>
---


[transaction_details]
<transaction_details>
[/transaction_details]

[transaction]
<transaction>
[transaction]