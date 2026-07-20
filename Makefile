.PHONY: preflight db-up db-down app-up app-down app-logs backend frontend verify smoke reset-db

preflight:
	sh scripts/preflight.sh

db-up:
	docker compose up -d postgres

db-down:
	docker compose stop postgres

app-up:
	docker compose --profile app up -d --build

app-down:
	docker compose --profile app down

app-logs:
	docker compose --profile app logs -f

backend:
	sh ./mvnw -f backend/pom.xml spring-boot:run

frontend:
	cd frontend && npm install && npm run dev

verify:
	sh ./mvnw -B -f backend/pom.xml verify
	cd frontend && npm install && npm run typecheck && npm run build
	docker compose --profile app config
	docker build -t gestudio-crm:local .
	docker build -f frontend/Dockerfile -t gestudio-crm-frontend:local frontend

smoke:
	sh scripts/smoke-test.sh

reset-db:
	docker compose --profile app down -v
