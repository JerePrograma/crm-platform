#!/usr/bin/env sh
set -eu

maven_image=${MAVEN_IMAGE:-maven:3.9.16-eclipse-temurin-21}
maven_cache_volume=${MAVEN_CACHE_VOLUME:-crm_maven_cache}
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
target_volume="crm_backend_verify_target_$$"
container_name="gestudio-crm-backend-verify-$$"

command -v docker >/dev/null 2>&1 || {
  printf 'ERROR: Docker is required.\n' >&2
  exit 1
}

cleanup() {
  docker rm -f "$container_name" >/dev/null 2>&1 || true
  docker volume rm -f "$target_volume" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

docker volume create "$maven_cache_volume" >/dev/null
docker volume create "$target_volume" >/dev/null

docker run --rm \
  --name "$container_name" \
  --add-host host.docker.internal:host-gateway \
  --environment DOCKER_HOST=unix:///var/run/docker.sock \
  --environment TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  --mount "type=bind,source=$repo_root,target=/workspace,readonly" \
  --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock \
  --mount "type=volume,source=$maven_cache_volume,target=/root/.m2" \
  --mount "type=volume,source=$target_volume,target=/workspace/backend/target" \
  --workdir /workspace \
  "$maven_image" \
  mvn -B -f backend/pom.xml verify

printf 'Containerized backend verification passed.\n'
printf 'Covered: compilation, Spotless, unit tests, ArchUnit and Testcontainers.\n'
