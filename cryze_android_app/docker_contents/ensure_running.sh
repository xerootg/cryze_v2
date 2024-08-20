pm install --abi arm64-v8a -g --full /app/app.apk

while true; do
    if [ "`getprop breakloop`" -eq 1 ]; then
        echo 'breakloop property set, exiting loop';
        break;
    fi
    am start -n com.tencentcs.iotvideo/.MainActivity;
    sleep 30;
done;
