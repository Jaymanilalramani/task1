$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
Write-Host "[INFO] Running Maven tests for Task 2 (Order Domain System)..." -ForegroundColor Cyan
& "C:\Users\jay ramani\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd" clean test
