# Threat Model

This document summarizes the primary attack surfaces and mitigations.

## Attack surfaces
- Bootstrap Channel misuse (Bluetooth, AirDrop, USB)
- Pairing process errors or bypass
- Transport misuse or downgrade
- Local privilege escalation on the User PC or User Device
- Malicious or compromised Paired Device

## Mitigations
- Bootstrap requires user approval on both devices.
- Pairing is explicit and user-driven.
- Noise handshake provides confidentiality and integrity.
- Transport selection is user-driven; no hidden fallback endpoints.
- No background network calls outside user-initiated sessions.

## Boundaries and assumptions
- A Paired Device exists only after explicit OS-level pairing.
- The Noise handshake begins only after user-approved pairing.
- Bootstrap does not bypass pairing or grant remote access.

## Residual risks
- Compromised devices can still misuse authorized access.
- Users must validate pairing prompts carefully.
