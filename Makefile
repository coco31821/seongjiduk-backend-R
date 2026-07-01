.PHONY: test build local-up local-down

test:
	./gradlew clean test --no-daemon

build:
	./gradlew clean bootJar --no-daemon

local-up:
	docker compose -f infra/docker/docker-compose.local.yml --env-file .env up --build

local-down:
	docker compose -f infra/docker/docker-compose.local.yml --env-file .env down
