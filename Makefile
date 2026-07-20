.PHONY: preflight preflight-container postgres-port db-up db-down app-up app-down app-logs backend frontend frontend-lock verify smoke smoke-container reset-db

preflight:
	sh scripts/preflight.sh --local

preflight-container:
	sh scripts/preflight.sh --container-only

postgres-port:
	sh scripts/set-postgres-host-port.sh 55432

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

frontend-lock:
	sh scripts/generate-frontend-lock.sh

verify:
	sh ./mvnw -B -f backend/pom.xml verify
	cd frontend && npm install && npm run typecheck && npm run build
	docker compose --profile app --profile smoke config
	docker compose --progress plain --profile app build --no-cache frontend
	docker compose --progress plain --profile app build --no-cache backend

smoke:
	sh scripts/smoke-test.sh

smoke-container:
	@set -eu; \
	trap 'docker compose --profile app --profile smoke down --remove-orphans' EXIT; \
	docker compose --profile app --profile smoke up --build --abort-on-container-exit --exit-code-from smoke smoke

reset-db:
	docker compose --profile app --profile smoke down -v --remove-orphans
