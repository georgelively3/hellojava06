# Clean Test Execution Script
# This script ensures a clean environment for running tests

Write-Host "🧹 Cleaning up previous processes..." -ForegroundColor Yellow

# Stop Gradle daemons
Write-Host "Stopping Gradle daemons..."
./gradlew --stop

# Kill any lingering Java processes
Write-Host "Stopping Java processes..."
Get-Process | Where-Object {$_.ProcessName -eq "java"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Wait a moment for cleanup
Start-Sleep -Seconds 2

# Check ports
Write-Host "Checking port availability..."
$port8080 = netstat -ano | findstr :8080
$port9999 = netstat -ano | findstr :9999

if ($port8080) {
    Write-Host "⚠️  Port 8080 still in use. Stopping process..." -ForegroundColor Red
    $processId = ($port8080 -split '\s+')[-1]
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

if ($port9999) {
    Write-Host "⚠️  Port 9999 still in use. Stopping process..." -ForegroundColor Red  
    $processId = ($port9999 -split '\s+')[-1]
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

Write-Host "✅ Environment cleaned!" -ForegroundColor Green
Write-Host ""

# Run tests in sequence
Write-Host "🧪 Running Test Suite..." -ForegroundColor Cyan
Write-Host "==============================================="

Write-Host ""
Write-Host "1️⃣  Running Unit Tests..." -ForegroundColor Green
./gradlew test --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Unit tests failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "2️⃣  Running WireMock Integration Tests..." -ForegroundColor Green  
./gradlew wireMockIntegrationTest --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ WireMock integration tests failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "3️⃣  Compiling and checking for errors..." -ForegroundColor Green
./gradlew compileJava compileTestJava --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "🎉 All tests passed successfully!" -ForegroundColor Green
Write-Host "==============================================="

# Final cleanup
Write-Host ""
Write-Host "🧹 Final cleanup..." -ForegroundColor Yellow
./gradlew --stop

Write-Host "✅ Test suite completed!" -ForegroundColor Green
