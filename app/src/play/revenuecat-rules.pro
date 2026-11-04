# RevenueCat explicitly catches missing Block Store classes. Its optional anonymous-ID
# cloud backup is excluded so access recovery stays behind the user's Restore action.
-dontwarn com.google.android.gms.auth.blockstore.**
