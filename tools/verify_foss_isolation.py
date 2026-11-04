"""Fail closed over resolved dependencies, merged manifest and every APK entry.

Extend the denylist when adding a vendor; this is a regression guard, not proof
that an arbitrary unknown SDK is free of remote services.
"""
import argparse
import re
import sys
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


def inspect_packaged_manifest(dump):
    elements = []
    current = None
    for line in dump.splitlines():
        element = re.match(r"\s*E: ([\w-]+)", line)
        attribute = re.match(r'\s*A: (?:(?:android|http://schemas.android.com/apk/res/android):)?([\w.]+)(?:\([^)]*\))?="([^"]*)"', line)
        if element:
            current = (element[1], {})
            elements.append(current)
        elif attribute and current is not None:
            current[1][attribute[1]] = attribute[2]
    requirements = (
        ("manifest", {"package": "com.uc.caffeine"}),
        ("application", {"name": "com.uc.caffeine.CaffeineApplication"}),
        ("meta-data", {"name": "com.uc.caffeine.distribution", "value": "foss"}),
    )
    problems = [f"Packaged manifest lacks {tag}: {attributes}" for tag, attributes in requirements
                if not any(name == tag and all(values.get(key) == value for key, value in attributes.items())
                           for name, values in elements)]
    if any(marker in dump.lower() for marker in ARTIFACT_MARKERS) or any(permission in dump for permission in FORBIDDEN_PERMISSIONS):
        problems.append("Forbidden billing/account/sync component in packaged manifest")
    return problems


DEPENDENCY_MARKERS = (
    "com.revenuecat", "com.android.billingclient", "com.google.android.gms",
    "com.google.android.play", "com.google.firebase", "com.google.ads",
    "com.google.android.ump", "com.facebook.android", "com.appsflyer",
    "com.adjust", "com.amplitude", "com.segment", "io.sentry", "com.mixpanel",
    "supabase", "appwrite", "com.amplifyframework", "com.amazonaws",
    "com.auth0", "com.okta", "com.microsoft.identity", "com.clerk",
)
ARTIFACT_MARKERS = (
    *DEPENDENCY_MARKERS, "api.revenuecat.com", "purchases.revenuecat.com",
    "api.segment.io", "api.amplitude.com", "firebaseio.com", "supabase.co",
    "cloud.appwrite.io", "libappwrite", "com.android.vending.billing",
    # The Play Drive backup talks to these over plain HTTPS, so no dependency would reveal a leak.
    "googleapis.com/drive", "googleapis.com/upload/drive", "auth/drive.appdata",
)
FORBIDDEN_PERMISSIONS = {
    "com.android.vending.BILLING", "android.permission.GET_ACCOUNTS",
    "android.permission.MANAGE_ACCOUNTS", "android.permission.AUTHENTICATE_ACCOUNTS",
    "android.permission.READ_SYNC_SETTINGS", "android.permission.WRITE_SYNC_SETTINGS",
    "android.permission.READ_SYNC_STATS", "com.google.android.gms.permission.AD_ID",
}
ANDROID = "{http://schemas.android.com/apk/res/android}"


def inspect_inputs(dependencies, manifest, apk):
    problems = []
    if not dependencies.strip():
        problems.append("Missing resolved dependency graph")
    for line in dependencies.lower().splitlines():
        if any(marker in line for marker in DEPENDENCY_MARKERS):
            problems.append(f"Forbidden FOSS dependency: {line}")

    root = ET.fromstring(manifest)
    if root.get("package") != "com.uc.caffeine":
        problems.append("Unexpected FOSS release application ID")
    application = root.find("application")
    if application is None or application.get(ANDROID + "name") != "com.uc.caffeine.CaffeineApplication":
        problems.append("Missing application-scoped distribution provider")
    editions = root.findall("application/meta-data")
    if not any(item.get(ANDROID + "name") == "com.uc.caffeine.distribution" and
               item.get(ANDROID + "value") == "foss" for item in editions):
        problems.append("Packaged edition must be FOSS")
    for element in root.iter():
        for value in element.attrib.values():
            if value in FORBIDDEN_PERMISSIONS or any(marker in value.lower() for marker in ARTIFACT_MARKERS):
                problems.append(f"Forbidden manifest value: {value}")

    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if "AndroidManifest.xml" not in names or not any(re.fullmatch(r"classes\d*\.dex", name) for name in names):
            problems.append("APK lacks a manifest or DEX code")
        for name in names:
            content = archive.read(name).lower()
            for marker in ARTIFACT_MARKERS:
                if marker == "com.google.android.gms":
                    # AndroidX's existing downloadable font provider uses these two
                    # package/authority strings without bundling a GMS SDK. Class
                    # descriptors and every other GMS namespace remain forbidden.
                    payloads = (name.lower(), content.decode("latin1"),
                                content.decode("utf-16le", errors="ignore"),
                                content[1:].decode("utf-16le", errors="ignore"))
                    gms_names = [value for payload in payloads for value in
                                 re.findall(r"com[./]google[./]android[./]gms(?:[./][a-z0-9_$]+)*", payload)]
                    if any(value not in ("com.google.android.gms", "com.google.android.gms.fonts") for value in gms_names):
                        problems.append(f"Forbidden GMS code/reference in {name}")
                    continue
                spellings = (marker, marker.replace(".", "/"))
                if any(spelling in name.lower() or
                       spelling.encode() in content or
                       spelling.encode("utf-16le") in content for spelling in spellings):
                    problems.append(f"Forbidden APK marker {marker} in {name}")
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dependencies", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--apk-dir", type=Path, required=True)
    parser.add_argument("--aapt2", type=Path, required=True)
    args = parser.parse_args()
    apks = list(args.apk_dir.glob("*.apk"))
    if len(apks) != 1:
        parser.error(f"Expected exactly one FOSS APK in {args.apk_dir}; found {len(apks)}")
    problems = inspect_inputs(args.dependencies.read_text(encoding="utf-8"),
                              args.manifest.read_text(encoding="utf-8"), apks[0])
    dump = subprocess.run([str(args.aapt2), "dump", "xmltree", "--file", "AndroidManifest.xml", str(apks[0])],
                          check=True, capture_output=True, text=True, encoding="utf-8").stdout
    problems.extend(inspect_packaged_manifest(dump))
    for problem in problems:
        print(problem, file=sys.stderr)
    if problems:
        return 1
    print(f"FOSS isolation passed: resolved graph, merged manifest and all entries in {apks[0]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
