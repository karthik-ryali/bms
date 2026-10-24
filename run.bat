@echo off
echo =========================================================================
echo Starting BMS - Smart Wallet Management (single port)...
echo UI + REST API : http://localhost:3000
echo Swagger UI    : http://localhost:3000/swagger-ui.html
echo OpenAPI spec  : http://localhost:3000/v3/api-docs
echo Database      : MySQL (bms)
echo Default login : karthik / password123
echo =========================================================================

call "%~dp0mvnw.cmd" spring-boot:run
pause
