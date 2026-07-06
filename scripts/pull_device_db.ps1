# Pull rootie-db from connected Android device (includes WAL files for SQLite Studio).
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb"
    exit 1
}

$outDir = Join-Path $PSScriptRoot "..\db-export"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = @("rootie-db", "rootie-db-wal", "rootie-db-shm")
foreach ($name in $files) {
    $dest = Join-Path $outDir $name
    & $adb exec-out "run-as com.veganbeauty.app cat databases/$name" |
        python -c "import sys; open(r'$dest','wb').write(sys.stdin.buffer.read())"
    $size = (Get-Item $dest -ErrorAction SilentlyContinue).Length
    Write-Output "$name -> $dest ($size bytes)"
}

Write-Output ""
Write-Output "Open db-export/rootie-db in SQLite Studio (keep all 3 files in same folder)."
