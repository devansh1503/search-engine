🔎 Distributed Search Engine

A scalable, distributed search engine built using microservices architecture with Spring Boot, Kafka, Redis, and Elasticsearch.

⸻

🚀 Overview

This project simulates a real-world search engine pipeline:
	•	Crawl web pages
	•	Process and index data
	•	Provide search with ranking
	•	Support autocomplete suggestions
	•	Compute PageRank for relevance

⸻

🧠 Architecture

Crawler → Kafka → Indexer → Elasticsearch
                 ↓
           Pagerank Service

Query Service → Elasticsearch + Redis (cache)
Autocomplete Service → Redis (FT.SUG)


⸻

⚙️ Tech Stack

Component	Technology
Backend	Spring Boot (Java 21)
Messaging	Kafka
Search Engine	Elasticsearch
Cache / Store	Redis (Redis Stack)
Parsing	Jsoup
Containerization	Docker


⸻

📦 Services

1. Crawler Service
	•	Crawls web pages using Jsoup
	•	Extracts:
	•	Title
	•	Content
	•	Links
	•	Publishes:
	•	Raw pages → Kafka (raw-pages)
	•	URLs → Kafka (urls)

Features:
	•	Depth limiting
	•	Rate limiting (Redis)
	•	Domain limiting
	•	Duplicate prevention (visited URLs)

⸻

2. Indexer Service
	•	Consumes raw pages from Kafka
	•	Indexes into Elasticsearch (pages index)
	•	Pushes titles to Redis autocomplete

⸻

3. Query Service
	•	Handles search queries
	•	Uses Elasticsearch for full-text search
	•	Applies ranking:
	•	Text relevance
	•	PageRank score
	•	Caches results in Redis

⸻

4. Autocomplete Service
	•	Uses Redis FT.SUGGET
	•	Provides prefix-based suggestions
	•	Supports fuzzy matching

⸻

5. PageRank Service
	•	Builds graph using Redis:
	•	graph:nodes
	•	graph:in:*
	•	graph:out:*
	•	Runs iterative PageRank algorithm
	•	Updates scores back into Elasticsearch

⸻

🔄 Data Flow
	1.	Seed URLs pushed to Kafka
	2.	Crawler processes URLs
	3.	Pages sent to Indexer
	4.	Indexer stores in Elasticsearch
	5.	Pagerank builds graph + computes scores
	6.	Query Service serves ranked results
	7.	Autocomplete updated continuously

⸻

🧪 Running the Project

1. Start all services

docker-compose up --build


⸻

2. Seed URLs into Kafka

docker exec -it kafka-searchengine \
  kafka-console-producer.sh \
  --broker-list localhost:9092 \
  --topic urls

Then input:

https://example.com|0


⸻

3. Search API

curl "http://localhost:8080/search?query=spotify"


⸻

4. Autocomplete API

curl "http://localhost:8084/autocomplete?query=spo"


⸻

📊 Key Features
	•	✅ Distributed microservices architecture
	•	✅ Event-driven pipeline using Kafka
	•	✅ Full-text search using Elasticsearch
	•	✅ Redis-based autocomplete (FT.SUG)
	•	✅ PageRank-based ranking
	•	✅ Query caching for performance
	•	✅ Rate-limited web crawling

⸻

⚠️ Known Limitations
	•	Basic URL normalization
	•	Limited ranking signals (no click tracking yet)
	•	PageRank computation not optimized for large scale
	•	No frontend UI

⸻

🚀 Future Improvements
	•	Add frontend (React)
	•	Click tracking + learning-to-rank
	•	Better ranking signals (recency, title boost)
	•	Improved URL normalization
	•	Distributed PageRank computation
	•	Observability (metrics, logging, tracing)

⸻

📁 Project Structure

searchengine/
├── crawler-service/
├── indexer-service/
├── query-service/
├── pagerank-service/
├── autocomplete-service/
├── common/
└── docker-compose.yml


⸻

💡 Resume Description

Built a distributed search engine using Spring Boot microservices, Kafka, Redis, and Elasticsearch. Implemented web crawling, indexing, autocomplete using Redis FT.SUG, and PageRank-based ranking for relevance.

⸻

👨‍💻 Author

Devansh

⸻

⭐ If you like this project, give it a star!
