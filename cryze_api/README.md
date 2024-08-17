.env needs to be loaded or otherwise the variables therein.
for windows, check out this powershell:
```ps
Get-Content ".env" | ForEach-Object {
    $line = $_.Split('#')[0].Trim()
    if ($line) {
        $name, $value = $line.Split('=')
        Set-Item -Path "Env:\$name" -Value $value
    }
}
```

TODO:
- add a RTSP proxy. the one in the android app is very flakey and doesn't support the full RTSP protocol
- rewrite the python into c#, or figure out how to use the wyze_api python library