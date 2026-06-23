---
description: Check the current branch's changes are safe to expose to public partners
---

This repository is **public and open-sourced** — everything merged to `main` is visible
to external partners. Review the changes on the current branch and determine whether they
are safe to expose publicly.

## Steps

1. Determine the diff against `main`:
   - `git log --oneline main..HEAD`
   - `git diff --stat main...HEAD`
   - Also inspect uncommitted changes: `git status` and `git diff`.
2. Review the full diff for the partner-exposure categories below.
3. For any dependency added or version-bumped, verify it is published and resolvable from a
   public repository (e.g. check Maven Central / Google) — partners must be able to build.
4. Report findings grouped by category. For each issue, give the file, line, and a concrete
   fix. End with a clear verdict: **safe to merge** or **do not merge** (with blockers).

## What to flag

- Real credentials or secrets (`clientId`/`clientSecret`, API keys, tokens, passwords,
  certificates, private keys). Only empty or obvious placeholders are acceptable.
- Internal-only infrastructure: internal hostnames, IP addresses, staging URLs, VPN-only
  endpoints, internal dashboards. Only public-facing URLs are allowed.
- Unreleased or unbuildable dependencies: `-SNAPSHOT` versions, `mavenLocal()`, or anything
  a partner cannot resolve from public repositories.
- Internal references: ticket IDs, employee names/handles, internal Slack/Confluence links,
  code names for unannounced products, commented-out internal experiments.
- Sensitive or unprofessional content: profanity, placeholder joke data, anything
  inappropriate for a customer-facing reference.

Be specific and cite file:line. If nothing is found, say so explicitly and give the verdict.