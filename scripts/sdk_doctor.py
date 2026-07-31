#!/usr/bin/env python3
"""
Bidscube SDK Doctor — build/release and archive validation for AppLovin-SDK-for-BidsCube-Android.

Python 3 only. No pip dependencies.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, List, Optional, Sequence, Tuple

FORBIDDEN_REPO_PATHS = [
    ".git/",
    ".gradle/",
    ".kotlin/",
    ".idea/",
    "build/",
    "sdk/build/",
    "applovin-adapter/build/",
    "local.properties",
    ".DS_Store",
    "__MACOSX/",
]

FORBIDDEN_ARCHIVE_PREFIXES = [
    ".git/",
    ".gradle/",
    ".kotlin/",
    ".idea/",
    "build/",
    "local.properties",
    ".DS_Store",
    "__MACOSX/",
    "._",
]

PRIVATE_KEY_PATHS = [
    "keys/private-gpg.asc",
    "keys/private-gpg.asc.base64",
]

FORBIDDEN_STRINGS = [
    b"bidscube_test_signal",
    b"Bidscube Native Ad",
    b"Native ad from Bidscube",
    b"Learn More",
]

ADAPTER_NATIVE_FORBIDDEN = [
    b"MaxNativeAdAdapter",
    b"loadNativeAd",
]

REQUIRED_FILES = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "sdk/build.gradle.kts",
    "applovin-adapter/build.gradle.kts",
    "sdk/src/main/java/com/bidscube/sdk/BidscubeSDK.java",
    "applovin-adapter/src/main/java/com/applovin/mediation/adapters/BidscubeMediationAdapter.java",
    "README.md",
    "RELEASE.md",
    "CHANGELOG.md",
    "scripts/sdk_doctor.py",
]

REQUIRED_SDK_METHODS = [
    "collectSignal",
    "clearPreloadCache",
    "setMediationAdapterVersion",
    "initialize",
    "preloadImageAd",
    "preloadInterstitialVideoAd",
    "preloadRewardedVideoAd",
    "showInterstitialVideoAd",
    "showRewardedVideoAd",
]

ADAPTER_INTERFACES = [
    "MaxAdViewAdapter",
    "MaxInterstitialAdapter",
    "MaxRewardedAdapter",
    "MaxSignalProvider",
]

ADAPTER_FORBIDDEN_INTERFACE = "MaxNativeAdAdapter"

POM_MAPPING = {
    "applovin-bidscube-max-adapter-lite-no-video": "sdk-lite-no-video",
    "applovin-bidscube-max-adapter-webview-video": "sdk-webview-video",
    "applovin-bidscube-max-adapter-legacy-media-video": "sdk-legacy-media-video",
    "applovin-bidscube-max-adapter-full-video": "sdk-full-video",
}

OPENRTB_PARSER_CLASSES = [
    "OpenRtbResponseNormalizer.java",
    "OpenRtbVideoObjectParser.java",
    "OpenRtbBidCandidate.java",
    "PoddedPlaybackPlanBuilder.java",
    "VastAdSequenceParser.java",
    "VastPodComposer.java",
    "VideoAdPayloadResolver.java",
]

OPENRTB_FALSE_CLAIM_PATTERNS = [
    re.compile(r"OpenRTB\s+2\.6\s+supported", re.I),
    re.compile(r"podded\s+video\s+supported", re.I),
    re.compile(r"seatbid\s+supported", re.I),
    re.compile(r"bids\[\]\s+supported", re.I),
]

OPENRTB_NOT_IMPL_PATTERNS = [
    re.compile(r"OpenRTB.*not\s+implemented", re.I),
    re.compile(r"not\s+implemented.*OpenRTB", re.I),
]

SECRET_GRADLE_PATTERNS = [
    "mavenCentralPassword",
    "signing.password",
    "sonatype",
    "token",
    "secret",
    "private",
]

VERSION_SDK_RE = re.compile(
    r'val\s+sdkVersionString\s*=\s*System\.getenv\("BidscubeVersion"\)\s*\?:\s*"([^"]+)"'
)
VERSION_ADAPTER_RE = re.compile(
    r'val\s+adapterVersion\s*=\s*System\.getenv\("BidscubeAdapterVersion"\)\s*\?:\s*"([^"]+)"'
)
ADAPTER_VERSION_JAVA_RE = re.compile(
    r'getVersionString\(BidscubeMediationAdapter\.class,\s*"([^"]+)"\)'
)
ADAPTER_VERSION_BUILDCONFIG_RE = re.compile(
    r'return\s+BuildConfig\.VERSION_NAME\s*;'
)


@dataclass
class Check:
    category: str
    status: str  # PASS, WARN, FAIL, INFO
    title: str
    message: str = ""
    fix: str = ""

    @property
    def critical(self) -> bool:
        return self.status == "FAIL"


@dataclass
class Report:
    repo: Path
    checks: List[Check] = field(default_factory=list)

    def add(self, check: Check) -> None:
        self.checks.append(check)

    def has_failures(self) -> bool:
        return any(c.status == "FAIL" for c in self.checks)

    def summary_counts(self) -> dict:
        counts = {"PASS": 0, "WARN": 0, "FAIL": 0, "INFO": 0}
        for c in self.checks:
            counts[c.status] = counts.get(c.status, 0) + 1
        return counts


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def rel_posix(path: Path, root: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def parse_versions(repo: Path) -> Tuple[str, str]:
    sdk_gradle = read_text(repo / "sdk/build.gradle.kts")
    adapter_gradle = read_text(repo / "applovin-adapter/build.gradle.kts")
    sdk_m = VERSION_SDK_RE.search(sdk_gradle)
    adapter_m = VERSION_ADAPTER_RE.search(adapter_gradle)
    sdk_v = sdk_m.group(1) if sdk_m else ""
    adapter_v = adapter_m.group(1) if adapter_m else ""
    adapter_java = repo / "applovin-adapter/src/main/java/com/applovin/mediation/adapters/BidscubeMediationAdapter.java"
    if adapter_java.is_file():
        adapter_java_text = read_text(adapter_java)
        jm = ADAPTER_VERSION_JAVA_RE.search(adapter_java_text)
        if jm:
            java_v = jm.group(1)
            if adapter_v and java_v != adapter_v:
                pass  # reported separately
            elif not adapter_v:
                adapter_v = java_v
        elif ADAPTER_VERSION_BUILDCONFIG_RE.search(adapter_java_text) and not adapter_v:
            pass  # version comes from adapterVersion in build.gradle.kts
    return sdk_v, adapter_v


def git_tracked_files(repo: Path) -> Optional[List[str]]:
    git_dir = repo / ".git"
    if not git_dir.exists():
        return None
    try:
        result = subprocess.run(
            ["git", "-C", str(repo), "ls-files", "-z"],
            capture_output=True,
            check=False,
        )
        if result.returncode != 0:
            return None
        return [p.decode("utf-8", errors="replace") for p in result.stdout.split(b"\0") if p]
    except OSError:
        return None


def check_repo_hygiene(report: Report, repo: Path, release_mode: bool) -> None:
    tracked = git_tracked_files(repo)
    tracked_set = set(tracked) if tracked is not None else None

    def is_tracked(rel: str) -> bool:
        if tracked_set is None:
            return False
        return rel in tracked_set or any(t.startswith(rel.rstrip("/") + "/") for t in tracked_set)

    # Private keys — always critical on disk or in git
    for key_path in PRIVATE_KEY_PATHS:
        p = repo / key_path
        if p.is_file() or is_tracked(key_path):
            report.add(Check("hygiene", "FAIL", f"Private key in repo: {key_path}",
                             "Never commit GPG private keys.", "Remove file and rotate credentials if exposed."))

    # Forbidden paths — fail if git-tracked or in release archive scan (handled separately)
    forbidden_rel = FORBIDDEN_REPO_PATHS + [k for k in PRIVATE_KEY_PATHS]
    for forbidden in forbidden_rel:
        if forbidden.endswith("/"):
            prefix = forbidden.rstrip("/")
            if tracked_set is not None:
                matches = [t for t in tracked_set if t == prefix or t.startswith(prefix + "/")]
                if matches:
                    report.add(Check("hygiene", "FAIL", f"Forbidden tracked path: {forbidden}",
                                     f"Examples: {', '.join(matches[:3])}",
                                     f"Remove {forbidden} from git history before release."))
            elif release_mode and (repo / prefix).is_dir():
                report.add(Check("hygiene", "FAIL", f"Forbidden directory present: {forbidden}",
                                 "Release tree must not include build caches.", f"Delete {forbidden}"))
        else:
            p = repo / forbidden
            if is_tracked(forbidden):
                report.add(Check("hygiene", "FAIL", f"Forbidden tracked file: {forbidden}",
                                 "Must not ship in source release.", f"Remove {forbidden} from git."))
            elif forbidden == "local.properties" and p.is_file():
                report.add(Check("hygiene", "WARN", "local.properties present on disk",
                                 "Expected for local Android builds; must not be committed or archived.",
                                 "Add local.properties to .gitignore and exclude from release ZIP."))
            elif forbidden == ".DS_Store" and p.is_file() and not is_tracked(forbidden):
                report.add(Check("hygiene", "WARN", ".DS_Store present on disk",
                                 "Remove before creating release archives.", "Delete .DS_Store files."))
            elif release_mode and p.is_file() and forbidden not in ("local.properties", ".DS_Store"):
                report.add(Check("hygiene", "FAIL", f"Forbidden file in release tree: {forbidden}"))

    # AppleDouble / resource fork — scan tracked and filesystem
    if tracked_set is not None:
        for t in tracked_set:
            base = os.path.basename(t)
            if base.startswith("._") or "__MACOSX" in t or t.endswith(".DS_Store"):
                report.add(Check("hygiene", "FAIL", f"Forbidden macOS junk tracked: {t}",
                                 "Clean before release.", "Remove from git and .gitignore."))
    else:
        for path in repo.rglob("._*"):
            if path.is_file() and "build/" not in rel_posix(path, repo):
                report.add(Check("hygiene", "FAIL", f"Resource fork file: {rel_posix(path, repo)}",
                                 "._* files must not ship.", "Delete and exclude from archives."))

    gradle_props = repo / "gradle.properties"
    if gradle_props.is_file():
        content = read_text(gradle_props).lower()
        for pat in SECRET_GRADLE_PATTERNS:
            if pat.lower() in content:
                report.add(Check("hygiene", "WARN", f"gradle.properties may contain secret: {pat}",
                                 "Review before sharing source.", "Use env vars or local gradle.properties."))
    else:
        report.add(Check("hygiene", "INFO", "gradle.properties", "Not present (OK for CI)."))


def check_structure(report: Report, repo: Path) -> None:
    for rel in REQUIRED_FILES:
        p = repo / rel
        if p.is_file():
            report.add(Check("structure", "PASS", f"Required file: {rel}"))
        else:
            report.add(Check("structure", "FAIL", f"Missing required file: {rel}",
                             "Project layout incomplete.", f"Restore {rel}"))

    settings = read_text(repo / "settings.gradle.kts") if (repo / "settings.gradle.kts").is_file() else ""
    for inc in ['include(":sdk")', 'include(":applovin-adapter")']:
        if inc in settings:
            report.add(Check("structure", "PASS", f"settings.gradle.kts contains {inc}"))
        else:
            report.add(Check("structure", "FAIL", f"settings.gradle.kts missing {inc}"))

    if 'include(":bidscube-testapp-android")' in settings and "testAppDir.exists()" not in settings:
        report.add(Check("structure", "FAIL", "Test app included unconditionally",
                         "Test app must be optional via testAppDir.exists().",
                         "Use conditional include in settings.gradle.kts"))
    elif "testAppDir.exists()" in settings:
        report.add(Check("structure", "PASS", "Test app optional include", "Conditional test app wiring OK."))


def check_versions(report: Report, repo: Path, staged: Optional[Path]) -> Tuple[str, str]:
    sdk_v, adapter_v = parse_versions(repo)
    if sdk_v:
        report.add(Check("version", "INFO", f"Parsed SDK version: {sdk_v}"))
    else:
        report.add(Check("version", "FAIL", "Could not parse SDK version",
                         "sdk/build.gradle.kts missing BidscubeVersion default."))
    if adapter_v:
        report.add(Check("version", "INFO", f"Parsed adapter version: {adapter_v}"))
    else:
        report.add(Check("version", "FAIL", "Could not parse adapter version"))

    if sdk_v and adapter_v and sdk_v != adapter_v:
        report.add(Check("version", "WARN", "SDK and adapter versions differ",
                         f"SDK={sdk_v} adapter={adapter_v}",
                         "Align versions unless intentional hotfix."))

    readme = read_text(repo / "README.md") if (repo / "README.md").is_file() else ""
    if sdk_v and sdk_v not in readme:
        report.add(Check("version", "WARN", "README may reference old SDK version",
                         f"Expected {sdk_v} in README examples."))

    if staged and staged.is_dir() and sdk_v and adapter_v:
        expected = [
            f"bidscube-sdk-lite-no-video-{sdk_v}.aar",
            f"bidscube-sdk-webview-video-{sdk_v}.aar",
            f"bidscube-sdk-legacy-media-video-{sdk_v}.aar",
            f"bidscube-sdk-full-video-{sdk_v}.aar",
            f"applovin-bidscube-max-adapter-lite-no-video-{adapter_v}.aar",
            f"applovin-bidscube-max-adapter-webview-video-{adapter_v}.aar",
            f"applovin-bidscube-max-adapter-legacy-media-video-{adapter_v}.aar",
            f"applovin-bidscube-max-adapter-full-video-{adapter_v}.aar",
        ]
        for name in expected:
            p = staged / name
            if p.is_file():
                report.add(Check("staged_aar", "PASS", f"Staged AAR present: {name}"))
            else:
                report.add(Check("staged_aar", "FAIL", f"Missing staged AAR: {name}",
                                 f"Expected under {staged}", "Run ./gradlew stageAllReleaseAars"))
        extras = [f.name for f in staged.glob("*.aar") if f.name not in expected]
        if extras:
            report.add(Check("staged_aar", "WARN", "Unexpected staged AAR files",
                             ", ".join(sorted(extras)),
                             "Clean build/staged-aars or rebuild with ./gradlew clean sdkDoctorRelease"))
    elif staged:
        report.add(Check("staged_aar", "FAIL", f"Staged AAR directory missing: {staged}"))

    return sdk_v, adapter_v


def check_gradle_deps(report: Report, repo: Path) -> None:
    adapter_gradle = read_text(repo / "applovin-adapter/build.gradle.kts")
    if 'project(":sdk")' in adapter_gradle:
        report.add(Check("gradle", "PASS", "Adapter depends on :sdk"))
    else:
        report.add(Check("gradle", "FAIL", "Adapter missing project(\":sdk\") dependency"))

    if "com.applovin:applovin-sdk" in adapter_gradle:
        report.add(Check("gradle", "PASS", "AppLovin SDK dependency declared"))
        if "@aar" in adapter_gradle:
            report.add(Check("gradle", "WARN", "AppLovin @aar in adapter build",
                             "Transitive deps may not propagate to host app.",
                             "Document that apps must add applovin-sdk explicitly if needed."))
    else:
        report.add(Check("gradle", "FAIL", "Missing com.applovin:applovin-sdk dependency"))

    for adapter_id, sdk_id in POM_MAPPING.items():
        if adapter_id in adapter_gradle and sdk_id in adapter_gradle:
            report.add(Check("gradle", "PASS", f"POM mapping: {adapter_id} -> {sdk_id}"))
        else:
            report.add(Check("gradle", "FAIL", f"Missing POM flavor mapping for {adapter_id} -> {sdk_id}"))

    for doc_rel in ("README.md", "docs/guide.md", "applovin-adapter/README.md"):
        doc = repo / doc_rel
        if not doc.is_file():
            continue
        readme = read_text(doc)
        if re.search(r'com\.bidscube:[^"\']+@aar', readme):
            if "transitive" not in readme.lower() and "Do not" not in readme:
                report.add(Check("gradle", "WARN", f"{doc_rel} uses @aar coordinates",
                                 "Maven @aar may drop transitive SDK dependency.",
                                 "Prefer Maven coordinates without @aar or document transitive SDK."))


def check_adapter_source(report: Report, repo: Path) -> None:
    adapter_path = repo / "applovin-adapter/src/main/java/com/applovin/mediation/adapters/BidscubeMediationAdapter.java"
    if not adapter_path.is_file():
        return
    src = read_text(adapter_path)
    if "class BidscubeMediationAdapter" in src:
        report.add(Check("adapter", "PASS", "BidscubeMediationAdapter class present"))
    for iface in ADAPTER_INTERFACES:
        if iface in src:
            report.add(Check("adapter", "PASS", f"Implements {iface}"))
        else:
            report.add(Check("adapter", "FAIL", f"Missing interface {iface}"))

    if ADAPTER_FORBIDDEN_INTERFACE in src:
        report.add(Check("adapter", "FAIL", "Native MAX adapter interface exposed",
                         "MaxNativeAdAdapter must not be implemented without real native mapping.",
                         "Remove native MAX support or implement fully."))

    for forbidden in FORBIDDEN_STRINGS:
        if forbidden.decode("utf-8", errors="replace") in src:
            report.add(Check("adapter", "FAIL", f"Forbidden string in adapter source: {forbidden!r}",
                             "Production adapter must not contain placeholders.",
                             f"Remove {forbidden!r}"))


def scan_jar_strings(jar_path: Path, forbidden: Sequence[bytes], label: str, report: Report,
                     category: str = "aar_scan") -> None:
    data = jar_path.read_bytes()
    for needle in forbidden:
        if needle in data:
            report.add(Check(category, "FAIL", f"{label}: forbidden string {needle!r}",
                             f"Found in {jar_path.name}", "Remove placeholder / native dummy code."))


def extract_classes_jar(aar_path: Path, dest_dir: Path) -> Optional[Path]:
    try:
        with zipfile.ZipFile(aar_path, "r") as zf:
            if "classes.jar" not in zf.namelist():
                return None
            out = dest_dir / f"{aar_path.stem}-classes.jar"
            out.write_bytes(zf.read("classes.jar"))
            return out
    except zipfile.BadZipFile:
        return None


def javap_has_methods(jar_path: Path, class_name: str, methods: Sequence[str]) -> Tuple[bool, List[str]]:
    missing = []
    if not shutil.which("javap"):
        return True, ["javap not available"]
    try:
        result = subprocess.run(
            ["javap", "-classpath", str(jar_path), "-public", class_name],
            capture_output=True, text=True, check=False,
        )
        output = result.stdout + result.stderr
        if "Error:" in output or result.returncode != 0:
            return False, [f"javap failed for {class_name}: {output.strip()}"]
        for method in methods:
            if method + "(" not in output:
                missing.append(method)
        return len(missing) == 0, missing
    except OSError as e:
        return True, [str(e)]


def check_aar_compatibility(report: Report, staged: Path) -> None:
    if not staged.is_dir():
        return
    sdk_aars = sorted(staged.glob("bidscube-sdk-*-*.aar"))
    adapter_aars = sorted(staged.glob("applovin-bidscube-max-adapter-*-*.aar"))

    with tempfile.TemporaryDirectory(prefix="sdk-doctor-") as tmp:
        tmp_path = Path(tmp)
        for aar in sdk_aars + adapter_aars:
            jar = extract_classes_jar(aar, tmp_path)
            if jar is None:
                report.add(Check("aar_scan", "FAIL", f"No classes.jar in {aar.name}"))
                continue
            scan_jar_strings(jar, FORBIDDEN_STRINGS, aar.name, report)
            if aar.name.startswith("applovin-"):
                scan_jar_strings(jar, ADAPTER_NATIVE_FORBIDDEN, aar.name, report, "adapter_native")

        if not shutil.which("javap"):
            report.add(Check("compat", "WARN", "javap not available",
                             "Skipping SDK method compatibility javap checks.",
                             "Install JDK to enable AAR API verification."))
            return

        sdk_methods = REQUIRED_SDK_METHODS
        for aar in sdk_aars:
            jar = extract_classes_jar(aar, tmp_path)
            if jar is None:
                continue
            ok, missing = javap_has_methods(jar, "com.bidscube.sdk.BidscubeSDK", sdk_methods)
            if ok:
                report.add(Check("compat", "PASS", f"SDK API OK: {aar.name}"))
            else:
                for m in missing:
                    if m == "javap not available":
                        continue
                    report.add(Check("compat", "FAIL", f"{aar.name} missing BidscubeSDK.{m}",
                                     "Adapter/SDK AAR mismatch risk (NoSuchMethodError).",
                                     "Rebuild SDK and adapter with aligned versions."))


def check_openrtb_docs(report: Report, repo: Path) -> None:
    openrtb_dir = repo / "sdk/src/main/java/com/bidscube/sdk/openrtb"
    parser_present = openrtb_dir.is_dir() and any(openrtb_dir.glob("*.java"))
    if not parser_present:
        for cls in OPENRTB_PARSER_CLASSES:
            if (repo / f"sdk/src/main/java/com/bidscube/sdk/openrtb/{cls}").is_file():
                parser_present = True
                break

    doc_files = [repo / "README.md", repo / "applovin-adapter/README.md", repo / "CHANGELOG.md"]
    claims_support = False
    claims_not_impl = False
    for doc in doc_files:
        if not doc.is_file():
            continue
        text = read_text(doc)
        for pat in OPENRTB_FALSE_CLAIM_PATTERNS:
            if pat.search(text):
                claims_support = True
        for pat in OPENRTB_NOT_IMPL_PATTERNS:
            if pat.search(text):
                claims_not_impl = True

    if parser_present:
        report.add(Check("openrtb", "INFO", "OpenRTB parser source present"))
    else:
        report.add(Check("openrtb", "PASS", "OpenRTB parser not in source (expected for current release)"))

    if claims_support and not parser_present:
        report.add(Check("openrtb", "FAIL", "Docs claim OpenRTB support without parser",
                         "OpenRTB parser package missing.",
                         "Remove false claims or implement sdk/openrtb parser."))
    elif claims_not_impl and not parser_present:
        report.add(Check("openrtb", "PASS", "Docs state OpenRTB not implemented"))
    elif not claims_not_impl and not parser_present:
        report.add(Check("openrtb", "WARN", "Docs should state OpenRTB not implemented"))


def check_workflows(report: Report, repo: Path) -> None:
    for wf_name in ("publish.yml", "release-applovin-adapter.yml"):
        wf = repo / ".github/workflows" / wf_name
        if not wf.is_file():
            report.add(Check("ci", "WARN", f"Missing workflow: {wf_name}"))
            continue
        text = read_text(wf)
        report.add(Check("ci", "PASS", f"Workflow present: {wf_name}"))

        if "Require tag ref for manual runs" in text or "refs/heads/" in text:
            report.add(Check("ci", "PASS", f"{wf_name}: manual branch guard"))
        else:
            report.add(Check("ci", "FAIL", f"{wf_name}: missing manual branch guard"))

        if "BidscubeVersion / BidscubeAdapterVersion would be empty" in text or "Verify release version" in text:
            report.add(Check("ci", "PASS", f"{wf_name}: empty version guard"))
        elif "Could not derive version" in text:
            report.add(Check("ci", "PASS", f"{wf_name}: version derivation check"))

        if "Failed to install Android SDK packages" in text:
            report.add(Check("ci", "PASS", f"{wf_name}: SDK install fails hard"))
        elif '|| true' in text and "platforms;android-36" in text:
            report.add(Check("ci", "FAIL", f"{wf_name}: silent SDK platform install (|| true)"))

        if "sdkDoctorRelease" in text or "sdk_doctor.py" in text:
            report.add(Check("ci", "PASS", f"{wf_name}: runs SDK Doctor"))
        else:
            report.add(Check("ci", "WARN", f"{wf_name}: SDK Doctor not referenced",
                             "Add ./gradlew sdkDoctorRelease before release."))


def check_archive(report: Report, archive: Path) -> None:
    if not archive.is_file():
        report.add(Check("archive", "FAIL", f"Archive not found: {archive}"))
        return
    try:
        with zipfile.ZipFile(archive, "r") as zf:
            for name in zf.namelist():
                norm = name.replace("\\", "/")
                for prefix in FORBIDDEN_ARCHIVE_PREFIXES:
                    if prefix.endswith("/"):
                        if f"/{prefix}" in f"/{norm}/" or norm.startswith(prefix) or f"/{prefix.lstrip('/')}" in norm:
                            if prefix.rstrip("/") in norm.split("/"):
                                report.add(Check("archive", "FAIL", f"Forbidden archive entry: {norm}",
                                                 f"Matches {prefix}", "Rebuild clean archive."))
                    elif norm.endswith(prefix) or f"/{prefix}" in norm:
                        report.add(Check("archive", "FAIL", f"Forbidden archive entry: {norm}"))
                for key in PRIVATE_KEY_PATHS:
                    if norm.endswith(key) or key in norm:
                        report.add(Check("archive", "FAIL", f"Private key in archive: {norm}"))
            report.add(Check("archive", "PASS", f"Archive scanned: {archive.name}",
                             f"{len(zf.namelist())} entries"))
    except zipfile.BadZipFile:
        report.add(Check("archive", "FAIL", f"Invalid ZIP: {archive}"))


def run_doctor(repo: Path, staged: Optional[Path], archive: Optional[Path], strict: bool) -> Report:
    report = Report(repo=repo)
    release_mode = staged is not None or archive is not None
    check_repo_hygiene(report, repo, release_mode)
    check_structure(report, repo)
    check_versions(report, repo, staged)
    check_gradle_deps(report, repo)
    check_adapter_source(report, repo)
    check_openrtb_docs(report, repo)
    check_workflows(report, repo)
    if staged:
        check_aar_compatibility(report, staged)
    if archive:
        check_archive(report, archive)
    return report


def print_console(report: Report) -> None:
    counts = report.summary_counts()
    print("Bidscube SDK Doctor")
    print(f"Repo: {report.repo}")
    print(f"Summary: PASS={counts['PASS']} WARN={counts['WARN']} FAIL={counts['FAIL']} INFO={counts['INFO']}")
    print()
    for c in report.checks:
        line = f"[{c.status}] {c.category}: {c.title}"
        print(line)
        if c.message:
            print(f"       {c.message}")
        if c.fix:
            print(f"       Fix: {c.fix}")


def write_json(report: Report, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "repo": str(report.repo),
        "summary": report.summary_counts(),
        "checks": [
            {
                "category": c.category,
                "status": c.status,
                "title": c.title,
                "message": c.message,
                "fix": c.fix,
            }
            for c in report.checks
        ],
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_markdown(report: Report, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    counts = report.summary_counts()
    lines = [
        "# Bidscube SDK Doctor Report",
        "",
        f"**Repo:** `{report.repo}`",
        "",
        f"**Summary:** PASS={counts['PASS']} WARN={counts['WARN']} FAIL={counts['FAIL']} INFO={counts['INFO']}",
        "",
        "| Status | Category | Title | Message |",
        "|--------|----------|-------|---------|",
    ]
    for c in report.checks:
        msg = c.message.replace("|", "\\|").replace("\n", " ")
        lines.append(f"| {c.status} | {c.category} | {c.title} | {msg} |")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def run_self_test() -> int:
    """Minimal script-level tests without pytest."""
    failures = 0
    with tempfile.TemporaryDirectory(prefix="sdk-doctor-selftest-") as tmp:
        root = Path(tmp) / "repo"
        root.mkdir()
        # Minimal fake repo
        (root / "settings.gradle.kts").write_text(
            'include(":sdk")\ninclude(":applovin-adapter")\n'
            'val testAppDir = file("../bidscube-testapp-android")\n'
            'if (testAppDir.exists()) { include(":bidscube-testapp-android") }\n',
            encoding="utf-8",
        )
        for rel in REQUIRED_FILES:
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            if rel.endswith(".gradle.kts"):
                p.write_text('val sdkVersionString = System.getenv("BidscubeVersion") ?: "9.9.9"\n', encoding="utf-8")
            elif rel.endswith("BidscubeMediationAdapter.java"):
                p.write_text(
                    "package com.applovin.mediation.adapters;\n"
                    "public class BidscubeMediationAdapter implements MaxAdViewAdapter {}\n",
                    encoding="utf-8",
                )
            else:
                p.write_text("# stub\n", encoding="utf-8")
        (root / "sdk/build.gradle.kts").write_text(
            'val sdkVersionString = System.getenv("BidscubeVersion") ?: "9.9.9"\n', encoding="utf-8"
        )
        (root / "applovin-adapter/build.gradle.kts").write_text(
            'val adapterVersion = System.getenv("BidscubeAdapterVersion") ?: "9.9.9"\n'
            'project(":sdk")\ncom.applovin:applovin-sdk\n'
            + "\n".join(f"{k} -> {v}" for k, v in POM_MAPPING.items()),
            encoding="utf-8",
        )
        (root / "local.properties").write_text("sdk.dir=/tmp\n", encoding="utf-8")
        (root / "keys").mkdir(parents=True)
        (root / "keys/private-gpg.asc").write_text("PRIVATE\n", encoding="utf-8")

        report = run_doctor(root, staged=None, archive=None, strict=True)
        if not any("Private key" in c.title for c in report.checks if c.status == "FAIL"):
            print("SELF-TEST FAIL: expected private key FAIL")
            failures += 1
        if not any("local.properties" in c.title for c in report.checks):
            print("SELF-TEST FAIL: expected local.properties check")
            failures += 1

        # Archive mode must FAIL on local.properties inside ZIP
        zip_path = Path(tmp) / "bad-release.zip"
        with zipfile.ZipFile(zip_path, "w") as zf:
            zf.writestr("local.properties", "sdk.dir=/tmp\n")
            zf.writestr("README.md", "# ok\n")
        report_archive = run_doctor(root, staged=None, archive=zip_path, strict=True)
        if not any(c.status == "FAIL" and "local.properties" in c.title for c in report_archive.checks):
            print("SELF-TEST FAIL: expected local.properties FAIL in archive mode")
            failures += 1

        # Forbidden string detection
        adapter = root / "applovin-adapter/src/main/java/com/applovin/mediation/adapters/BidscubeMediationAdapter.java"
        adapter.write_text("bidscube_test_signal\n", encoding="utf-8")
        report2 = run_doctor(root, None, None, True)
        if not any("bidscube_test_signal" in c.title for c in report2.checks if c.status == "FAIL"):
            print("SELF-TEST FAIL: expected bidscube_test_signal FAIL")
            failures += 1

        # OpenRTB false claim
        (root / "README.md").write_text("OpenRTB 2.6 supported\n", encoding="utf-8")
        report3 = run_doctor(root, None, None, True)
        if not any(c.status == "FAIL" and "OpenRTB" in c.title for c in report3.checks):
            print("SELF-TEST FAIL: expected OpenRTB false claim FAIL")
            failures += 1

        # Missing staged AARs
        staged = root / "staged"
        staged.mkdir()
        report4 = run_doctor(root, staged, None, True)
        if not any(c.status == "FAIL" and "Missing staged AAR" in c.title for c in report4.checks):
            print("SELF-TEST FAIL: expected missing staged AAR FAIL")
            failures += 1

    if failures == 0:
        print("SELF-TEST PASS")
        return 0
    return 1


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Bidscube SDK Doctor")
    parser.add_argument("--repo", type=Path, default=Path("."), help="Repository root")
    parser.add_argument("--staged-aars", type=Path, default=None, help="Staged AAR directory")
    parser.add_argument("--archive", type=Path, default=None, help="Release ZIP to validate")
    parser.add_argument("--json", type=Path, default=None, help="Write JSON report")
    parser.add_argument("--markdown", type=Path, default=None, help="Write Markdown report")
    parser.add_argument("--strict", action="store_true", help="Treat warnings seriously (exit 1 on FAIL only)")
    parser.add_argument("--self-test", action="store_true", help="Run built-in self tests")
    args = parser.parse_args(argv)

    if args.self_test:
        return run_self_test()

    repo = args.repo.resolve()
    if not repo.is_dir():
        print(f"ERROR: repo not found: {repo}", file=sys.stderr)
        return 1

    report = run_doctor(repo, args.staged_aars, args.archive, args.strict)
    print_console(report)

    if args.json:
        write_json(report, args.json)
        print(f"\nJSON report: {args.json}")
    if args.markdown:
        write_markdown(report, args.markdown)
        print(f"Markdown report: {args.markdown}")

    if report.has_failures():
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
