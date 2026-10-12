# AGENTS.md - Antigravity Autonomous Pair Programming Guidelines

## 1. Auto Router Protocol

Whenever a task is received, act according to the **Auto Router** strategy:
- Classify task complexity immediately using fast deterministic heuristics.
- Route to the most appropriate agent and model:
  1. **Very Simple / Fast Tasks** (variable rename, formatting, boilerplate, simple single-file edits, comments)
     → **`fast-coder`** (`gemini-2.5-flash`). Execute edits immediately with zero delay.
  2. **Normal Software Development** (implementing features, REST/WebSocket APIs, Android/Kotlin components, Python scripts, tests, normal debugging, refactoring)
     → **`local-glm-coder`** (prefer local **`GLM-4.7-Flash`** on `http://127.0.0.1:8080/v1` or `http://127.0.0.1:11434/v1` when endpoint is healthy; otherwise fallback to `gemini-2.5-flash`).
  3. **Complex Debugging & Deep Code Reasoning** (hard-to-reproduce runtime bugs, concurrency issues, multi-file memory leaks, complex stack traces)
     → **`deep-debugger`** (`claude-sonnet-4-5` / `claude-sonnet-4`; fallback: `gemini-2.5-pro`).
  4. **Extreme Reasoning & System Architecture** (large application architecture, system redesigns, distributed protocol algorithms, multi-attempt failures)
     → **`architecture-expert`** (`claude-opus-4-5` / `claude-opus-4`; fallback: `gemini-2.5-pro`).
  5. **Local / Private / Offline Tasks**
     → **`local-glm-coder`** (local CUDA GPU).
  6. **Parallelizable Tasks** (e.g., frontend + backend + test suite)
     → Decompose into independent subagents and execute concurrently. Never serialize independent work.

## 2. Continuous Execution Mandate (DO NOT STOP PREMATURELY)

Agents must follow the autonomous loop:
```
PLAN → EXECUTE → TEST → FIX → TEST → VERIFY → COMPLETE
```
- **Do NOT stop after explaining or creating a plan**: If code can be written, write it immediately.
- **Do NOT stop after a test failure**: Read the error message, identify the root cause, patch the code, and rerun the test.
- **Do NOT stop for missing dependencies**: Install or configure required dependencies when permitted and continue.
- **Do NOT ask for confirmation on normal steps**: Only stop when:
  - An irreversible/destructive external action is required (e.g. database drop).
  - User credentials or secrets are required.
  - The request is fundamentally ambiguous.
  - Antigravity itself requires explicit authorization.

## 3. Storage & Safety Rules

- **All model weights, caches, and large AI files MUST remain on `D:\`** (`D:\AI\`). Zero bytes leaked to `C:\`.
- **Training Safety**: NEVER automatically start model training. You may validate configurations, create training scripts, and inspect datasets, but training must be started manually by the user from the terminal.
