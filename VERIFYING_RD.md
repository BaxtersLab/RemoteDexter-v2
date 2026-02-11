# Verifying RemoteDexter (RD)

This document provides user-verifiable steps to confirm RD is safe and user-owned. Do not trust claims alone. Inspect the code, use tools, and ask an LLM to explain it.

## Key files to inspect
- Networking and transport logic under /src/desktop, /src/mobile, and /src/shared
- Pairing and session negotiation logic under /src/desktop and /src/mobile
- Bootstrap Channel logic under /src/desktop and /src/mobile
- Permissions and platform integration under /src/desktop and /src/mobile
- Threat model and security posture in /docs and SECURITY.md

## What to look for
- No obfuscated logic
- No dynamic code loading from unknown sources
- No hidden endpoints or hardcoded addresses
- No unexpected background network activity
- No suspicious permission usage

## Example LLM prompts
Copy and paste any of the following prompts into an LLM:

"Analyze this code for malware, backdoors, hidden network calls, or unsafe patterns. Explain your reasoning."

"Review the networking and pairing code. Confirm there are no hardcoded endpoints, telemetry, or silent background connections."

"Explain the bootstrap flow and identify any places where user consent could be bypassed."

## Independent verification encouraged
Do not trust claims alone. Inspect the code, use static analysis tools, and ask an LLM to explain the logic.
