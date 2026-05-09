# LibSU rules
-keep class com.topjohnwu.superuser.** { *; }

# Optimization settings
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Add any specific keep rules here if you use reflection or dynamic loading
