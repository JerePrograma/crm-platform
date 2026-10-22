#!/usr/bin/env python3
"""Validate reviewed release identity without printing the rendered secrets."""
import json
import re
import subprocess
import sys

POSTGRES_IMAGE = "postgres:17.11-alpine@sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb"


def inspect_local_image(reference):
    result = subprocess.run(
        ["docker", "image", "inspect", "--format", "{{json .}}", "--", reference],
        capture_output=True, text=True, check=False,
    )
    if result.returncode:
        raise ValueError("A reviewed release image is not present in the local Docker image store.")
    try:
        return json.loads(result.stdout)
    except (ValueError, TypeError) as error:
        raise ValueError("Docker did not return a valid image identity.") from error


def validate_configuration(configuration, inspect_image=inspect_local_image):
    services = configuration.get("services", {})
    if services.get("postgres", {}).get("image") != POSTGRES_IMAGE:
        raise ValueError("Hostinger PostgreSQL must use the reviewed PostgreSQL 17.11 digest.")
    commits = set()
    for name in ("backend", "frontend"):
        service = services.get(name, {})
        reference = service.get("image", "")
        labels = service.get("labels", {})
        commit = labels.get("org.scalaris.release.commit", "")
        expected_id = labels.get("org.scalaris.release.image-id", "")
        if not re.fullmatch(r"[a-f0-9]{40}", commit):
            raise ValueError("The reviewed release must identify its complete 40-character commit.")
        commits.add(commit)
        if not re.fullmatch(r"sha256:[a-f0-9]{64}", expected_id):
            raise ValueError("Each reviewed CRM image must identify its expected local SHA-256 image ID.")
        digest = re.fullmatch(r"[a-z0-9][a-z0-9./:_-]*@sha256:[a-f0-9]{64}", reference)
        commit_tag = re.fullmatch(r"[a-z0-9][a-z0-9./:_-]*:release-" + commit, reference)
        if not digest and not commit_tag:
            raise ValueError("CRM images must use a digest or an exact release-<full-commit> tag; local/latest tags are prohibited.")
        actual = inspect_image(reference)
        if actual.get("Id") != expected_id:
            raise ValueError("The local CRM image ID does not match the reviewed release inventory.")
        revision = (actual.get("Config", {}).get("Labels") or {}).get("org.opencontainers.image.revision")
        if revision is not None and revision != commit:
            raise ValueError("The image revision label does not match the reviewed release commit.")
    if len(commits) != 1:
        raise ValueError("Both CRM images must belong to the same reviewed release commit.")
    backend = services["backend"]
    frontend = services["frontend"]
    if str(backend.get("environment", {}).get("SESSION_COOKIE_SECURE", "")).lower() != "true":
        raise ValueError("Hostinger requires Secure session cookies.")
    ports = frontend.get("ports", [])
    if len(ports) != 1 or ports[0].get("host_ip") != "127.0.0.1" or str(ports[0].get("published")) != "18095" or ports[0].get("target") != 8080:
        raise ValueError("Hostinger frontend must publish only 127.0.0.1:18095 to container port 8080.")
    if backend.get("ports") or services["postgres"].get("ports"):
        raise ValueError("Hostinger backend and PostgreSQL must not publish host ports.")


if __name__ == "__main__":
    try:
        validate_configuration(json.load(sys.stdin))
    except (ValueError, TypeError, KeyError):
        # Validation errors contain only static policy explanations, never data
        # from service environment variables or raw Docker diagnostics.
        message = sys.exc_info()[1]
        if isinstance(message, json.JSONDecodeError):
            message = "Compose did not return a valid release model."
        print(f"Hostinger release policy failed: {message}", file=sys.stderr)
        sys.exit(1)
    print("Hostinger image policy passed: exact references, reviewed local image IDs and isolated ports.")
