# Build Desktop Application

# Ensure Go is installed
if (-not (Get-Command go -ErrorAction SilentlyContinue)) {
    Write-Host "Go is not installed. Please install Go 1.21+ from https://golang.org/dl/"
    exit 1
}

# Navigate to desktop module
$desktopDir = Join-Path $PSScriptRoot "src\desktop"
if (-not (Test-Path $desktopDir)) {
    Write-Host "Desktop source directory not found."
    exit 1
}

Set-Location $desktopDir

# Tidy modules
Write-Host "Tidying Go modules..."
go mod tidy

# Build all
Write-Host "Building RD Desktop Application..."
go build ./cmd/rd-desktop

# Check for errors
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed."
    exit 1
}

Write-Host "Build successful. Executable is in the current directory."