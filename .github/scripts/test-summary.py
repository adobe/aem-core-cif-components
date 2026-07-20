#!/usr/bin/env python3
#
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Copyright 2026 Adobe
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
# Renders a CircleCI-style test report into the GitHub Actions Job Summary.
#
# GitHub has no native "Tests" tab, so this parses the JUnit XML under
# it/http/target/failsafe-reports (integration jobs) and
# ui.tests/test-module/reports (selenium jobs), and writes a Markdown report to
# $GITHUB_STEP_SUMMARY:
#   - an overview table (Total / Passed / Failed / Skipped)
#   - for each failed test: class, assertion message, stack trace, and the
#     captured server log (<system-out>) inside a collapsible <details> block.
#
# failsafe-summary.xml (maven-failsafe-plugin's separate aggregate-counts file) is
# skipped automatically: its root tag is neither testsuites nor testsuite.
#
# Ported from aem-cif-guides-venia's .github/ci/test-summary.py.

import glob
import os
import xml.etree.ElementTree as ET

# GitHub's step summary caps at ~1 MB. Keep each test's captured log bounded so a
# few very chatty tests can't blow the budget (we keep the TAIL, where failures land).
MAX_LOG_CHARS = 30000
# Hard ceiling on the whole document, leaving headroom under the 1 MB limit.
MAX_TOTAL_CHARS = 900000

job = os.environ.get("GITHUB_JOB", "tests")
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")

xml_files = sorted(
    glob.glob("it/http/target/failsafe-reports/*.xml")
    + glob.glob("ui.tests/test-module/reports/**/*.xml", recursive=True)
)

total = passed = skipped = 0
failed = []  # list of dicts: classname, name, message, trace, sysout


def text_of(node):
    return (node.text or "") if node is not None else ""


for path in xml_files:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    if root.tag == "testsuites":
        suites = root.findall("testsuite")
    elif root.tag == "testsuite":
        suites = [root]
    else:
        # e.g. failsafe-summary.xml — not a test suite, skip it.
        continue

    for suite in suites:
        for case in suite.findall("testcase"):
            total += 1
            fail = case.find("failure")
            err = case.find("error")
            skip = case.find("skipped")
            problem = fail if fail is not None else err
            if problem is not None:
                sysout = text_of(case.find("system-out")).strip()
                failed.append(
                    {
                        "classname": case.get("classname", ""),
                        "name": case.get("name", ""),
                        "message": (problem.get("message") or "").strip(),
                        "trace": text_of(problem).strip(),
                        "sysout": sysout,
                    }
                )
            elif skip is not None:
                skipped += 1
            else:
                passed += 1

out = []
out.append("## 🧪 {} — Test Results\n".format(job))

if not xml_files:
    out.append(
        "> ⚠️ **No JUnit XML reports found.** The tests almost certainly did not "
        "run — this is an environment/startup failure, not a test failure. "
        "Check the `run-containerized-test.sh` step log and the AEM logs "
        "in the uploaded `*-reports` artifact.\n"
    )
else:
    out.append("| Total | ✅ Passed | ❌ Failed | ⏭️ Skipped |")
    out.append("|------:|----------:|----------:|-----------:|")
    out.append("| {} | {} | {} | {} |\n".format(total, passed, len(failed), skipped))

    if failed:
        out.append("### ❌ {} failed\n".format(len(failed)))
        for tc in failed:
            out.append("#### `{}`".format(tc["name"]))
            out.append("_{}_\n".format(tc["classname"]))
            if tc["message"]:
                out.append("> {}\n".format(tc["message"].replace("\n", " ")))
            detail = tc["trace"]
            if tc["sysout"]:
                log = tc["sysout"]
                if len(log) > MAX_LOG_CHARS:
                    log = "…(log truncated — see the *-reports artifact for the full log)…\n" + log[-MAX_LOG_CHARS:]
                detail = (detail + "\n\n===== captured server log (system-out) =====\n" + log).strip()
            out.append("<details><summary>Stack trace &amp; server log</summary>\n")
            out.append("```")
            out.append(detail if detail else "(no stack trace captured)")
            out.append("```")
            out.append("</details>\n")
    else:
        out.append("All tests passed. 🎉\n")

report = "\n".join(out)
if len(report) > MAX_TOTAL_CHARS:
    report = report[:MAX_TOTAL_CHARS] + "\n\n> …(summary truncated to fit GitHub's size limit)…\n"

# Write to the Job Summary page (rendered as Markdown) and echo to the step log.
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as fh:
        fh.write(report + "\n")
print(report)
