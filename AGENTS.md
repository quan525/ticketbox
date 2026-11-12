# Ticketbox AI access rules

## Environment files and secrets

- Do not read, write, copy, search within, summarize, upload, or reveal environment files: `.env`, `.env.*`, `*.env`, `*.env.*`, and their copies/backups at any depth. This includes environment example files. Use configuration key names documented in `docs/SEPAY.md` and placeholder-only snippets instead.
- Do not access private keys, certificates containing private keys, keystores, credential stores, or files under `secrets/`, `.secrets/`, `.ssh/`, or `.aws/`. Treat `*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`, and `*.keystore` as protected.
- Do not inspect database files or dumps for secrets or user data. In particular, do not open `.local` database files, database backups, or local account-seed files.
- Do not print process environment variables, dump all configuration, inspect process command lines for credentials, or retrieve secrets from a secret manager. Do not expose secret values in logs, screenshots, tool output, diffs, commit messages, or chat.
- Do not bypass these restrictions with another tool, shell, script, symlink, archive, database query, browser, connector, subprocess, or permission escalation. Do not disable or weaken the access profile to complete another task.
- Before repository-wide searches, explicitly exclude protected files and directories. Search source files and known non-secret documentation directly where possible. A search exclusion or `.gitignore` is not a security boundary.
- If work needs a secret or an environment-file change, finish the code with placeholders, document the required key names, and let the user configure values locally. Do not ask the user to paste secrets into chat. The user must explicitly name and authorize a specific exception; general requests to build, run, debug, or configure the app do not authorize reading secrets.
- If a tool unexpectedly returns a secret, do not repeat or store it. Stop that inspection and tell the user which source was exposed without including the value.

## Scope

These are project instructions for AI assistants, not an OS-enforced access boundary. The accompanying `.codex/config.toml` defines a deny-access profile for supported Codex runtimes. Managed app permissions must activate/allow that profile; an already-running session does not gain new restrictions merely because this file was added.

## Verification preference

Do not run or add tests unless the user asks to resume testing. Builds may be run when needed.
