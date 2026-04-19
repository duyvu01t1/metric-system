# Metric System - Database Setup Script (PowerShell)
# This script initializes the PostgreSQL database using Flyway

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Metric System - Database Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if PostgreSQL is installed
Write-Host "Checking PostgreSQL connection..." -ForegroundColor Yellow

try {
    $pgVersion = & psql -U postgres -c "SELECT version();" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] PostgreSQL is running" -ForegroundColor Green
    } else {
        throw "PostgreSQL not accessible"
    }
} catch {
    Write-Host "[ERROR] PostgreSQL is not running or not installed" -ForegroundColor Red
    Write-Host "Please ensure PostgreSQL is installed and running" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Create database if not exists
Write-Host ""
Write-Host "Creating database 'metric_system' if it doesn't exist..." -ForegroundColor Yellow

try {
    & psql -U postgres -c "CREATE DATABASE metric_system;" 2>$null
    Write-Host "[OK] Database created or already exists" -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Database might already exist" -ForegroundColor Yellow
}

# Run Maven to apply Flyway migrations
Write-Host ""
Write-Host "Running Maven to apply Flyway migrations..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

if (-not (Test-Path "pom.xml")) {
    Write-Host "[ERROR] pom.xml not found. Please run this script from the project root directory" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Building project and running migrations..." -ForegroundColor Yellow
& mvn clean install -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed. Please check the error messages above" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Database setup completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To start the application, run:" -ForegroundColor Yellow
Write-Host "  mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "Or access:" -ForegroundColor Yellow
Write-Host "  - Application: http://localhost:8080/api" -ForegroundColor White
Write-Host "  - Swagger UI: http://localhost:8080/api/swagger-ui.html" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to exit"
