# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to all build types.
-keepattributes *Annotation*
-keepclassmembers class * extends android.content.BroadcastReceiver { *; }
-keepclassmembers class * extends android.app.Service { *; }
