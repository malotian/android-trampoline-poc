# PowerShell script to configure App Links for the Trampoline POC
# Use this to force the system to open staples.com URLs in this app.

$adbPath = "C:\Users\malot\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$packageName = "com.staples.trampolinepoc"
$domains = "staples.com www.staples.com"

if (-not (Test-Path $adbPath)) {
    Write-Error "ADB not found at $adbPath. Please update the `$adbPath` variable in this script."
    exit 1
}

Write-Host "Searching for devices..." -NoNewline

# Get list of device serials
$serials = & $adbPath devices | Select-String -Pattern "\s+device$" | ForEach-Object { $_.ToString().Split("`t", 2)[0].Trim() }

if (-not $serials) {
    Write-Host "`n"
    Write-Error "No Android devices or emulators found."
    exit 1
}

$devices = @()
foreach ($serial in $serials) {
    # 1. Try to get AVD name (common for emulators like "Pixel_7")
    $avdName = (& $adbPath -s $serial shell getprop ro.boot.qemu.avd_name).Trim()

    # 2. Get standard product properties
    $manufacturer = (& $adbPath -s $serial shell getprop ro.product.manufacturer).Trim()
    $model = (& $adbPath -s $serial shell getprop ro.product.model).Trim()

    # Logic to build a friendly name
    $name = ""
    if ($avdName) {
        $name = $avdName.Replace("_", " ")
    } elseif ($manufacturer -and $model) {
        # Capitalize manufacturer (e.g., samsung -> Samsung)
        $manufacturer = (Get-Culture).TextInfo.ToTitleCase($manufacturer.ToLower())
        $name = "$manufacturer $model"
    } else {
        $name = $serial
    }

    $devices += [PSCustomObject]@{
        Serial = $serial
        DisplayName = "$name ($serial)"
    }
}
Write-Host " Done.`n"

$targetSerial = ""

if ($devices.Count -eq 1) {
    $targetSerial = $devices[0].Serial
    Write-Host "Found one device: $($devices[0].DisplayName)"
} else {
    Write-Host "Multiple devices found. Please select one:"
    for ($i = 0; $i -lt $devices.Count; $i++) {
        Write-Host "[$i] $($devices[$i].DisplayName)"
    }

    $choice = Read-Host "`nEnter the number of the target device"
    if ($choice -as [int] -ne $null -and [int]$choice -lt $devices.Count -and $choice -match "^\d+$") {
        $targetSerial = $devices[[int]$choice].Serial
    } else {
        Write-Error "Invalid selection."
        exit 1
    }
}

Write-Host "`nTargeting device: $targetSerial"
Write-Host "Setting App Links user selection for $packageName..."

& $adbPath -s $targetSerial shell pm set-app-links-user-selection --user 0 --package $packageName true $domains

if ($LASTEXITCODE -eq 0) {
    Write-Host "Successfully enabled App Links for $domains" -ForegroundColor Green
} else {
    Write-Host "Failed to set App Links. Exit code: $LASTEXITCODE" -ForegroundColor Red
    Write-Host "Ensure the app is installed on the selected device."
}
