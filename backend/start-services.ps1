# Load .env file into current environment
Get-Content .\.env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.+)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "  Set: $name"
    }
}
Write-Host "`nEnvironment loaded. Starting $args[0]..."

# Pass through to Maven
& .\mvnw.cmd spring-boot:run -pl $args[0]
