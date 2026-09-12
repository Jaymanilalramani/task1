@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-22
echo [INFO] Running Order Domain Approval Workflow Demo...
"C:\Users\jay ramani\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd" compile -pl order-adapters -am
"C:\Users\jay ramani\.gemini\antigravity\scratch\apache-maven-3.9.6\bin\mvn.cmd" exec:java -pl order-adapters
pause
