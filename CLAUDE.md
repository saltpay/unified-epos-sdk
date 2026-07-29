# CLAUDE.md

Guidance for working in this repository. These are sample apps that show partners how to
integrate the Teya Unified ePOS SDK, so the code is read far more than it is run.

## Core Principles

### Optimise for readability

- Partners read these samples to learn the SDK, so favour clear, simple code over clever code.
- Prefer straightforward, linear flows that are easy to follow end to end.
- Keep the code self-explanatory; do not add comments to explain what the code does.
- Only demonstrate what a partner needs. Avoid abstractions, indirection, and edge-case
  handling that obscure the integration.

### New samples mimic the existing ones

- Before adding a sample, study the closest existing one and follow it closely: directory
  layout, package/namespace structure, file naming, and screen/flow breakdown.
- Reuse the same names for equivalent concepts across samples.
- Match the established tech stack and patterns for the platform rather than introducing new
  libraries or conventions.

### Minimise drift between samples

- The samples should feel like one family. When you change a shared concept in one sample,
  apply the equivalent change to the others so they stay consistent.
- Keep the same feature set and flow order across samples where the platform allows it.
- If a divergence is unavoidable (platform or SDK-type difference), keep it minimal and make it
  obvious why it exists.

## Public Repository

This repository is public and shared with partners. Do not add anything internal: internal
URLs, dashboards, ticket references, credentials, keys, test merchant identifiers, or
internal-only commentary.