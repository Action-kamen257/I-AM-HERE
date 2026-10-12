#!/usr/bin/env python3
"""
Test Suite for Antigravity Model Router & Continuous Execution System
Validates all mandatory user test scenarios:
1. Fast task: "Rename this variable" -> fast-coder / Gemini Flash
2. Normal dev: "Implement a normal API endpoint" -> local-glm-coder / GLM-4.7-Flash
3. Deep debug: "Find and fix a complicated bug" -> deep-debugger / Claude Sonnet
4. Extreme reasoning: "Design a large application architecture" -> architecture-expert / Claude Opus
5. Parallelizable task: Two independent components -> parallel subagents
6. Fallback behavior: Local endpoint unavailable -> Gemini Flash
7. Escalation behavior: 2 consecutive failures -> Deep Debugger
8. Training safety gate: PreToolUse interceptor blocks automated training
"""

import sys
import os
import json
import time
import unittest

# Ensure scripts dir is on sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from model_router import AutoRouter, MODELS, AGENTS, EndpointHealthChecker
from router_hooks import handle_pre_tool_use, handle_stop


class TestModelRouter(unittest.TestCase):

    def test_01_fast_task_rename_variable(self):
        """TEST 1: 'Rename this variable' -> Fast model (Gemini Flash)"""
        prompt = "Rename this variable"
        res = AutoRouter.classify_and_route(prompt)

        self.assertEqual(res["category"], "VERY_SIMPLE_FAST")
        self.assertEqual(res["target_agent"], AGENTS["FAST_CODER"])
        self.assertEqual(res["target_model"], MODELS["GEMINI_FLASH"])
        self.assertFalse(res["parallel"])
        self.assertLess(res["routing_latency_ms"], 5.0, "Routing must complete in <5ms")

    def test_02_normal_software_dev(self):
        """TEST 2: 'Implement a normal API endpoint' -> Local GLM or Fast Coder"""
        prompt = "Implement a normal API endpoint"
        res = AutoRouter.classify_and_route(prompt)

        self.assertEqual(res["category"], "NORMAL_SOFTWARE_DEV")
        # If local endpoint is UP, should select local-glm-coder; otherwise fallback
        if "local_endpoint" in res:
            self.assertEqual(res["target_agent"], AGENTS["LOCAL_GLM_CODER"])
            self.assertEqual(res["target_model"], MODELS["LOCAL_GLM"])
        else:
            self.assertEqual(res["target_agent"], AGENTS["FAST_CODER"])
            self.assertEqual(res["target_model"], MODELS["GEMINI_FLASH"])
        self.assertFalse(res["parallel"])

    def test_03_complex_debugging(self):
        """TEST 3: 'Find and fix a complicated bug' -> Stronger debugging model (Claude Sonnet)"""
        prompt = "Find and fix a complicated bug"
        res = AutoRouter.classify_and_route(prompt)

        self.assertEqual(res["category"], "COMPLEX_DEBUGGING")
        self.assertEqual(res["target_agent"], AGENTS["DEEP_DEBUGGER"])
        self.assertEqual(res["target_model"], MODELS["CLAUDE_SONNET"])
        self.assertFalse(res["parallel"])

    def test_04_architecture_and_extreme_reasoning(self):
        """TEST 4: 'Design a large application architecture' -> Strong reasoning model (Claude Opus)"""
        prompt = "Design a large application architecture"
        res = AutoRouter.classify_and_route(prompt)

        self.assertEqual(res["category"], "EXTREME_REASONING")
        self.assertEqual(res["target_agent"], AGENTS["ARCHITECTURE_EXPERT"])
        self.assertEqual(res["target_model"], MODELS["CLAUDE_OPUS"])
        self.assertFalse(res["parallel"])

    def test_05_parallelizable_task(self):
        """TEST 5: Independent components -> Parallel subagents"""
        prompt = "Build authentication backend and frontend dashboard"
        res = AutoRouter.classify_and_route(prompt)

        self.assertEqual(res["category"], "PARALLELIZABLE")
        self.assertEqual(res["target_agent"], AGENTS["AUTO_ROUTER"])
        self.assertTrue(res["parallel"])
        self.assertIn("subtasks", res)
        self.assertGreaterEqual(len(res["subtasks"]), 2)

    def test_06_fallback_behavior(self):
        """TEST 6: Fallback when local GLM is unavailable"""
        prompt = "Implement a normal API endpoint"
        res = AutoRouter.classify_and_route(prompt, simulate_local_down=True)

        self.assertEqual(res["category"], "NORMAL_SOFTWARE_DEV")
        self.assertEqual(res["target_agent"], AGENTS["FAST_CODER"])
        self.assertEqual(res["target_model"], MODELS["GEMINI_FLASH"])
        self.assertIn("Local GLM offline", res["reason"])

    def test_07_auto_escalation(self):
        """TEST 7: Auto-escalation after 2 consecutive failures"""
        prompt = "Calculate tax rate"
        res = AutoRouter.classify_and_route(prompt, prior_failures=2)

        self.assertEqual(res["category"], "ESCALATED_DEBUGGING")
        self.assertEqual(res["target_agent"], AGENTS["DEEP_DEBUGGER"])
        self.assertEqual(res["target_model"], MODELS["CLAUDE_SONNET"])
        self.assertIn("Auto-escalated after 2", res["reason"])

    def test_08_training_safety_gate(self):
        """TEST 8: Training safety gate intercepts automated training commands"""
        # Training command should be gated
        res1 = handle_pre_tool_use({
            "toolCall": {"name": "run_command", "args": {"CommandLine": "python train.py --epochs 10"}}
        })
        self.assertEqual(res1["decision"], "ask")
        self.assertIn("TRAINING SAFETY GATE", res1["reason"])

        # Non-training command should be allowed
        res2 = handle_pre_tool_use({
            "toolCall": {"name": "run_command", "args": {"CommandLine": "python check_hw.py"}}
        })
        self.assertEqual(res2["decision"], "allow")


if __name__ == "__main__":
    unittest.main()
