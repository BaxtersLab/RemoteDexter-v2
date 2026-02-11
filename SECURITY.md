# Security Posture

RemoteDexter (RD) is designed to be auditable and user-owned. The default posture is zero call-home and user-initiated connections only.

## Guarantees
- No hardcoded servers, domains, or IPs
- No telemetry, analytics, or tracking
- No auto-update or hidden network calls
- No hidden privilege escalation
- No silent pairing, no silent installs
- No dynamic code loading from unknown sources
- All connections are user-initiated and user-owned

## Threat model
RD reduces risk by removing centralized infrastructure and keeping all endpoints user-owned.

Attack surfaces:
- Bootstrap Channel misuse (Bluetooth, AirDrop, USB)
- Pairing and session negotiation errors
- Transport misuse or downgrade
- Local privilege escalation on the User PC or User Device
- Malicious or compromised Paired Device

Mitigations and boundaries:
- Bootstrap is offline and user-approved; no silent installs.
- Pairing is explicit and requires user action on both devices.
- The Noise handshake establishes confidentiality and integrity for sessions and only begins after explicit user action.
- Transport selection is user-driven; no hidden fallback endpoints.
- No background network calls outside a user-initiated session.

## Reporting vulnerabilities
Please report security issues using one of the following:
- A private email address managed by the repo owner
- A private issue channel designated by the repo owner

Provide:
- A clear description
- Steps to reproduce
- Impact and severity assessment (if known)

We will acknowledge valid findings and release fixes in a timely manner.
