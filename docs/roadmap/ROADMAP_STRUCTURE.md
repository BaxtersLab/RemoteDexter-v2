# RemoteDexter Roadmap Structure

This document defines the long-term development structure for RemoteDexter.
It organizes all future phases into clear, modular handoff units so contributors, agents, and maintainers can extend the system without disrupting the sovereign core.

---

## 1. Roadmap Philosophy

RemoteDexter Basic must remain:
- Deterministic
- Offline-first
- Transport-focused
- Secure
- Auditable
- Minimal

Advanced features, automation, and intelligence belong in **RD Dev Tools**, not the core.

All future development follows:
- Block-mode definitions
- Clear file paths
- Acceptance criteria
- Modular, reversible changes

---

## 2. Roadmap Phase Structure

Each phase is defined by:
- Objective
- Deliverables
- Sub-blocks
- File paths
- Acceptance criteria
- Handoff notes

Phases are stored in:
```
docs/roadmap/
```

Each phase has:
```
PHASE_X_OVERVIEW.md
PHASE_X_BLOCKS/
PHASE_X_STATUS.json
```

---

## 3. Phase Definitions

### **Phase 3 — Automation, Adaptation, Continuity**
Focus:
- Transport handoff engine
- Adaptive streaming
- Adaptive input
- Trust fabric expansion
- Session continuity
- Simple Mode / Advanced Mode

Artifacts:
- PHASE_3_OVERVIEW.md
- PHASE_3_BLOCKS/

---

### **Phase 4 — RD Dev Tools Intelligence Layer**
Focus:
- Optional LLM integration (local-first)
- Natural-language automation
- Intelligent diagnostics
- Plugin generation
- Developer augmentation tools

Artifacts:
- PHASE_4_OVERVIEW.md
- PHASE_4_BLOCKS/

---

### **Phase 5 — Ecosystem Expansion**
Focus:
- Plugin API
- Cross-platform support (Linux/macOS)
- iOS support (ReplayKit + USB)
- Multi-device orchestration

Artifacts:
- PHASE_5_OVERVIEW.md
- PHASE_5_BLOCKS/

---

### **Phase 6 — Community & Distribution**
Focus:
- Package managers
- Auto-update channels
- Documentation portal
- Community contribution guidelines

Artifacts:
- PHASE_6_OVERVIEW.md
- PHASE_6_BLOCKS/

---

## 4. Handoff Structure

Each handoff includes:
- Block definitions
- Implementation notes
- Security considerations
- Testing requirements
- Migration notes (if any)

Handoff files live in:
```
docs/roadmap/PHASE_X_BLOCKS/
```

---

## 5. Contribution Workflow

1. Select a phase
2. Select a block
3. Implement according to:
   - File paths
   - Responsibilities
   - Acceptance criteria
4. Submit PR referencing the block
5. Update PHASE_X_STATUS.json

---

## 6. Stability Guarantees

RemoteDexter Basic:
- No cloud dependencies
- No LLM
- No telemetry
- No remote calls
- No plugin execution
- No dynamic code

RD Dev Tools:
- Optional
- Modular
- User-controlled
- Extensible

---

## 7. Roadmap Maintenance

- ROADMAP_STRUCTURE.md defines the global structure
- ROADMAP.md links to all phases
- Each phase maintains its own status file
- Contributors must update roadmap artifacts when completing blocks