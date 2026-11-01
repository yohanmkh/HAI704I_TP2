#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Demo setup script for HAI704I TP2 SOAP Hotel/Agency demo
.DESCRIPTION
    This script starts the hotel services and runs the agency CLI for demonstration.
    Run each step in a separate terminal window for the full demo.
#>

# Build both modules
Write-Host "Building hotel module..." -ForegroundColor Green
cd hotel
mvn clean compile dependency:copy-dependencies -DskipTests
cd ..

Write-Host "Building agence module..." -ForegroundColor Green
cd agence
mvn clean compile dependency:copy-dependencies -DskipTests
cd ..

Write-Host "Build complete!" -ForegroundColor Green
Write-Host ""
Write-Host "To run the demo, open 3 terminal windows and run:" -ForegroundColor Yellow
Write-Host ""
Write-Host "TERMINAL 1 - Hotel 1 (Grand Hotel Montpellier):" -ForegroundColor Cyan
Write-Host "  cd hotel" -ForegroundColor Cyan
Write-Host '  java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18080 hotel1' -ForegroundColor Cyan
Write-Host ""
Write-Host "TERMINAL 2 - Hotel 2 (Hôtel de la Comédie):" -ForegroundColor Cyan
Write-Host "  cd hotel" -ForegroundColor Cyan
Write-Host '  java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18081 hotel2' -ForegroundColor Cyan
Write-Host ""
Write-Host "TERMINAL 3 - Agency CLI (interactive):" -ForegroundColor Cyan
Write-Host "  cd agence" -ForegroundColor Cyan
Write-Host '$env:HOTEL_URLS = "http://localhost:18080/hotel,http://localhost:18081/hotel"' -ForegroundColor Cyan
Write-Host '  java -cp "target/classes;target/dependency/*" agence.AgencyApp' -ForegroundColor Cyan
Write-Host ""
Write-Host "Demo scenarios to show:" -ForegroundColor Yellow
Write-Host "  1. Search for stays in Montpellier (shows offers from both hotels with different prices)" -ForegroundColor White
Write-Host "  2. Compare prices - Hotel 1 has 15% discount for AGENCY_VOYAGE_PRO, Hotel 2 has 5%" -ForegroundColor White
Write-Host "  3. Book an offer - enter traveler details and payment" -ForegroundColor White
Write-Host "  4. Show reservation reference" -ForegroundColor White
Write-Host "  5. Business rule refusal: try to book with wrong agency secret (modify agency.secret)" -ForegroundColor White
Write-Host "  6. Hotel down: stop Hotel 2, search again - agency shows warning" -ForegroundColor White
Write-Host "  7. Clean shutdown: quit agency (option 2), stop hotels (Ctrl+C)" -ForegroundColor White