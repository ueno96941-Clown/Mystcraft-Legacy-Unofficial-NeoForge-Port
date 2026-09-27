$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tmp = Join-Path $env:TEMP ('mystcraft-neoforge-mdk-' + [Guid]::NewGuid().ToString('N'))
$zip = "$tmp.zip"
# Pin the MDK snapshot so wrapper behavior does not silently change later.
$mdkCommit = '30cafee9cd8d7f46427ec88fa8579d49c146df9a'
$url = "https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/archive/$mdkCommit.zip"

Write-Host "Downloading official NeoForge 1.21.1 ModDevGradle MDK at $mdkCommit..."
try {
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
} catch {
    Write-Error "Could not download the NeoForge MDK. Check DNS/firewall/proxy access to github.com. $($_.Exception.Message)"
    throw
}

Expand-Archive -Path $zip -DestinationPath $tmp -Force
$mdk = Get-ChildItem -Path $tmp -Directory | Select-Object -First 1
if (-not $mdk) { throw 'Could not locate extracted MDK directory.' }

New-Item -ItemType Directory -Path (Join-Path $root 'gradle\wrapper') -Force | Out-Null
Copy-Item (Join-Path $mdk.FullName 'gradlew') (Join-Path $root 'gradlew') -Force
Copy-Item (Join-Path $mdk.FullName 'gradlew.bat') (Join-Path $root 'gradlew.bat') -Force
Copy-Item (Join-Path $mdk.FullName 'gradle\wrapper\gradle-wrapper.jar') (Join-Path $root 'gradle\wrapper\gradle-wrapper.jar') -Force
Copy-Item (Join-Path $mdk.FullName 'gradle\wrapper\gradle-wrapper.properties') (Join-Path $root 'gradle\wrapper\gradle-wrapper.properties') -Force


# Verify the exact wrapper JAR imported from the pinned official NeoForge MDK snapshot.
# This checksum is for that MDK-carried wrapper JAR, not the Gradle distribution ZIP.
$expectedWrapperSha256 = '7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172'
$wrapperJar = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
$actualWrapperSha256 = (Get-FileHash -Algorithm SHA256 $wrapperJar).Hash.ToLowerInvariant()
if ($actualWrapperSha256 -ne $expectedWrapperSha256) {
    Remove-Item $wrapperJar -Force -ErrorAction SilentlyContinue
    throw "Unexpected Gradle wrapper JAR SHA-256: $actualWrapperSha256"
}

# Pin and checksum the Gradle 9.2.1 binary distribution as well.  Keeping this
# after the MDK copy prevents a future bootstrap from silently dropping the hash.
$wrapperProperties = Join-Path $root 'gradle\wrapper\gradle-wrapper.properties'
$propertiesText = Get-Content -Raw $wrapperProperties
$expectedDistributionUrl = 'distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip'
if ($propertiesText -notmatch [regex]::Escape($expectedDistributionUrl)) {
    throw 'Pinned NeoForge MDK no longer supplies the expected Gradle 9.2.1 distribution URL.'
}
$distributionShaLine = 'distributionSha256Sum=72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f'
if ($propertiesText -match '(?m)^distributionSha256Sum=.*$') {
    $propertiesText = [regex]::Replace($propertiesText, '(?m)^distributionSha256Sum=.*$', $distributionShaLine)
} else {
    $propertiesText = $propertiesText -replace '(?m)^(distributionUrl=.*)$', "`$1`r`n$distributionShaLine"
}
Set-Content -Path $wrapperProperties -Value $propertiesText -Encoding UTF8 -NoNewline

Remove-Item $zip -Force -ErrorAction SilentlyContinue
Remove-Item $tmp -Force -Recurse -ErrorAction SilentlyContinue
Write-Host 'Gradle wrapper installed from the pinned official NeoForge MDK.'
