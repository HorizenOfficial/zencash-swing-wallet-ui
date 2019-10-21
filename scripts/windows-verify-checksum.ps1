param
(
    [Parameter(Mandatory=$true, HelpMessage='The file containing the hash.')]
    [string]$File,
    [Parameter(Mandatory=$true, HelpMessage='The Algorithm to use')]
    [string]$Algorithm
) 
foreach ($line in (Get-Content $File)) {
    $fields = $line -split '\s+'
    $hash = $fields[0].Trim().ToUpper()
    $filename = $fields[1].Trim()
    if($filename.StartsWith("*")){
        $filename = $filename.Substring(1).Trim()
    }

    $computedHash = (Get-FileHash -Algorithm $Algorithm $filename).Hash.ToUpper()

    if($hash.Equals($computedHash)){
        Write-Host $filename, ": Passed"
    }else{
        Write-Host $filename, ": Not Passed"
        Write-Host "Read from file: ", $hash
        Write-Host "Computed:       ", $computedHash
        exit 1
    }
}
