# RemoteDexter Roadmap

This roadmap outlines the long-term evolution of RemoteDexter.
It defines each development phase, its objectives, deliverables, and handoff structure.
RemoteDexter Basic remains sovereign, deterministic, offline-first, and minimal.
RD Dev Tools provides optional expansion, automation, and intelligence.

---

## Phase Structure

All phases are stored in:

docs/roadmap/

Code

Each phase includes:

PHASE_X_OVERVIEW.md
PHASE_X_BLOCKS/
PHASE_X_STATUS.json

Code

---

## Phase 1 — Foundation (Completed)

**Objective:**
Establish the sovereign core of RemoteDexter.

**Deliverables:**
- Noise-based secure pairing
- Transport abstraction
- Bluetooth bootstrap
- Wi-Fi Direct integration
- USB transport
- Basic UI
- Core protocol

---

## Phase 2 — Remote Session System (In Progress)

**Objective:**
Deliver full remote control capability.

**Deliverables:**
- Screen streaming
- Input control
- File transfer
- Session orchestration
- Permission revocation
- Security hardening
- First-run experience
- Diagnostics panel
- Packaging and installers

Artifacts:
- PHASE_2_OVERVIEW.md
- PHASE_2_BLOCKS/
- PHASE_2_STATUS.json

---

## Phase 3 — Automation, Adaptation, Continuity

**Objective:**
Make RD self-managing and adaptive.

**Deliverables:**
- Transport handoff engine
- Adaptive streaming
- Adaptive input
- Trust fabric expansion
- Simple Mode / Advanced Mode
- Session continuity
- Watchdogs and resilience

Artifacts:
- PHASE_3_OVERVIEW.md
- PHASE_3_BLOCKS/
- PHASE_3_STATUS.json

---

## Phase 4 — RD Dev Tools Intelligence Layer

**Objective:**
Provide optional intelligence for advanced users.

**Deliverables:**
- Local-first LLM integration (optional)
- Natural-language automation
- Intelligent diagnostics
- Plugin generation
- Developer augmentation tools

Artifacts:
- PHASE_4_OVERVIEW.md
- PHASE_4_BLOCKS/
- PHASE_4_STATUS.json

---

## Phase 5 — Ecosystem Expansion

**Objective:**
Extend RD beyond its initial platform boundaries.

**Deliverables:**
- Plugin API
- Linux/macOS desktop support
- iOS support (ReplayKit + USB)
- Multi-device orchestration

Artifacts:
- PHASE_5_OVERVIEW.md
- PHASE_5_BLOCKS/
- PHASE_5_STATUS.json

---

## Phase 6 — Community & Distribution

**Objective:**
Prepare RD for broad adoption.

**Deliverables:**
- Package managers
- Auto-update channels
- Documentation portal
- Contribution guidelines
- Community governance

Artifacts:
- PHASE_6_OVERVIEW.md
- PHASE_6_BLOCKS/
- PHASE_6_STATUS.json

---

## Handoff Structure

Each phase includes:

- Block definitions
- File paths
- Responsibilities
- Acceptance criteria
- Security considerations
- Testing requirements
- Migration notes (if any)

Handoff files live in:

docs/roadmap/PHASE_X_BLOCKS/

Code

---

## Stability Guarantees

RemoteDexter Basic:
- Deterministic
- Offline-first
- No cloud dependencies
- No telemetry
- No LLM
- No dynamic code
- Minimal and auditable

RD Dev Tools:
- Optional
- Modular
- User-controlled
- Extensible

---

## Contribution Workflow

1. Select a phase
2. Select a block
3. Implement according to:
   - Block definition
   - File paths
   - Acceptance criteria
4. Submit PR referencing the block
5. Update PHASE_X_STATUS.json

---