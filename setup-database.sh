#!/bin/bash
# Metric System - Database Setup Script (Linux/Mac)
# This script initializes the PostgreSQL database using Flyway

echo "========================================"
echo "Metric System - Database Setup"
echo "========================================"
echo ""

# Check if PostgreSQL is installed
echo "Checking PostgreSQL connection..."

psql -U postgres -c "SELECT version();" > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "[ERROR] PostgreSQL is not running or not installed"
    echo "Please ensure PostgreSQL is installed and running"
    exit 1
fi

echo "[OK] PostgreSQL is running"

# Create database if not exists
echo ""
echo "Creating database 'metric_system' if it doesn't exist..."

psql -U postgres -c "CREATE DATABASE metric_system;" 2>/dev/null

if [ $? -ne 0 ]; then
    echo "[WARNING] Database might already exist or error occurred"
else
    echo "[OK] Database created successfully"
fi

# Run Maven to apply Flyway migrations
echo ""
echo "Running Maven to apply Flyway migrations..."
echo "========================================"

cd "$(dirname "$0")"

if [ ! -f "pom.xml" ]; then
    echo "[ERROR] pom.xml not found. Please run this script from the project root directory"
    exit 1
fi

echo "Building project and running migrations..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Build failed. Please check the error messages above"
    exit 1
fi

echo ""
echo "========================================"
echo "[SUCCESS] Database setup completed!"
echo "========================================"
echo ""
echo "To start the application, run:"
echo "  mvn spring-boot:run"
echo ""
echo "Or access:"
echo "  - Application: http://localhost:8080/api"
echo "  - Swagger UI: http://localhost:8080/api/swagger-ui.html"
echo ""
