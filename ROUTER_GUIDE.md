# Antigravity Model Router & Continuous Execution Engine Guide

## Overview

The **Antigravity Model Router** is an intelligent, production-quality, low-latency (<5ms) model and agent routing system built natively inside Google Antigravity. It eliminates manual model switching, accelerates coding workflows, enforces continuous execution without premature stopping, and prioritizes local GPU acceleration on **GLM-4.7-Flash** while maintaining seamless fallbacks to Google Gemini and Anthropic Claude.

---

## 1. How the Router Works

The primary workflow follows a fast, deterministic decision pipeline:

```
[USER PROMPT]
       ↓
[DETERMINISTIC AUTO ROUTER (<5ms)] ──(Evaluates keywords, complexity, scope)
       ↓
[AGENT & MODEL SELECTION]
       ↓
[IMMEDIATE EXECUTION] ───────────────(Applies file edits immediately, zero preamble)
       ↓
[VERIFIER & TEST SUITE] ─────────────(Runs syntax checks and automated tests)
       ↓ (Passed)                           ↓ (Failed)
[COMPLETE & REPORT]                  [DIAGNOSE & SURGICAL FIX]
                                            ↓
                                     [RE-RUN VERIFICATION]
                                            ↓ (2 Consecutive Failures)
                                     [AUTO-ESCALATE TO SONNET]
```

### Key Principles:
1. **Zero LLM Pre-Classification Latency**: Routing decisions use deterministic regex heuristics and fast socket/HTTP health checks (0.3s timeout). No extra API token or LLM call is wasted merely deciding which model to use.
2. **Continuous Execution**: The router and lifecycle hooks (`hooks.json`) ensure the agent never stops prematurely after creating a plan, after a single file edit, or after a test failure. It fixes failures and re-tests autonomously.
3. **Training Safety (Rule 20)**: Lifecycle hooks intercept automated training commands (`train.py`, `accelerate launch`, etc.) to guarantee that model training is only ever started manually by the user.

---

## 2. Available Custom Agents & Models

All agents are configured in both **Global** (`~/.gemini/config/agents/`), **Plugin** (`~/.gemini/config/plugins/model-router/agents/`), and **Workspace** (`.agents/agents/`) locations.

| Agent Name | Role | Primary Model | Fallback Model | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **`auto-router`** | Master Coordinator | `auto` / `gemini-2.5-flash` | `gemini-2.5-pro` | Default pair-programmer, task classification, parallel subagent dispatching |
| **`local-glm-coder`** | Local GPU Engineer | `GLM-4.7-Flash` (port 8080/11434) | `gemini-2.5-flash` | Normal software development, features, APIs, scripts, tests, private/offline tasks |
| **`fast-coder`** | Ultra-Fast Editor | `gemini-2.5-flash` | `gemini-2.5-pro` | Variable renames, formatting, typos, quick single-file modifications |
| **`deep-debugger`** | Deep Reasoning & RCA | `claude-sonnet-4-5` | `gemini-2.5-pro` | Hard runtime bugs, race conditions, memory leaks, complex multi-file bugs |
| **`architecture-expert`** | Principal Architect | `claude-opus-4-5` | `gemini-2.5-pro` | High-level system design, large refactors, complex algorithms, multi-attempt failures |
| **`verifier`** | Autonomous QA | `gemini-2.5-flash` | `gemini-2.5-pro` | Git diff inspection, syntax compilation checks, running project test suites |

---

## 3. Routing Rules Matrix

Incoming requests are categorized using the following priority order:

1. **Auto-Escalation (Prior Failures ≥ 2)**
   - If previous verification failed twice consecutively:
   - **Target**: `deep-debugger` (`claude-sonnet-4-5`).
2. **Local / Private / Offline Tasks**
   - Triggered by: `local`, `offline`, `private`, `confidential`, `glm`, `on-device`, `airgap`.
   - **Target**: `local-glm-coder` (`GLM-4.7-Flash` on port 8080).
   - If offline: Graceful fallback to `fast-coder` (`gemini-2.5-flash`).
3. **Parallelizable Multi-Component Tasks**
   - Triggered by: Multi-concern prompts (`frontend and backend`, `api and ui`, `tests and implementation`).
   - **Target**: Concurrent subagents dispatched simultaneously.
4. **Extreme Reasoning & System Architecture**
   - Triggered by: `architecture`, `system design`, `redesign`, `distributed`, `complex algorithm`, `compiler`.
   - **Target**: `architecture-expert` (`claude-opus-4-5`).
5. **Complex Debugging**
   - Triggered by: `debug`, `crash`, `freeze`, `segfault`, `race condition`, `memory leak`, `null pointer`, `subtle bug`, `heisenbug`.
   - **Target**: `deep-debugger` (`claude-sonnet-4-5`).
6. **Very Simple / Fast Tasks**
   - Triggered by: `rename`, `format`, `whitespace`, `typo`, `comment`, `boilerplate`, `one-line`.
   - **Target**: `fast-coder` (`gemini-2.5-flash`).
7. **Normal Software Development (Default)**
   - Triggered by: `implement`, `feature`, `api`, `endpoint`, `component`, `refactor`, `write tests`.
   - **Target**: `local-glm-coder` (`GLM-4.7-Flash` on port 8080/11434).
   - If offline: Fallback to `gemini-2.5-flash`.

---

## 4. Fallback Behavior

The system never crashes or halts due to an unavailable model:

- **Local GLM Offline**: If `llama-server.exe` on port 8080 is down:
  1. Checks secondary Ollama endpoint on port 11434 (`http://127.0.0.1:11434`).
  2. If both local endpoints are unavailable, instantly falls back to `gemini-2.5-flash`.
- **Claude Sonnet / Opus Offline or Unconfigured**:
  - Automatically falls back to `gemini-2.5-pro`.
- **Gemini Flash Offline**:
  - Automatically falls back to `gemini-2.5-pro`.

---

## 5. Escalation Behavior

When working on a difficult problem:
1. `local-glm-coder` attempts the implementation and triggers `verifier`.
2. If `verifier` reports a test or syntax failure:
   - Attempt 1: Injects failure diagnostics back to coder for a surgical fix.
   - Attempt 2: If the test still fails, the router automatically escalates to `deep-debugger` (`claude-sonnet-4-5`).
3. If an architectural overhaul is required, the task escalates to `architecture-expert` (`claude-opus-4-5`).

---

## 6. Parallel Execution

For independent task components, the Auto Router splits work into concurrent subagents:

Example Prompt:
> *"Build user authentication backend and frontend login screen"*

Decomposed Execution:
- **Subagent 1 (`local-glm-coder`)**: Implements backend authentication API, hashing, and token validation.
- **Subagent 2 (`fast-coder`)**: Implements frontend login form and UI bindings.
- **Subagent 3 (`verifier`)**: Runs automated integration tests against both components.

Work is executed in parallel to minimize overall wall-clock turnaround time.

---

## 7. How to Use & Access the System in Antigravity

### A. Automatic Mode (Default Pair Programmer)
Simply open the Antigravity Agent chat panel and type your task naturally:
- *"Rename this variable across all files"* → Fast coder responds instantly on Gemini Flash.
- *"Implement an Android notification channel"* → Local GLM coder responds on GPU (port 8080).
- *"Debug why the background service freezes on Android 14"* → Deep debugger runs on Claude Sonnet.
- *"Design a multi-region failover architecture"* → Architecture expert runs on Claude Opus.

### B. Explicit Skill Invocation
You can mention the skill in chat:
- `@auto-router Route and execute this task: ...`

### C. Manual Agent Selection
You can target specific agents directly from the chat prompt:
- `@local-glm-coder Implement the live location update loop`
- `@fast-coder Format and rename all constants in SosWidgetProvider.kt`
- `@deep-debugger Analyze this memory leak trace`
- `@architecture-expert Review our offline-first data synchronization strategy`
- `@verifier Verify all recent changes and run tests`

### D. CLI & Terminal Usage
You can test routing decisions or dispatch tasks directly from PowerShell or Command Prompt:

```cmd
# Windows Batch:
d:\tushar\I_AM_HERE\scripts\route.bat "Implement a websocket client"

# With JSON output:
d:\tushar\I_AM_HERE\scripts\route.bat "Fix subtle race condition" --json

# Test escalation:
d:\tushar\I_AM_HERE\scripts\route.bat "Fix tax calculation" --failures=2 --json

# Run complete diagnostic check:
D:\AI\Antigravity-Local\venv\Scripts\python.exe d:\tushar\I_AM_HERE\scripts\diagnose_router.py

# Run unit tests:
D:\AI\Antigravity-Local\venv\Scripts\python.exe d:\tushar\I_AM_HERE\scripts\test_model_router.py
```

---

## 8. How to Modify Routing Rules or Add Models

### Modifying Classification Rules:
Edit `d:\tushar\I_AM_HERE\scripts\model_router.py`:
- Update `RE_VERY_SIMPLE`, `RE_NORMAL_DEV`, `RE_COMPLEX_DEBUG`, or `RE_EXTREME_REASONING` regexes.
- Adjust `AutoRouter.classify_and_route()` logic.

### Adding a New Model:
1. Open `d:\tushar\I_AM_HERE\scripts\model_router.py`.
2. Add your model to the `MODELS` dictionary:
   ```python
   MODELS["MY_NEW_MODEL"] = "my-custom-model-id"
   ```
3. Assign it to a category or create a corresponding agent in `.agents/agents/<agent-name>.md`.

---

## 9. System Diagnostics & Health Check

Run the diagnostic suite at any time:
```cmd
D:\AI\Antigravity-Local\venv\Scripts\python.exe d:\tushar\I_AM_HERE\scripts\diagnose_router.py
```
This verifies:
- Primary and secondary local GLM endpoints (:8080 and :11434).
- Storage safety (weights strictly on `D:\`, zero leak to `C:\`).
- Agent definitions across global, plugin, and workspace folders.
- Hook scripts and rule files.
- Microsecond routing benchmark across all 5 test scenarios.
