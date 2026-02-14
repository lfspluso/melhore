@rem Gradle wrapper launcher for Windows.
@rem Use Android Studio to sync and build, or install Gradle and run: gradle wrapper --gradle-version=8.7
@rem Then run: gradlew.bat assembleDebug

@if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  @goto run
)

@echo Gradle wrapper JAR not found. Open the project in Android Studio and sync, or run:
@echo   gradle wrapper --gradle-version=8.7
@echo from the project root if Gradle is installed.
@exit /b 1

:run
set JAVA_EXE=java
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)
"%JAVA_EXE%" -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
@exit /b %ERRORLEVEL%
