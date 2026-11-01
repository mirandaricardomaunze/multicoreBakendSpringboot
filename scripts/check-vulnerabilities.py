#!/usr/bin/env python3
"""Reprova o build se uma dependência tiver vulnerabilidade conhecida na base OSV.

Excepções em scripts/vulnerability-exceptions.txt: um identificador por linha, com o motivo.
Uso: python scripts/check-vulnerabilities.py
"""
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
MVN = "mvn.cmd" if sys.platform == "win32" else "mvn"
DEPENDENCY = re.compile(r"([\w.-]+):([\w.-]+):jar:(\d[^:\s]*)")


def dependencies():
    found = set()
    for module in ("backend", "desktop"):
        with tempfile.TemporaryDirectory() as folder:
            output = pathlib.Path(folder) / "tree.txt"
            subprocess.run([MVN, "-q", "-pl", module, "-am", "dependency:tree", "-Dscope=runtime",
                            f"-DoutputFile={output}", "-DappendOutput=true"], cwd=ROOT, check=True)
            found.update(DEPENDENCY.findall(output.read_text(encoding="utf-8")))
    return sorted(found)


def query(packages):
    queries = [{"package": {"ecosystem": "Maven", "name": f"{group}:{artifact}"}, "version": version}
               for group, artifact, version in packages]
    request = urllib.request.Request("https://api.osv.dev/v1/querybatch", json.dumps({"queries": queries}).encode(),
                                     {"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)["results"]


def exceptions():
    lines = (ROOT / "scripts" / "vulnerability-exceptions.txt").read_text(encoding="utf-8").splitlines()
    return {line.split()[0] for line in lines if line.strip() and not line.startswith("#")}


def main():
    packages = dependencies()
    allowed = exceptions()
    open_issues = []
    for (group, artifact, version), result in zip(packages, query(packages)):
        for vulnerability in result.get("vulns") or []:
            if vulnerability["id"] not in allowed:
                open_issues.append(f"{group}:{artifact}:{version} {vulnerability['id']}")
    for issue in open_issues:
        print(issue)
    print(f"{len(packages)} dependências verificadas, {len(open_issues)} vulnerabilidades por tratar.")
    return 1 if open_issues else 0


if __name__ == "__main__":
    sys.exit(main())
