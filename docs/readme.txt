flutter run
Launching lib\main.dart on sdk gphone16k x86 64 in debug mode...

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':receive_sharing_intent:compileDebugKotlin'.
> 'compileDebugJavaWithJavac' task (current target is 1.8) and 'compileDebugKotlin' task (current target is 17) jvm target compatibility should be set to the same Java version.
  Consider using JVM toolchain: https://kotl.in/gradle/jvm/toolchain

========>>>>>
I encountered the same issue and found a workaround.

Here are the steps to resolve it:

    Open the "android" project in your Flutter project using Android Studio
    Locate the android/android[receive_sharing_intent] directory and open its build.gradle file
    Add the following compileOptions and kotlinOptions configurations:

android {
...
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlinOptions {
    jvmTarget = "21"
}
}

    Rebuild the project.

Please note that this is a temporary workaround rather than a permanent solution.
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------