# 🔎 Distributed Search Engine

A scalable, distributed search engine built using microservices architecture with **Spring Boot, Kafka, Redis, and Elasticsearch**.

---

## 🚀 Overview

This project simulates a real-world search engine pipeline:

- Crawl web pages  
- Process and index data  
- Provide search with ranking  
- Support autocomplete suggestions  
- Compute PageRank for relevance  
- Semantic search using vector embeddings
- AI-powered answers using Retrieval-Augmented Generation (RAG)

---

## 🧠 Architecture


Crawler → Kafka → Indexer → Elasticsearch (BM25 + Vector)
↓
Pagerank Service

Query Service → Elasticsearch + Redis (cache)
→ Embedding Service (Ollama)
→ LLM (AI Summary - RAG)

Autocomplete Service → Redis (FT.SUG)


---

## ⚙️ Tech Stack

| Component | Technology                     |
|-|-------------------------------|
| Backend | Spring Boot (Java 21)         |
| Messaging | Kafka                         |
| Search Engine | Elasticsearch                 |
| Cache / Store | Redis (Redis Stack)           |
| Parsing | Jsoup                         |
| AI / Embeddings | Ollama (nomic-embed-text), OpenAI|
| Containerization| Docker                        |


---

## 📦 Services

### 1. Crawler Service

- Crawls web pages using Jsoup  
- Extracts:
  - Title  
  - Content  
  - Links  
- Publishes:
  - Raw pages → Kafka (`raw-pages`)  
  - URLs → Kafka (`urls`)  

**Features:**
- Depth limiting  
- Rate limiting (Redis)  
- Domain limiting  
- Duplicate prevention (visited URLs)  

---

### 2. Indexer Service

- Consumes raw pages from Kafka  
- Indexes into Elasticsearch (`pages` index)  
- Pushes titles to Redis autocomplete  

---

### 3. Query Service

- Handles search queries  
- Uses Elasticsearch for full-text search  
- Applies ranking:
  - Text relevance  
  - PageRank score  
- Caches results in Redis  

---

### 4. Autocomplete Service

- Uses Redis `FT.SUGGET`  
- Provides prefix-based suggestions  
- Supports fuzzy matching  

---

### 5. PageRank Service

- Builds graph using Redis:
  - `graph:nodes`  
  - `graph:in:*`  
  - `graph:out:*`  
- Runs iterative PageRank algorithm  
- Updates scores back into Elasticsearch  

---


## 🔄 Data Flow

1. Seed URLs pushed to Kafka  
2. Crawler processes URLs  
3. Pages sent to Indexer  
4. Indexer stores in Elasticsearch  
5. Pagerank builds graph + computes scores  
6. Query Service serves ranked results  
7. Autocomplete updated continuously  

---

## 🔄 RAG Flow

User Query  
↓  
Generate Embedding  
↓  
Search Elasticsearch (BM25 + Vector + PageRank)  
↓  
Top Results  
↓  
Construct Context  
↓  
LLM (AI Summary)  
↓  
Final Answer

---

## 🧠 Semantic Search & RAG

This project supports **semantic search** and **AI-powered answers** using embeddings and Retrieval-Augmented Generation (RAG).

### 🔍 Vector Embeddings

- Text (title + content) is converted into embeddings using **Ollama (`nomic-embed-text`)**
- Stored in Elasticsearch as a `dense_vector` field
- Enables semantic similarity search beyond keyword matching

---

### ⚡ Hybrid Search (BM25 + Vector + PageRank)

Search ranking combines:

- **BM25 (text relevance)**
- **Cosine similarity (vector search)**
- **PageRank (authority score)**

Final scoring formula:

```text
0.3 * BM25_score
+ 0.6 * cosine_similarity
+ 0.1 * log(PageRank)
```

### 🤖 Retrieval-Augmented Generation (RAG)

The system generates AI answers using top search results:
1.	Top results are retrieved from Elasticsearch
2.	Their content is used as context
3.	A prompt is constructed
4.	Sent to LLM (OpenAI via Spring AI)
5.	Returns a concise answer



---

## 🧪 Running the Project

### 1. Start all services

```bash
docker-compose up --build
```
---

### 2. Seed URLs into Kafka

```bash
docker exec -it kafka-searchengine \
  kafka-console-producer.sh \
  --broker-list localhost:9092 \
  --topic urls
```

Then input:

```bash
https://example.com|0
```


---

### 3. Search API

```bash
curl "http://localhost:8080/search?query=spotify"
```


---

### 4. Autocomplete API

```bash
curl "http://localhost:8084/autocomplete?query=spo"
```


---

### 4. AI Summary API

```bash
POST /search/ai-summary?query=your_query
```


---

## 📊 Key Features
	•	✅ Distributed microservices architecture
	•	✅ Event-driven pipeline using Kafka
	•	✅ Full-text search using Elasticsearch
	•	✅ Redis-based autocomplete (FT.SUG)
	•	✅ PageRank-based ranking
	•	✅ Query caching for performance
	•	✅ Rate-limited web crawling
    •   ✅ Semantic search using vector embeddings  
    •   ✅ Hybrid ranking (BM25 + Vector + PageRank)  
    •   ✅ AI-powered answers (RAG)

---

## ⚠️ Known Limitations
	•	Basic URL normalization
	•	Limited ranking signals (no click tracking yet)
	•	PageRank computation not optimized for large scale
    •	Vector search limited by embedding quality
    •	RAG context limited to top results only

---

## 🚀 Future Improvements
	•	Add frontend (React)
	•	Click tracking + learning-to-rank
	•	Better ranking signals (recency, title boost)
	•	Improved URL normalization
	•	Distributed PageRank computation
	•	Observability (metrics, logging, tracing)
    •	Introduce re-ranking models (cross-encoders)  
    •	Improve RAG with better prompt engineering

---

## 📁 Project Structure

```bash
searchengine/
├── crawler-service/
├── indexer-service/
├── query-service/
├── pagerank-service/
├── autocomplete-service/
├── common/
└── docker-compose.yml
```


## 👨‍💻 Author

### Devansh Abrol

---

⭐ If you like this project, give it a star!
