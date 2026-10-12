#!/usr/bin/env python3
"""
Antigravity Verifier Script
Autonomous QA and verification tool:
- Inspects changed files
- Checks syntax compilation
- Runs automated tests
- Returns structured JSON failure diagnostics for automated fix loops
"""

import sys
import os
import subprocess
import json
import py_compile
from typing import Dict, Any, List


def check_python_syntax(files: List[str]) -> List[Dict[str, str]]:
    errors = []
    for f in files:
        if f.endswith(".py") and os.path.exists(f):
            try:
                py_compile.compile(f, doraise=True)
            except py_compile.PyCompileError as e:
                errors.append({
                    "file": f,
                    "type": "SyntaxError",
                    "details": str(e)
                })
    return errors


def get_changed_files(workspace_dir: str) -> List[str]:
    try:
        res = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=workspace_dir,
            capture_output=True,
            text=True,
            timeout=5
        )
        changed = []
        for line in res.stdout.splitlines():
            line = line.strip()
            if len(line) > 3:
                filepath = line[3:].strip()
                changed.append(os.path.join(workspace_dir, filepath))
        return changed
    except Exception:
        return []


def run_tests(workspace_dir: str) -> Dict[str, Any]:
    scripts_dir = os.path.join(workspace_dir, "scripts")
    # Targeted fast unit tests for router and workspace
    test_files = [
        "test_model_router.py"
    ]

    results = {
        "tests_found": len(test_files),
        "passed": 0,
        "failed": 0,
        "failures": []
    }

    python_exe = sys.executable

    for tf in test_files:
        p = os.path.join(scripts_dir, tf)
        try:
            res = subprocess.run(
                [python_exe, p],
                cwd=workspace_dir,
                capture_output=True,
                text=True,
                timeout=15
            )
            if res.returncode == 0:
                results["passed"] += 1
            else:
                results["failed"] += 1
                results["failures"].append({
                    "test_file": tf,
                    "returncode": res.returncode,
                    "stdout": res.stdout[-500:],
                    "stderr": res.stderr[-500:]
                })
        except Exception as e:
            results["failed"] += 1
            results["failures"].append({
                "test_file": tf,
                "error": str(e)
            })

    return results


def verify_workspace(workspace_dir: str) -> Dict[str, Any]:
    changed_files = get_changed_files(workspace_dir)
    syntax_errors = check_python_syntax(changed_files)
    test_results = run_tests(workspace_dir)

    is_passed = (len(syntax_errors) == 0 and test_results["failed"] == 0)

    return {
        "status": "PASSED" if is_passed else "FAILED",
        "changed_files_count": len(changed_files),
        "syntax_errors": syntax_errors,
        "test_results": test_results,
        "actionable_feedback": (
            "All verification checks passed." if is_passed else
            f"Fix {len(syntax_errors)} syntax errors and {test_results['failed']} failing tests."
        )
    }


def main():
    workspace = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    verdict = verify_workspace(workspace)
    print(json.dumps(verdict, indent=2))
    sys.exit(0 if verdict["status"] == "PASSED" else 1)


if __name__ == "__main__":
    main()
