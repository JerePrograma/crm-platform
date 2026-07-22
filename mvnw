#!/bin/sh
set -eu

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

MAVEN_VERSION=3.9.16
MAVEN_HOME="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/apache-maven-$MAVEN_VERSION"
ARCHIVE="$MAVEN_HOME/apache-maven-$MAVEN_VERSION-bin.zip"
DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
EXPECTED_SHA512=ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3

if [ ! -x "$MAVEN_HOME/apache-maven-$MAVEN_VERSION/bin/mvn" ]; then
  mkdir -p "$MAVEN_HOME"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$DISTRIBUTION_URL" --output "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$DISTRIBUTION_URL" --output-document="$ARCHIVE"
  else
    echo "curl or wget is required to download Maven" >&2
    exit 1
  fi

  if command -v sha512sum >/dev/null 2>&1; then
    printf '%s  %s\n' "$EXPECTED_SHA512" "$ARCHIVE" | sha512sum --check --strict
  elif command -v shasum >/dev/null 2>&1; then
    printf '%s  %s\n' "$EXPECTED_SHA512" "$ARCHIVE" | shasum -a 512 --check
  else
    echo "sha512sum or shasum is required to verify Maven" >&2
    exit 1
  fi

  command -v unzip >/dev/null 2>&1 || {
    echo "unzip is required to install Maven" >&2
    exit 1
  }
  unzip -q -o "$ARCHIVE" -d "$MAVEN_HOME"
  rm -f "$ARCHIVE"
fi

exec "$MAVEN_HOME/apache-maven-$MAVEN_VERSION/bin/mvn" "$@"
