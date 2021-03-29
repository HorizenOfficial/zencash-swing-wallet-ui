$Password = ConvertTo-SecureString -String $Env:WIN_CERT_PASSWORD -AsPlainText -Force

Import-PfxCertificate -FilePath windows.p12 -CertStoreLocation Cert:\LocalMachine\My -Password $Password

Set-ItemProperty -Path $Env:APPLICATION_PATH -Name IsReadOnly -Value $false
scripts/signtool.exe sign /debug /v /f windows.p12 /t http://timestamp.digicert.com /du https://github.com/HorizenOfficial/zen /p $Env:WIN_CERT_PASSWORD /n "Zen Blockchain Foundation" $Env:APPLICATION_PATH
scripts/signtool.exe sign /debug /v /f windows.p12 /as /fd sha256 /tr http://timestamp.digicert.com /td sha256 /du https://github.com/HorizenOfficial/zen /p $Env:WIN_CERT_PASSWORD /n "Zen Blockchain Foundation" $Env:APPLICATION_PATH
