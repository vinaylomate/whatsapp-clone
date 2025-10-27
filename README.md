Whatsapp Clone — Real-time chat with 1:1 DMs & Groups on Spring Boot microservices (Eureka, Cloud Gateway, WebSocket/STOMP), React frontend, PostgreSQL, JWT auth, Resilience4j (CircuitBreaker/Retry/Bulkhead), and production observability with Prometheus + Grafana (p95 latency, retries, bulkhead saturation). Includes GKE-ready Kubernetes manifests and Skaffold profiles.

Highlights
- Direct Messages (1:1) and Group chats with membership + admin controls
- Spring Cloud Gateway (HTTP + lb:ws) and Eureka service discovery
- WebSocket/STOMP broadcasts for real-time delivery, message persistence in PostgreSQL
- JWT-based authentication (register/login) and authorization on chat APIs
- Resilience4j on Feign calls (presence): CircuitBreaker, Retry, Bulkhead + fallbacks
- Prometheus scrape + prebuilt Grafana dashboard (p95, retries, CB transitions, bulkhead saturation)
- Kubernetes manifests (GKE-ready) and Skaffold profiles (dev, gke)
- React/Vite frontend demo with login, chat list, DM/Group creation, messaging

Quick Start
1) unzip whatsapp-clone-full.zip -d whatsapp-clone && cd whatsapp-clone
2) docker compose up --build
   - Gateway:   http://localhost:8088
   - Frontend:  http://localhost:5180
   - Eureka:    http://localhost:8762
   - Grafana:   http://localhost:3001 (admin/admin)

GKE Deploy (summary)
skaffold run -p gke --default-repo=gcr.io/<PROJECT_ID>
kubectl -n whatsapp get ingress whatsapp-ingress
# REST: http://<IP>/api/...  WS: ws://<IP>/ws
