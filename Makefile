.PHONY: preflight preflight-container postgres-port local-ports repository-safety db-up db-down app-up app-down app-logs backend backend-verify-container frontend frontend-lock verify verify-container validate-seg001 validate-complete-crm smoke smoke-container reset-db

preflight:
	sh scripts/preflight.sh --local

preflight-container:
	sh scripts/preflight.sh --container-only

postgres-port:
	sh scripts/set-postgres-host-port.sh 55432

local-ports:
	sh scripts/set-local-host-ports.sh 55432 8080 5173

repository-safety:
	sh scripts/check-repository-safety.sh

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

backend-verify-container:
	sh scripts/verify-backend-container.sh

frontend:
	cd frontend && if [ -f package-lock.json ]; then npm ci; else npm install; fi && npm run dev

frontend-lock:
	sh scripts/generate-frontend-lock.sh

verify:
	sh ./mvnw -B -f backend/pom.xml verify
	cd frontend && if [ -f package-lock.json ]; then npm ci; else npm install; fi && npm run typecheck && npm run build
	docker compose --profile app --profile smoke config
	docker compose --progress plain --profile app build --no-cache frontend
	docker compose --progress plain --profile app build --no-cache backend
	sh scripts/check-repository-safety.sh

verify-container: preflight-container backend-verify-container frontend-lock
	docker compose --profile app --profile smoke config
	docker compose --progress plain --profile app build --no-cache frontend
	docker compose --progress plain --profile app build --no-cache backend
	$(MAKE) smoke-container
	sh scripts/check-repository-safety.sh

validate-seg001:
	bash scripts/validate-seg001.sh

validate-complete-crm:
	bash scripts/validate-complete-crm.sh

smoke:
	sh scripts/smoke-test.sh

smoke-container:
	@set -eu; \
	trap 'docker compose --profile app --profile smoke down --remove-orphans' EXIT; \
	docker compose --profile app --profile smoke up --build --abort-on-container-exit --exit-code-from smoke smoke

reset-db:
	docker compose --profile app --profile smoke down -v --remove-orphans
