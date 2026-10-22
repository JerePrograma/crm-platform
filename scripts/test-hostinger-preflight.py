#!/usr/bin/env python3
"""Regression cases for the production release inventory and local identities."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("hostinger_images", Path(__file__).resolve().parents[1] / "deploy/hostinger/verify-images.py")
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)
COMMIT = "a" * 40
IMAGE_ID = "sha256:" + "b" * 64


def model():
    labels = {"org.scalaris.release.commit": COMMIT, "org.scalaris.release.image-id": IMAGE_ID}
    return {"services": {
        "postgres": {"image": policy.POSTGRES_IMAGE},
        "backend": {"image": "scalaris-crm-backend:release-" + COMMIT, "labels": labels.copy(), "environment": {"SESSION_COOKIE_SECURE": "true"}},
        "frontend": {"image": "scalaris-crm-frontend:release-" + COMMIT, "labels": labels.copy(), "ports": [{"host_ip": "127.0.0.1", "published": "18095", "target": 8080}]},
    }}


def image(_reference):
    return {"Id": IMAGE_ID, "Config": {"Labels": {"org.opencontainers.image.revision": COMMIT}}}


class ReleasePolicyTest(unittest.TestCase):
    def test_accepts_matching_commit_tags_and_local_image_ids(self):
        policy.validate_configuration(model(), image)

    def test_accepts_digest_with_matching_local_identity(self):
        configuration = model()
        configuration["services"]["frontend"]["image"] = "registry.example/crm/frontend@sha256:" + "c" * 64
        policy.validate_configuration(configuration, image)

    def test_rejects_local_latest_and_unrelated_tags(self):
        for reference in ("scalaris-crm-backend:local", "scalaris-crm-backend:latest", "scalaris-crm-backend:release-deadbeef", "scalaris-crm-backend:release-" + "d" * 40):
            with self.subTest(reference=reference):
                configuration = model()
                configuration["services"]["backend"]["image"] = reference
                with self.assertRaisesRegex(ValueError, "local/latest"):
                    policy.validate_configuration(configuration, image)

    def test_rejects_missing_inventory_and_mismatched_local_image(self):
        for field in ("org.scalaris.release.commit", "org.scalaris.release.image-id"):
            configuration = model()
            del configuration["services"]["backend"]["labels"][field]
            with self.assertRaises(ValueError):
                policy.validate_configuration(configuration, image)
        with self.assertRaisesRegex(ValueError, "image ID"):
            policy.validate_configuration(model(), lambda _: {"Id": "sha256:" + "d" * 64})

    def test_rejects_unavailable_image_and_wrong_revision(self):
        def unavailable(_):
            raise ValueError("Image is not present")
        with self.assertRaisesRegex(ValueError, "not present"):
            policy.validate_configuration(model(), unavailable)
        with self.assertRaisesRegex(ValueError, "revision label"):
            policy.validate_configuration(model(), lambda _: {"Id": IMAGE_ID, "Config": {"Labels": {"org.opencontainers.image.revision": "d" * 40}}})

    def test_rejects_mutable_postgres_or_public_ports(self):
        configuration = model()
        configuration["services"]["postgres"]["image"] = "postgres:17-alpine"
        with self.assertRaisesRegex(ValueError, "PostgreSQL 17.11"):
            policy.validate_configuration(configuration, image)
        configuration = model()
        configuration["services"]["frontend"]["ports"][0]["host_ip"] = "0.0.0.0"
        with self.assertRaisesRegex(ValueError, "127.0.0.1:18095"):
            policy.validate_configuration(configuration, image)
        configuration = model()
        configuration["services"]["backend"]["ports"] = [{"target": 8080}]
        with self.assertRaisesRegex(ValueError, "must not publish"):
            policy.validate_configuration(configuration, image)

    def test_rejects_insecure_cookie_and_different_release(self):
        configuration = model()
        configuration["services"]["backend"]["environment"]["SESSION_COOKIE_SECURE"] = "false"
        with self.assertRaisesRegex(ValueError, "Secure session"):
            policy.validate_configuration(configuration, image)
        configuration = model()
        other = "d" * 40
        configuration["services"]["frontend"]["labels"]["org.scalaris.release.commit"] = other
        configuration["services"]["frontend"]["image"] = "scalaris-crm-frontend:release-" + other
        with self.assertRaisesRegex(ValueError, "same reviewed release"):
            policy.validate_configuration(configuration, lambda _: {"Id": IMAGE_ID})


if __name__ == "__main__":
    unittest.main()
