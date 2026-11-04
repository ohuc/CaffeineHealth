import tempfile
import unittest
import zipfile
from pathlib import Path

from verify_foss_isolation import inspect_inputs, inspect_packaged_manifest


MANIFEST = '''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.uc.caffeine">
<uses-permission android:name="android.permission.INTERNET"/>
<queries><package android:name="com.google.android.apps.healthdata"/></queries>
<application android:name="com.uc.caffeine.CaffeineApplication">
<meta-data android:name="com.uc.caffeine.distribution" android:value="foss"/>
</application></manifest>'''


class FossIsolationTest(unittest.TestCase):
    def test_packaged_manifest_must_agree_with_merged_foss_manifest(self):
        dump = '''N: android=http://schemas.android.com/apk/res/android
  E: manifest (line=1)
    A: package="com.uc.caffeine" (Raw: "com.uc.caffeine")
    E: application (line=2)
      A: android:name(0x01010003)="com.uc.caffeine.CaffeineApplication"
      E: meta-data (line=3)
        A: android:name(0x01010003)="com.uc.caffeine.distribution"
        A: android:value(0x01010024)="foss" (Raw: "foss")'''
        self.assertEqual([], inspect_packaged_manifest(dump))
        self.assertEqual([], inspect_packaged_manifest(dump.replace("android:", "http://schemas.android.com/apk/res/android:")))
        for altered in (dump.replace('="foss"', '="play"'),
                        dump.replace('package="com.uc.caffeine"', 'package="com.uc.other"'),
                        dump.replace("CaffeineApplication", "OtherApplication"), ""):
            self.assertTrue(inspect_packaged_manifest(altered))

    def inspect(self, dependencies="androidx.health.connect:connect-client:1.1.0", manifest=MANIFEST, entries=None):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "app.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("AndroidManifest.xml", b"compiled manifest")
                archive.writestr("classes.dex", b"Landroidx/health/connect/client/HealthConnectClient;")
                for name, data in (entries or {}).items():
                    archive.writestr(name, data)
            return inspect_inputs(dependencies, manifest, apk)

    def test_allows_health_connect_and_opt_in_barcode_networking(self):
        self.assertEqual([], self.inspect())

    def test_allows_existing_androidx_downloadable_font_provider_metadata(self):
        for content in (b"com.google.android.gms\0com.google.android.gms.fonts\0LocalFontLoader",
                        "com.google.android.gms.fonts\0".encode("utf-16le")):
            self.assertEqual([], self.inspect(entries={"classes2.dex": content}))

    def test_rejects_gms_classes_even_beside_allowed_font_metadata(self):
        self.assertTrue(self.inspect(entries={"classes2.dex": b"com.google.android.gms.fonts\0Lcom/google/android/gms/common/GoogleApi;"}))
        self.assertTrue(self.inspect(entries={"assets/classes": b"com.google.android.gms.auth.api.signin.GoogleSignIn"}))

    def test_rejects_resolved_vendor_dependencies_even_when_artifact_is_minified(self):
        for coordinate in (
            "com.revenuecat.purchases:purchases:10.0", "com.android.billingclient:billing:8.0",
            "com.google.android.gms:play-services-base:1", "com.google.android.play:review:1",
            "com.google.firebase:firebase-auth:1", "com.segment.analytics.kotlin:android:1",
            "io.github.jan-tennert.supabase:auth-kt:1", "io.appwrite:sdk-for-android:1",
            "com.amplifyframework:aws-auth-cognito:1",
        ):
            with self.subTest(coordinate=coordinate):
                self.assertTrue(self.inspect(dependencies=coordinate))

    def test_rejects_vendor_code_resources_and_service_endpoints_in_apk(self):
        for name, data in (
            ("classes2.dex", b"Lcom/android/billingclient/api/BillingClient;"),
            ("classes2.dex", b"Lcom/revenuecat/purchases/Purchases;"),
            ("assets/config.json", b'{"url":"https://api.revenuecat.com/v1"}'),
            ("res/raw/config", "com.google.firebase.auth.FirebaseAuth".encode("utf-16le")),
            ("lib/arm64-v8a/libappwrite.so", b"native code"),
        ):
            with self.subTest(name=name, data=data):
                self.assertTrue(self.inspect(entries={name: data}))

    def test_rejects_billing_or_account_or_sync_manifest_components(self):
        for permission in ("com.android.vending.BILLING", "android.permission.GET_ACCOUNTS", "android.permission.WRITE_SYNC_SETTINGS"):
            manifest = MANIFEST.replace("android.permission.INTERNET", permission)
            with self.subTest(permission=permission):
                self.assertTrue(self.inspect(manifest=manifest))

    def test_rejects_wrong_channel_or_identity(self):
        for manifest in (MANIFEST.replace('android:value="foss"', 'android:value="play"'),
                         MANIFEST.replace('package="com.uc.caffeine"', 'package="com.uc.caffeine.play"')):
            self.assertTrue(self.inspect(manifest=manifest))

    def test_missing_manifest_or_code_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "empty.apk"
            with zipfile.ZipFile(apk, "w"):
                pass
            self.assertTrue(inspect_inputs("", MANIFEST, apk))


if __name__ == "__main__":
    unittest.main()
