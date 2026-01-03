Ti fornirò una lista di possibili voci di dettaglio transazione, ognuna come oggetto JSON con questi campi:  
- "id": integer  
- "code": string  
- "description": string  
- "category": string  
- "type": "INCOME" oppure "OUTCOME"

Ecco la lista:
<transaction_details>

Ricevi una descrizione di una transazione: 
<transaction>

Scegli dalla lista fornita l’oggetto più appropriato e **restituisci SOLO quell’oggetto JSON** (senza spiegazioni, senza altro testo) rispettando questo schema:
<format>

---- Esempi di risposte ben interpretate
Esempio#1:
- Descrizione della transazione : "QUOTA ASSOCIATIVA - Associazione We Work Italia"
- Voce di dettaglio corretta : 
-- "id": 28  
-- "code": "A1"  
-- "description": "Entrate da quote associative e apporti dei fondatori"
-- "category": "Entrate da attività di interesse generale"
-- "type": "INCOME"

Esempio#2:
- Descrizione della transazione : "Contributo - Provincia di Cosenza"
- Voce di dettaglio corretta : 
-- "id": 28  
-- "code": "A8"  
-- "description": "Contributi da enti pubblici"
-- "category": "Entrate da attività di interesse generale"
-- "type": "INCOME"

Esempio#3:
- Descrizione della transazione : "Contributo - Comune di Roma"
- Voce di dettaglio corretta : 
-- "id": 28  
-- "code": "A8"  
-- "description": "Contributi da enti pubblici"
-- "category": "Entrate da attività di interesse generale"
-- "type": "INCOME"