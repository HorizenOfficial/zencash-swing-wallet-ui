param
(
    [Parameter(Mandatory=$true, HelpMessage='The input file')]
    [string]$File,
    [Parameter(Mandatory=$true, HelpMessage='The Algorithm to use')]
    [string]$Algorithm
)
$hash = Get-FileHash $File -Algorithm $Algorithm
$fileHash = $hash.Hash.ToLower()

Write-Host -NoNewline  $fileHash"  "$File
