$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force out | Out-Null
$javaFiles = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $javaFiles
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }
jar --create --file minibank.jar --main-class service.MiniBank -C out .
Write-Host "Created minibank.jar"
Write-Host "Run with: java -jar minibank.jar"
