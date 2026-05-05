#!/usr/bin/env bash
  # Downloads the OHS Player Configuration IG package and extracts the artifacts
  # required by ./gradlew generateFromIg.
  #
  # Fetches from the published FHIR npm package (package.tgz) so new ViewDefinitions
  # and StructureDefinitions are picked up automatically on each run — no code changes needed.
  #
  # Run from the project root after cloning, or whenever the IG is updated:
  #   bash scripts/fetch-ig.sh
  #   ./gradlew generateFromIg

  set -e

  PACKAGE_URL="https://ohs-foundation.github.io/ohs-player-config-ig/package.tgz"
  IGD="ohs-player-reference-app/ohs-sample-ig"
  TMP_DIR=$(mktemp -d)

  echo "Fetching IG package from $PACKAGE_URL ..."
  curl -sf "$PACKAGE_URL" -o "$TMP_DIR/package.tgz"

  echo "Extracting..."
  tar -xzf "$TMP_DIR/package.tgz" -C "$TMP_DIR"

  mkdir -p "$IGD/output"
  mkdir -p "$IGD/fsh-generated/resources"

  # Binary ViewDefinition instances (root + example/ subfolder) → output/
  find "$TMP_DIR/package" -name "Binary-*View.json" \
    -exec cp {} "$IGD/output/" \;

  # StructureDefinitions → fsh-generated/resources/
  find "$TMP_DIR/package" -maxdepth 1 -name "StructureDefinition-*.json" \
    -exec cp {} "$IGD/fsh-generated/resources/" \;

  # CodeSystems → fsh-generated/resources/
  find "$TMP_DIR/package" -maxdepth 1 -name "CodeSystem-*.json" \
    -exec cp {} "$IGD/fsh-generated/resources/" \;

  rm -rf "$TMP_DIR"

  echo ""
  echo "Extracted to $IGD:"
  echo "  output/          $(ls "$IGD/output" | wc -l | tr -d ' ') files"
  echo "  fsh-generated/   $(ls "$IGD/fsh-generated/resources" | wc -l | tr -d ' ') files"
  echo ""
  echo "Done. Run ./gradlew generateFromIg to generate Kotlin sources."