.PHONY: preflight preflight-container db-up db-down app-up app-down app-logs backend frontend verify smoke smoke-container reset-db

preflight:
	sh scripts/preflight.sh --local

preflight-container:
	sh scripts/preflight.sh --container-only

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
	docker compose --profile app --profile smoke config
	docker build -t gestudio-crm:local .
	docker build -f frontend/Dockerfile -t gestudio-crm-frontend:local frontend

smoke:
	sh scripts/smoke-test.sh

smoke-container:
	@set -eu; \
	trap 'docker compose --profile app --profile smoke down --remove-orphans' EXIT; \
	docker compose --profile app --profile smoke up --build --abort-on-container-exit --exit-code-from smoke smoke

reset-db:
	docker compose --profile app --profile smoke down -v --remove-orphans
