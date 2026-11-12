# AI file access restrictions

`AGENTS.md` prohibits AI access to environment files (including examples), keys, credential stores and local databases. Use documented variable names and placeholders for configuration work; enter real values locally yourself. `.gitignore` also excludes environment variants and key material from accidental versioning. Neither file enforces file access at the operating-system level or removes files already tracked by Git.

## Codex permission profile

`.codex/config.toml` defines `ticketbox-private` and selects it as the default for sessions that honor project configuration. It extends the built-in workspace policy, denies environment files, private key formats, secret directories, local database files and local account-seed artifacts. Paths outside the workspace are restricted to the runtime's minimal needs. Command networking remains disabled; this profile does not grant extra access or approvals.

This chat uses app-managed permissions. Saving project configuration does not change the running session or override an administrator's managed profile. Restart Codex/open a new chat and select or allow `ticketbox-private` through the app's permission controls. If it is not available, ask the administrator to add the deny rules to the managed profile. Do not claim enforcement until the active policy shows those denies.

Older `sandbox_mode` / `sandbox_workspace_write` settings can supersede named profiles unless a managed profile allowlist selects them. Do not blindly replace global or administrator settings. Review compatibility with the official permissions documentation before activating this profile.

The profile bounds recursive glob scanning to eight directory levels; use additional exact deny paths for sensitive files stored deeper. Deny globs are expanded when sandboxed processes start on Windows/Linux/WSL. Already-running processes may retain access to newly created matching files. The profile also includes exact root paths for common environment files. Restart relevant sessions/processes after adding secrets. This configuration is intended for Codex, not every AI editor, and does not revoke copies already in chat or credentials already inherited by a running process.

Validate an activated policy with a disposable, non-secret fixture in a separate session. Never open a real environment file just to check whether it is blocked. No secret files were opened to create these rules, and no access-probe tests were run.

Official OpenAI documentation: https://learn.chatgpt.com/docs/permissions
