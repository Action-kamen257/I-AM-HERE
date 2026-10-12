#!/usr/bin/env python3
"""
Antigravity Auto Router & Model Dispatcher
Production-quality, low-latency (<5ms) deterministic task classifier,
model selector, fallback engine, and continuous execution orchestrator.
"""

import sys
import os
import re
import json
import time
import urllib.request
import urllib.error
from typing import Dict, Any, List, Optional, Tuple

# Available models in installed Antigravity environment
MODELS = {
    "GEMINI_FLASH": "gemini-2.5-flash",
    "GEMINI_PRO": "gemini-2.5-pro",
    "CLAUDE_SONNET": "claude-sonnet-4-5",
    "CLAUDE_OPUS": "claude-opus-4-5",
    "LOCAL_GLM": "GLM-4.7-Flash",
}

# Agents mapping
AGENTS = {
    "AUTO_ROUTER": "auto-router",
    "FAST_CODER": "fast-coder",
    "LOCAL_GLM_CODER": "local-glm-coder",
    "DEEP_DEBUGGER": "deep-debugger",
    "ARCHITECTURE_EXPERT": "architecture-expert",
    "VERIFIER": "verifier",
}

# Endpoints for local GLM
LOCAL_ENDPOINTS = {
    "PRIMARY": "http://127.0.0.1:8080",      # llama-server.exe
    "FALLBACK": "http://127.0.0.1:11434",    # ollama.exe
}

# Pre-compiled regex patterns for zero-latency (<1ms) classification
RE_VERY_SIMPLE = re.compile(
    r"\b(rename|format|formatting|indent|whitespace|typo|spelling|comment|docstring|"
    r"quick edit|small change|one-line|change variable|update constant|boilerplate|"
    r"simple explanation|print statement|add log)\b",
    re.IGNORECASE
)

RE_COMPLEX_DEBUG = re.compile(
    r"\b(debug|debugging|bug|bugs|crash|crashes|crashing|freeze|freezes|segfault|core dump|"
    r"race condition|deadlock|memory leak|stack overflow|null pointer|undefined is not|"
    r"subtle|intermittent|flaky|production only|why does it fail|heisenbug|"
    r"concurrency bug|regression|investigate error|traceback|panic|exception|"
    r"complicated|troubleshoot|diagnose|hard to reproduce|unexpected behavior)\b",
    re.IGNORECASE
)

RE_EXTREME_REASONING = re.compile(
    r"\b(architect|architecture|system design|redesign|rearchitect|distributed|"
    r"consensus|cryptographic|microservices migration|scaling to|tradeoff analysis|"
    r"complex algorithm|dynamic programming|formal verification|kernel|compiler design|"
    r"system-wide overhaul|failed multiple times|exhausted attempts|large application architecture)\b",
    re.IGNORECASE
)

RE_LOCAL_OFFLINE = re.compile(
    r"\b(local|offline|private|confidential|proprietary|glm|ollama|on-device|"
    r"airgap|air-gapped|no cloud|internal data|sensitive)\b",
    re.IGNORECASE
)

RE_PARALLEL = re.compile(
    r"\b(and\b|both\b|frontend and backend|client and server|api and ui|ui and backend|"
    r"tests and implementation|documentation and code|modules concurrently|independent components)\b",
    re.IGNORECASE
)

RE_NORMAL_DEV = re.compile(
    r"\b(implement|create|build|develop|feature|endpoint|api|component|service|"
    r"controller|route|crud|database|schema|script|function|class|method|"
    r"write tests|unit test|integration test|refactor|optimize)\b",
    re.IGNORECASE
)


class EndpointHealthChecker:
    """Fast non-blocking cached health checker with 0.3s timeout"""
    _cache: Dict[str, Tuple[bool, float]] = {}
    CACHE_TTL_SEC = 5.0

    @classmethod
    def check_url(cls, url: str, path: str = "/health", timeout: float = 0.3) -> bool:
        cache_key = url + path
        now = time.time()
        if cache_key in cls._cache:
            is_up, cached_at = cls._cache[cache_key]
            if (now - cached_at) < cls.CACHE_TTL_SEC:
                return is_up

        full_url = f"{url.rstrip('/')}{path}"
        try:
            req = urllib.request.Request(
                full_url,
                headers={"User-Agent": "Antigravity-Router/1.0"}
            )
            with urllib.request.urlopen(req, timeout=timeout) as res:
                is_up = (200 <= res.status < 300)
                cls._cache[cache_key] = (is_up, now)
                return is_up
        except Exception:
            cls._cache[cache_key] = (False, now)
            return False

    @classmethod
    def get_healthy_local_endpoint(cls, force_down: bool = False) -> Optional[str]:
        """Returns the healthiest local GLM endpoint, or None if unavailable."""
        if force_down:
            return None
        # 1. Try Primary: llama-server on port 8080
        if cls.check_url(LOCAL_ENDPOINTS["PRIMARY"], "/health"):
            return LOCAL_ENDPOINTS["PRIMARY"]
        # 2. Try Secondary: Ollama on port 11434
        if cls.check_url(LOCAL_ENDPOINTS["FALLBACK"], "/api/tags"):
            return LOCAL_ENDPOINTS["FALLBACK"]
        return None


class AutoRouter:
    """Core deterministic routing logic"""

    @classmethod
    def classify_and_route(
        cls,
        prompt: str,
        files_count: int = 1,
        prior_failures: int = 0,
        force_local: bool = False,
        simulate_local_down: bool = False
    ) -> Dict[str, Any]:
        start_time = time.perf_counter_ns()
        prompt_clean = prompt.strip()

        # Check escalation first: if task has failed repeatedly, escalate automatically
        if prior_failures >= 2:
            return cls._build_result(
                category="ESCALATED_DEBUGGING",
                target_agent=AGENTS["DEEP_DEBUGGER"],
                target_model=MODELS["CLAUDE_SONNET"],
                fallback_model=MODELS["GEMINI_PRO"],
                reason=f"Auto-escalated after {prior_failures} verification/test failures.",
                parallel=False,
                start_time=start_time
            )

        # Signal 1: Local / Offline explicit requirement
        if force_local or RE_LOCAL_OFFLINE.search(prompt_clean):
            local_ep = EndpointHealthChecker.get_healthy_local_endpoint(force_down=simulate_local_down)
            if local_ep:
                return cls._build_result(
                    category="LOCAL_OFFLINE",
                    target_agent=AGENTS["LOCAL_GLM_CODER"],
                    target_model=MODELS["LOCAL_GLM"],
                    fallback_model=MODELS["GEMINI_FLASH"],
                    reason=f"Local/private task requested. Healthy endpoint: {local_ep}",
                    parallel=False,
                    endpoint=local_ep,
                    start_time=start_time
                )
            else:
                return cls._build_result(
                    category="LOCAL_OFFLINE_FALLBACK",
                    target_agent=AGENTS["FAST_CODER"],
                    target_model=MODELS["GEMINI_FLASH"],
                    fallback_model=MODELS["GEMINI_PRO"],
                    reason="Local GLM offline; graceful fallback to Gemini Flash.",
                    parallel=False,
                    start_time=start_time
                )

        # Signal 2: Parallelizable task detection
        is_parallel_candidate = (
            files_count > 2 or
            ("frontend" in prompt_clean.lower() and "backend" in prompt_clean.lower()) or
            RE_PARALLEL.search(prompt_clean)
        )
        if is_parallel_candidate and len(prompt_clean) > 30 and not RE_VERY_SIMPLE.search(prompt_clean):
            subtasks = cls._decompose_parallel_tasks(prompt_clean)
            if len(subtasks) > 1:
                return cls._build_result(
                    category="PARALLELIZABLE",
                    target_agent=AGENTS["AUTO_ROUTER"],
                    target_model=MODELS["GEMINI_FLASH"],
                    fallback_model=MODELS["GEMINI_PRO"],
                    reason=f"Task cleanly splits into {len(subtasks)} independent concurrent subagents.",
                    parallel=True,
                    subtasks=subtasks,
                    start_time=start_time
                )

        # Signal 3: Extreme reasoning / Architecture
        if RE_EXTREME_REASONING.search(prompt_clean):
            return cls._build_result(
                category="EXTREME_REASONING",
                target_agent=AGENTS["ARCHITECTURE_EXPERT"],
                target_model=MODELS["CLAUDE_OPUS"],
                fallback_model=MODELS["GEMINI_PRO"],
                reason="High-level architecture, complex algorithm, or major redesign detected.",
                parallel=False,
                start_time=start_time
            )

        # Signal 4: Complex debugging / Subtle bugs
        if RE_COMPLEX_DEBUG.search(prompt_clean):
            return cls._build_result(
                category="COMPLEX_DEBUGGING",
                target_agent=AGENTS["DEEP_DEBUGGER"],
                target_model=MODELS["CLAUDE_SONNET"],
                fallback_model=MODELS["GEMINI_PRO"],
                reason="Complex runtime bug, crash, or subtle logic investigation detected.",
                parallel=False,
                start_time=start_time
            )

        # Signal 5: Very simple / Fast tasks
        if RE_VERY_SIMPLE.search(prompt_clean) and files_count <= 2:
            return cls._build_result(
                category="VERY_SIMPLE_FAST",
                target_agent=AGENTS["FAST_CODER"],
                target_model=MODELS["GEMINI_FLASH"],
                fallback_model=MODELS["GEMINI_PRO"],
                reason="Quick edit, formatting, rename, or boilerplate detected.",
                parallel=False,
                start_time=start_time
            )

        # Signal 6: Normal software development (features, APIs, tests, refactoring)
        if RE_NORMAL_DEV.search(prompt_clean) or files_count <= 5:
            local_ep = EndpointHealthChecker.get_healthy_local_endpoint(force_down=simulate_local_down)
            if local_ep:
                return cls._build_result(
                    category="NORMAL_SOFTWARE_DEV",
                    target_agent=AGENTS["LOCAL_GLM_CODER"],
                    target_model=MODELS["LOCAL_GLM"],
                    fallback_model=MODELS["GEMINI_FLASH"],
                    reason=f"Standard coding task. Using local GLM-4.7-Flash on {local_ep}.",
                    parallel=False,
                    endpoint=local_ep,
                    start_time=start_time
                )
            else:
                return cls._build_result(
                    category="NORMAL_SOFTWARE_DEV",
                    target_agent=AGENTS["FAST_CODER"],
                    target_model=MODELS["GEMINI_FLASH"],
                    fallback_model=MODELS["GEMINI_PRO"],
                    reason="Standard coding task. Local GLM offline; routed to Gemini Flash.",
                    parallel=False,
                    start_time=start_time
                )

        # Default fallback: Fast model (Gemini Flash)
        return cls._build_result(
            category="DEFAULT_FAST",
            target_agent=AGENTS["FAST_CODER"],
            target_model=MODELS["GEMINI_FLASH"],
            fallback_model=MODELS["GEMINI_PRO"],
            reason="General task with no complex triggers. Defaulted to fast model.",
            parallel=False,
            start_time=start_time
        )

    @classmethod
    def _decompose_parallel_tasks(cls, prompt: str) -> List[Dict[str, str]]:
        """Decompose prompt into independent subagent task definitions"""
        tasks = []
        lower = prompt.lower()
        if "backend" in lower and "frontend" in lower:
            tasks.append({
                "agent": AGENTS["LOCAL_GLM_CODER"],
                "model": MODELS["LOCAL_GLM"],
                "focus": "Backend API, service layer, and data validation",
            })
            tasks.append({
                "agent": AGENTS["FAST_CODER"],
                "model": MODELS["GEMINI_FLASH"],
                "focus": "Frontend UI components, view binding, and styling",
            })
            tasks.append({
                "agent": AGENTS["VERIFIER"],
                "model": MODELS["GEMINI_FLASH"],
                "focus": "Integration test suite, linting, and build verification",
            })
            return tasks

        # Generic split on conjunctions if applicable
        parts = re.split(r"\s+(?:and also|and then|and|along with)\s+", prompt, flags=re.IGNORECASE)
        if len(parts) >= 2 and all(len(p.strip()) > 10 for p in parts[:3]):
            for idx, part in enumerate(parts[:3]):
                tasks.append({
                    "agent": AGENTS["LOCAL_GLM_CODER"],
                    "model": MODELS["LOCAL_GLM"],
                    "focus": part.strip(),
                })
            tasks.append({
                "agent": AGENTS["VERIFIER"],
                "model": MODELS["GEMINI_FLASH"],
                "focus": "Automated verification of combined changes",
            })
            return tasks

        return []

    @classmethod
    def _build_result(
        cls,
        category: str,
        target_agent: str,
        target_model: str,
        fallback_model: str,
        reason: str,
        parallel: bool,
        start_time: int,
        endpoint: Optional[str] = None,
        subtasks: Optional[List[Dict[str, str]]] = None
    ) -> Dict[str, Any]:
        duration_ms = (time.perf_counter_ns() - start_time) / 1_000_000.0
        res = {
            "category": category,
            "target_agent": target_agent,
            "target_model": target_model,
            "fallback_model": fallback_model,
            "reason": reason,
            "parallel": parallel,
            "routing_latency_ms": round(duration_ms, 3),
            "timestamp": time.time(),
        }
        if endpoint:
            res["local_endpoint"] = endpoint
        if subtasks:
            res["subtasks"] = subtasks
        return res


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} \"<task description>\" [--files=N] [--failures=N] [--json]")
        sys.exit(1)

    prompt = sys.argv[1]
    files_count = 1
    prior_failures = 0
    as_json = False
    simulate_local_down = False

    for arg in sys.argv[2:]:
        if arg.startswith("--files="):
            try: files_count = int(arg.split("=")[1])
            except ValueError: pass
        elif arg.startswith("--failures="):
            try: prior_failures = int(arg.split("=")[1])
            except ValueError: pass
        elif arg == "--simulate-local-down":
            simulate_local_down = True
        elif arg == "--json":
            as_json = True

    result = AutoRouter.classify_and_route(
        prompt=prompt,
        files_count=files_count,
        prior_failures=prior_failures,
        simulate_local_down=simulate_local_down
    )

    if as_json:
        print(json.dumps(result, indent=2))
    else:
        print(f"--- ANTIGRAVITY AUTO ROUTER ---")
        print(f"Task:            \"{prompt}\"")
        print(f"Classification:  {result['category']}")
        print(f"Target Agent:    {result['target_agent']}")
        print(f"Selected Model:  {result['target_model']}")
        print(f"Fallback Model:  {result['fallback_model']}")
        print(f"Parallel Mode:   {result['parallel']}")
        print(f"Routing Latency: {result['routing_latency_ms']} ms")
        print(f"Reason:          {result['reason']}")
        if "local_endpoint" in result:
            print(f"Local Endpoint:  {result['local_endpoint']}")
        if "subtasks" in result:
            print(f"Subagents ({len(result['subtasks'])}):")
            for i, st in enumerate(result['subtasks'], 1):
                print(f"  [{i}] Agent: {st['agent']} ({st['model']}) -> {st['focus']}")


if __name__ == "__main__":
    main()
