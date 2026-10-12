---
name: auto-router
description: >-
  Production-quality automatic model routing and continuous execution system for Google Antigravity.
  Classifies incoming tasks (<5ms) using deterministic signals, routes to the optimal model/agent
  (local GLM-4.7-Flash on CUDA GPU, Gemini Flash, Claude Sonnet, or Claude Opus), enforces continuous
  execution without premature stops, verifies code changes, and manages parallel subagents.
---

# Antigravity Auto Router & Continuous Execution Engine

The Auto Router serves as your primary pair-programming coordinator. It classifies requests instantly,
routes them to the fastest appropriate model or local GPU agent, verifies changes, and keeps working
until the goal is achieved.

## 1. Routing Matrix

| Category | Task Type Examples | Primary Target | Model ID | Fallback Model |
| :--- | :--- | :--- | :--- | :--- |
| **A. Simple / Fast** | Rename variable, format, whitespace, typo, small boilerplate, comment | `fast-coder` | `gemini-2.5-flash` | `gemini-2.5-pro` |
| **B. Normal Software Dev** | Features, APIs, components, scripts, standard debugging, tests, refactoring | `local-glm-coder` | `GLM-4.7-Flash` (port 8080/11434) | `gemini-2.5-flash` |
| **C. Complex Debugging** | Runtime crash, subtle race condition, multi-file bug, memory leak, heisenbug | `deep-debugger` | `claude-sonnet-4-5` | `gemini-2.5-pro` |
| **D. Extreme Reasoning** | Architecture overhaul, complex algorithms, system redesign, multi-attempt failures | `architecture-expert` | `claude-opus-4-5` | `gemini-2.5-pro` |
| **E. Local / Offline** | Private, sensitive, offline, on-device tasks | `local-glm-coder` | `GLM-4.7-Flash` (local CUDA) | `gemini-2.5-flash` |
| **F. Parallelizable** | Frontend + Backend, UI + API, multi-component tasks | `auto-router` (subagents) | Concurrent subagents | Sequential |

## 2. Continuous Execution Contract

The system enforces non-stop execution through the following state machine:

```
[USER PROMPT]
      ↓
[AUTO ROUTER CLASSIFICATION (<5ms)]
      ↓
[MODEL/AGENT DISPATCH]
      ↓
[EXECUTE EDITS IMMEDIATELY]
      ↓
[RUN TESTS / VERIFIER] ──(Passed)──→ [COMPLETE & REPORT]
      ↓ (Failed)
[DIAGNOSE ERROR]
      ↓
[APPLY SURGICAL FIX]
      ↓
[RE-RUN TESTS] ──(Passed)──→ [COMPLETE & REPORT]
      ↓ (2 Consecutive Failures)
[AUTO-ESCALATE TO DEEP-DEBUGGER (Sonnet)]
```

### Non-Stop Rules:
1. **Never stop after a plan**: If an implementation plan is drafted, proceed immediately to writing code.
2. **Never stop on a test failure**: Read the failure stack trace, identify the cause, patch the code, and re-test.
3. **Never stop for missing dependencies**: Install required packages within project virtual environments and resume.
4. **Only pause for user confirmation when**:
   - A destructive/irreversible action is required (deleting databases, dropping production tables).
   - User secrets or credentials are needed.
   - The user's requirement is fundamentally ambiguous.

## 3. Local Model Integration (GLM-4.7-Flash)

- **Primary High-Speed Endpoint**: `http://127.0.0.1:8080/v1` (`llama-server.exe` with Flash-Attention, CUDA 13, 16K context).
- **Secondary Fallback Endpoint**: `http://127.0.0.1:11434/v1` (`ollama.exe`).
- **GGUF Weights Path**: `D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf` (Strictly on D:, zero disk usage on C:).
- **Health Verification**: Fast non-blocking HTTP GET to `http://127.0.0.1:8080/health` (timeout: 300ms).

## 4. Parallel Subagent Delegation

When tasks contain independent concerns (e.g., "Build backend API and frontend dashboard"):
1. Subagent 1 (`local-glm-coder`): Backend API endpoints and schema models.
2. Subagent 2 (`fast-coder`): Frontend UI components and state bindings.
3. Subagent 3 (`verifier`): Integration tests, build validation, and linting.
Run subagents concurrently to maximize throughput.

## 5. Training Safety Rule

**NEVER start model training automatically.**
Inspecting training data, writing training scripts, and validating configurations is allowed. If a training script or pipeline is completed, STOP and present it to the user. Training must only be initiated manually by the user from the terminal.
