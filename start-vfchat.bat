@echo off
setlocal enabledelayedexpansion

:: VFChatAI Windows Startup Script
:: This script helps you get VFChatAI up and running quickly on Windows

title VFChatAI Setup

echo.
echo 🤖 Welcome to VFChatAI Setup!
echo ================================================
echo.

:: Configuration
set PROJECT_NAME=VFChatAI
set REQUIRED_JAVA_VERSION=17

:: Function to check if command exists
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed or not in PATH
    echo 💡 Please install Java 17+ and add it to your PATH
    pause
    exit /b 1
)

:: Check Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr "version"') do (
    set JAVA_VERSION_STRING=%%i
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "tokens=1 delims=." %%i in ("%JAVA_VERSION_STRING%") do set JAVA_MAJOR=%%i

if %JAVA_MAJOR% geq %REQUIRED_JAVA_VERSION% (
    echo ✅ Java %JAVA_MAJOR% is installed
) else (
    echo ❌ Java %REQUIRED_JAVA_VERSION%+ is required, but Java %JAVA_MAJOR% is installed
    pause
    exit /b 1
)

:: Check Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven is not installed or not in PATH
    echo 💡 Please install Maven and add it to your PATH
    pause
    exit /b 1
) else (
    echo ✅ Maven is installed
)

:: Check PostgreSQL
where psql >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ PostgreSQL is not installed or not in PATH
    echo 💡 Please install PostgreSQL and add it to your PATH
    
    :: Try to start PostgreSQL service
    echo 🔄 Attempting to start PostgreSQL service...
    net start postgresql-x64-15 >nul 2>&1
    if !errorlevel! equ 0 (
        echo ✅ PostgreSQL service started
    ) else (
        net start postgresql-x64-14 >nul 2>&1
        if !errorlevel! equ 0 (
            echo ✅ PostgreSQL service started
        ) else (
            net start postgresql-x64-13 >nul 2>&1
            if !errorlevel! equ 0 (
                echo ✅ PostgreSQL service started
            ) else (
                echo ⚠️  Could not start PostgreSQL service automatically
                echo 💡 Please start PostgreSQL manually
            )
        )
    )
) else (
    echo ✅ PostgreSQL is installed
)

:: Check .env file
if not exist ".env" (
    if exist ".env.example" (
        echo ⚠️  .env file not found, copying from .env.example
        copy .env.example .env >nul
        echo 📝 Please edit .env file with your credentials
        echo.
        echo Opening .env file for editing...
        notepad .env
        echo.
        echo Press any key after configuring your .env file...
        pause >nul
    ) else (
        echo ❌ Neither .env nor .env.example file found
        pause
        exit /b 1
    )
) else (
    echo ✅ .env file exists
    
    :: Check if .env file needs configuration
    findstr /C:"your-email@gmail.com" .env >nul 2>&1
    if !errorlevel! equ 0 (
        echo ⚠️  .env file needs configuration
        echo Opening .env file for editing...
        notepad .env
        echo.
        echo Press any key after configuring your .env file...
        pause >nul
    )
    
    findstr /C:"your-gemini-api-key-here" .env >nul 2>&1
    if !errorlevel! equ 0 (
        echo ⚠️  .env file needs configuration
        echo Opening .env file for editing...
        notepad .env
        echo.
        echo Press any key after configuring your .env file...
        pause >nul
    )
)

:: Parse command line arguments
set ACTION=%1
if "%ACTION%"=="" set ACTION=start

if "%ACTION%"=="start" goto :start
if "%ACTION%"=="setup" goto :setup
if "%ACTION%"=="test" goto :test
if "%ACTION%"=="clean" goto :clean
if "%ACTION%"=="check" goto :check
if "%ACTION%"=="help" goto :help
if "%ACTION%"=="-h" goto :help
if "%ACTION%"=="--help" goto :help

echo ❌ Unknown option: %ACTION%
goto :help

:start
echo.
echo 🔍 Testing database connection...

:: Test database connection (basic check)
echo Testing PostgreSQL connection...
psql -h localhost -U postgres -d postgres -c "SELECT 1;" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Database connection successful
) else (
    echo ⚠️  Could not connect to database
    echo 💡 Make sure PostgreSQL is running and credentials are correct
    echo 💡 Continuing anyway...
)

echo.
echo 🚀 Starting %PROJECT_NAME%...
echo.

:: Clean and compile
echo 📦 Installing dependencies...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ❌ Failed to compile project
    pause
    exit /b 1
)

:: Start the application
echo.
echo ✅ Dependencies installed successfully
echo.
echo 🎯 Launching application...
echo 📱 Access your app at: http://localhost:8080
echo 🌐 Landing page: http://localhost:8080/landingpage.html
echo.
echo 🛑 Press Ctrl+C to stop the application
echo.

call mvn spring-boot:run
goto :end

:setup
echo.
echo 🔧 Setting up %PROJECT_NAME%...

if not exist ".env" (
    copy .env.example .env >nul
    echo ✅ Created .env file from template
)

echo 📦 Installing Maven dependencies...
call mvn clean install -DskipTests -q
if %errorlevel% equ 0 (
    echo ✅ Setup completed!
    echo 📝 Don't forget to update your .env file with actual credentials
) else (
    echo ❌ Setup failed
    pause
    exit /b 1
)
goto :end

:test
echo.
echo 🧪 Running tests...
call mvn test
goto :end

:clean
echo.
echo 🧹 Cleaning build artifacts...
call mvn clean
echo ✅ Clean completed
goto :end

:check
echo.
echo 🔍 System requirements check completed
goto :end

:help
echo.
echo Usage: %0 [OPTION]
echo.
echo Options:
echo   start     Start the application (default)
echo   setup     Setup environment and dependencies
echo   test      Run tests
echo   clean     Clean build artifacts
echo   check     Check system requirements only
echo   help      Show this help message
echo.
echo Examples:
echo   %0                # Start the application
echo   %0 setup          # Setup environment
echo   %0 test           # Run tests
goto :end

:end
echo.
pause
