$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
Write-Host "[INFO] Compiling and running Order Domain Workflow Demo..." -ForegroundColor Cyan
& "C:\Users\jay ramani\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd" compile -pl order-adapters -am
& "C:\Users\jay ramani\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd" exec:java -pl order-adapters
