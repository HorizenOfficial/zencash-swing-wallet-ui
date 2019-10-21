$file = Get-FileHash $Env:ZEND_FILE_NAME -Algorithm "SHA256"
$fileHash = $file.Hash.ToLower() + "  $Env:ZEND_FILE_NAME"

Write-Output $fileHash
