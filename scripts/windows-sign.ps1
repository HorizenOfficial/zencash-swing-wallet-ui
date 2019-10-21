$Password = ConvertTo-SecureString -String $Env:WIN_CERT_PASSWORD -AsPlainText -Force

Import-PfxCertificate -FilePath C:\Users\travis\build\ZencashOfficial\zencash-swing-wallet-ui\windows.p12 -CertStoreLocation Cert:\LocalMachine\My -Password $Password

$AbsoluteExePath = "C:\Users\travis\build\ZencashOfficial\zencash-swing-wallet-ui\$Env:APPLICATION_PATH"
$CertSubjectName = "Zen Blockchain Foundation"

& "C:\Program Files (x86)\Windows Kits\10\bin\x64\signtool.exe" sign /v /debug /f C:\Users\travis\build\ZencashOfficial\zencash-swing-wallet-ui\windows.p12 /p $Env:WIN_CERT_PASSWORD /du https://github.com/ZencashOfficial/zen /t http://timestamp.digicert.com/ /n `"$CertSubjectName`" `"$AbsoluteExePath`"
