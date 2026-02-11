# Architecture Overview

RemoteDexter (RD) is a peer-to-peer remote access system with user-owned endpoints.

## Components
- RD Desktop Application: runs on the User PC
- RD Mobile Application: runs on the User Device
- Bootstrap Channel: Bluetooth, AirDrop, or USB for initial installation and pairing
- Paired Device: any device that has completed OS-level pairing

## High-level flow
1. User installs RD Desktop Application on the User PC.
2. User installs RD Mobile Application on the User Device.
3. User approves bootstrap and pairing on both devices.
4. User Device connects directly to the User PC.

## Design constraints
- No centralized servers
- No call-home behavior
- User-initiated connections only

## Sovereign Design Principles
- Peer-to-peer: No centralized servers
- User-owned endpoints: Desktop and mobile only communicate directly
- No call-home: No telemetry or external connections
- Auditable: Full source code available

## Noise Handshake Flow
1. Desktop generates ephemeral X25519 keypair
2. Sends NoiseInit with public key
3. Android generates keypair, derives shared secret
4. Android sends NoiseResponse
5. Both derive session keys via HKDF

## Command Channel Flow
1. Commands framed with length prefix
2. Encrypted with AEAD (AES-GCM) using session keys
3. Nonce incremented per message
4. Decrypted and handled on receiver

## Trust boundaries
- Bootstrap is offline and user-approved.
- Pairing requires explicit OS-level consent on both devices.
- The Noise handshake begins only after pairing and user intent are confirmed.
