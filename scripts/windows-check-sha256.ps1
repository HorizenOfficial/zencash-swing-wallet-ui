$file = Get-FileHash $Env:ZEND_FILE_NAME -Algorithm "SHA256"
$fileHash = $file.Hash.ToLower() + "  $Env:ZEND_FILE_NAME"
$hash = Get-Content -Path "$Env:ZEND_FILE_NAME.sha256"

if ($fileHash -like $hash) {
  Write-Output "sha256 matches!"
} else {
  Throw "sha256 does not match!"
}
