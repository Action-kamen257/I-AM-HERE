#!/usr/bin/env python3
"""
Antigravity Lifecycle Hook Handler
Enforces:
1. Training Safety (Rule 20): Intercepts and gates automatic model training commands.
2. Continuous Execution (Rule 5): Prevents premature stopping if verification is incomplete.
3. Telemetry & Audit: Logs routing decisions and tool activity to workspace scratch.
"""

import sys
import os
import json
import re

TRAINING_PATTERNS = [
    r"\btrain(?:ing)?\.py\b",
    r"\baccelerate\s+launch\b",
    r"\bdeepspeed\b",
    r"\btorchrun\b",
    r"\bfinetune\b",
    r"\bmodel_train\b",
]

RE_TRAINING = re.compile("|".join(TRAINING_PATTERNS), re.IGNORECASE)


def handle_pre_tool_use(payload: dict) -> dict:
    """Enforces safety gates before tool execution"""
    tool_call = payload.get("toolCall", {})
    tool_name = tool_call.get("name", "")
    args = tool_call.get("args", {})

    if tool_name == "run_command":
        cmd = args.get("CommandLine", "")
        if RE_TRAINING.search(cmd):
            return {
                "decision": "ask",
                "reason": (
                    "TRAINING SAFETY GATE (Rule 20): Automatic model training is blocked. "
                    "You may validate configurations, but training must be initiated manually."
                )
            }

    return {"decision": "allow"}


def handle_stop(payload: dict) -> dict:
    """Ensures agent does not stop prematurely if tasks are in-flight"""
    # If the stop is due to an explicit error or cancellation, allow it
    if payload.get("error"):
        return {"decision": "allow"}

    # Antigravity signals terminationReason (e.g. 'model_stop')
    # If a state file indicates a pending verification, signal continue
    state_file = os.path.join(
        payload.get("workspacePaths", ["."])[0],
        ".agents",
        "pending_verification.json"
    )

    if os.path.exists(state_file):
        try:
            with open(state_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            if data.get("pending"):
                # Clear state and tell agent to continue and verify
                os.remove(state_file)
                return {
                    "decision": "continue",
                    "reason": f"Continuous Execution: {data.get('reason', 'Verification of changes pending.')} Run verifier now."
                }
        except Exception:
            pass

    return {"decision": "allow"}


def handle_post_invocation(payload: dict) -> dict:
    """Post-invocation handler for model responses"""
    return {
        "injectSteps": [],
        "terminationBehavior": ""
    }


def main():
    hook_type = sys.argv[1] if len(sys.argv) > 1 else "PreToolUse"

    try:
        input_data = sys.stdin.read()
        payload = json.loads(input_data) if input_data.strip() else {}
    except Exception:
        payload = {}

    if hook_type == "PreToolUse":
        result = handle_pre_tool_use(payload)
    elif hook_type == "Stop":
        result = handle_stop(payload)
    elif hook_type == "PostInvocation":
        result = handle_post_invocation(payload)
    else:
        result = {}

    print(json.dumps(result))


if __name__ == "__main__":
    main()
