$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force out | Out-Null
$javaFiles = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $javaFiles
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }
$jarTemp = Join-Path $PSScriptRoot 'tmp\jar'
New-Item -ItemType Directory -Force $jarTemp | Out-Null
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = ((@($previousJavaToolOptions, "-Djava.io.tmpdir=$jarTemp") | Where-Object { $_ }) -join ' ')
    jar --create --file minibank.jar --main-class service.MiniBank -C out .
    if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed." }
} finally {
    if ($null -eq $previousJavaToolOptions) { Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue }
    else { $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions }
}
Write-Host "Created minibank.jar"
Write-Host "Run with: java -jar minibank.jar"
