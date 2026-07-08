# GPG signing keys

This directory contains **public** GPG material for verifying published Maven artifacts.

## Included in releases

- `public-gpg.asc` — public key for signature verification

## Never commit or distribute

- `private-gpg.asc`
- `private-gpg.asc.base64`

Private signing material must stay in a secure secret store (CI secrets, password manager, or local keychain). If a private key was ever committed or shared:

1. Revoke and rotate the key immediately.
2. Remove it from git history with [BFG](https://rtyley.github.io/bfg-repo-cleaner/) or `git-filter-repo`.
3. Force-push only after coordinating with the release team.

## Local signing

Configure GPG for Gradle publishing via environment variables or `gradle.properties` (not committed). See `gradle.properties.example` in the repository root.
