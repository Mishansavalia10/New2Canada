@echo off
REM Build and run the New2Canada server on http://localhost:8080 (Windows).
REM Needs JDK 17+ and Maven on PATH, or set JAVA_HOME.
REM Optional: set NEW2CANADA_CONTACT=you@example.com and NEW2CANADA_CRAWL=off
setlocal
cd /d "%~dp0"

where java >nul 2>nul || (echo error: Java not found. Install JDK 17+ or set JAVA_HOME. & exit /b 1)
where mvn  >nul 2>nul || (echo error: Maven not found. Install Maven 3.8+. & exit /b 1)

if "%1"=="test"  (mvn -B test & exit /b %errorlevel%)
if "%1"=="build" (mvn -B clean package & exit /b %errorlevel%)

if not exist target\new2canada.jar (
  echo First run: building with Maven...
  mvn -q -B package -DskipTests || exit /b 1
)
echo Starting New2Canada on http://localhost:8080  ^(Ctrl+C to stop^)
java -jar target\new2canada.jar
