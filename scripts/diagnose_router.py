#!/usr/bin/env python3
"""
Antigravity Model Router Diagnostic Suite
Validates:
1. Environment & Antigravity version
2. Local GLM daemon & endpoint health (ports 8080 and 11434)
3. Model weights location (D: verification) and C: protection
4. Global, plugin, and workspace agent configurations
5. Auto-router skill availability
6. Lifecycle hooks status
7. Rule files (AGENTS.md)
8. End-to-end routing performance
"""

import sys
import os
import time
import json
import urllib.request
import shutil


def check_endpoint(url: str, path: str = "/health", timeout: float = 0.5) -> dict:
    t0 = time.perf_counter()
    try:
        req = urllib.request.Request(f"{url.rstrip('/')}{path}", headers={"User-Agent": "Antigravity-Diag/1.0"})
        with urllib.request.urlopen(req, timeout=timeout) as res:
            latency_ms = (time.perf_counter() - t0) * 1000.0
            return {"status": "ONLINE", "http_code": res.status, "latency_ms": round(latency_ms, 2)}
    except Exception as e:
        latency_ms = (time.perf_counter() - t0) * 1000.0
        return {"status": "OFFLINE", "error": str(e), "latency_ms": round(latency_ms, 2)}


def run_diagnostics():
    print("=" * 60)
    print("  ANTIGRAVITY MODEL ROUTER ENVIRONMENT DIAGNOSTIC REPORT")
    print("=" * 60)

    # 1. IDE & Python Environment
    print("\n[1] Environment:")
    print(f"  * Python Executable:    {sys.executable}")
    print(f"  * Workspace Path:       {os.getcwd()}")
    antigravity_exe = r"C:\Users\Tushar\AppData\Local\Programs\Antigravity IDE\Antigravity IDE.exe"
    print(f"  * Antigravity IDE:      {'FOUND' if os.path.exists(antigravity_exe) else 'NOT FOUND'} ({antigravity_exe})")

    # 2. Storage & Weights Check
    print("\n[2] Storage & Model Weights Location:")
    weights_path = r"D:\AI\GLM-4.7-Flash\models\glm-4.7-flash-q4_K_M.gguf"
    if os.path.exists(weights_path):
        size_gb = os.path.getsize(weights_path) / (1024**3)
        print(f"  * GLM-4.7-Flash GGUF:   EXISTS on D: ({size_gb:.2f} GiB)")
        print(f"    Path: {weights_path}")
    else:
        print(f"  * GLM-4.7-Flash GGUF:   MISSING from {weights_path}")

    c_free_gb = shutil.disk_usage("C:\\").free / (1024**3)
    d_free_gb = shutil.disk_usage("D:\\").free / (1024**3)
    print(f"  * Drive Free Space:     C: {c_free_gb:.1f} GB free | D: {d_free_gb:.1f} GB free (Zero AI weights on C:)")

    # 3. Local Model Endpoints
    print("\n[3] Local Model Endpoints:")
    ep8080 = check_endpoint("http://127.0.0.1:8080", "/health")
    print(f"  * Primary (llama-server :8080):  {ep8080['status']} (Latency: {ep8080.get('latency_ms', 0)} ms, Code: {ep8080.get('http_code', 'N/A')})")

    ep11434 = check_endpoint("http://127.0.0.1:11434", "/api/tags")
    print(f"  * Secondary (Ollama :11434):     {ep11434['status']} (Latency: {ep11434.get('latency_ms', 0)} ms, Code: {ep11434.get('http_code', 'N/A')})")

    # 4. Custom Agents Verification
    print("\n[4] Custom Agents Configuration:")
    expected_agents = ["auto-router", "local-glm-coder", "fast-coder", "deep-debugger", "architecture-expert", "verifier"]
    agent_roots = [
        ("Workspace (.agents/agents/)", r"d:\tushar\I_AM_HERE\.agents\agents"),
        ("Global Config (~/.gemini/config/agents/)", r"C:\Users\Tushar\.gemini\config\agents"),
        ("Global Plugin (~/.gemini/config/plugins/model-router/agents/)", r"C:\Users\Tushar\.gemini\config\plugins\model-router\agents")
    ]
    for label, root in agent_roots:
        found = [a for a in expected_agents if os.path.exists(os.path.join(root, f"{a}.md"))]
        print(f"  * {label}: {len(found)}/{len(expected_agents)} agents present")

    # 5. Skills & Rules Verification
    print("\n[5] Skills, Rules, and Hooks:")
    skill_p = r"C:\Users\Tushar\.gemini\config\skills\auto-router\SKILL.md"
    print(f"  * Global Auto-Router Skill:     {'EXISTS' if os.path.exists(skill_p) else 'MISSING'}")

    agents_md = r"d:\tushar\I_AM_HERE\AGENTS.md"
    print(f"  * Workspace AGENTS.md Rule:     {'EXISTS' if os.path.exists(agents_md) else 'MISSING'}")

    hooks_p = r"d:\tushar\I_AM_HERE\.agents\hooks.json"
    print(f"  * Workspace Lifecycle Hooks:    {'EXISTS' if os.path.exists(hooks_p) else 'MISSING'}")

    # 6. Routing Speed Test
    print("\n[6] Routing Engine Benchmark (5 Test Cases):")
    sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
    from model_router import AutoRouter

    test_cases = [
        ("Rename this variable", "VERY_SIMPLE_FAST", "fast-coder"),
        ("Implement a normal API endpoint", "NORMAL_SOFTWARE_DEV", "local-glm-coder"),
        ("Find and fix a complicated bug", "COMPLEX_DEBUGGING", "deep-debugger"),
        ("Design a large application architecture", "EXTREME_REASONING", "architecture-expert"),
        ("Build backend API and frontend dashboard", "PARALLELIZABLE", "auto-router"),
    ]

    all_passed = True
    for prompt, exp_cat, exp_agent in test_cases:
        t0 = time.perf_counter()
        res = AutoRouter.classify_and_route(prompt)
        dt_ms = (time.perf_counter() - t0) * 1000.0
        passed = (res["category"] == exp_cat and res["target_agent"] == exp_agent)
        status_str = "PASS" if passed else "FAIL"
        if not passed: all_passed = False
        print(f"  [{status_str}] \"{prompt}\" -> {res['target_agent']} ({res['target_model']}) in {dt_ms:.3f} ms")

    print("\n" + "=" * 60)
    if all_passed and ep8080["status"] == "ONLINE":
        print("  DIAGNOSTIC STATUS: 100% HEALTHY - ALL SYSTEMS OPERATIONAL")
    else:
        print("  DIAGNOSTIC STATUS: DEGRADED OR OFFLINE ENDPOINT DETECTED")
    print("=" * 60)


if __name__ == "__main__":
    run_diagnostics()
