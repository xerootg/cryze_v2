pm install --abi arm64-v8a -g --full /app/app.apk

# Function to check if an interface has an IP address
wait_for_ip() {
    local iface=$1
    while ! ip addr show $iface | grep -q "inet "; do
        sleep 5
    done
}

# give the OS a second to settle, interfaces aren't named immediately
sleep 10
# Check if eth1 exists
if ip link show eth1 > /dev/null 2>&1; then
    
    # get an ip for eth1
    /system/bin/busybox udhcpc -i eth1 -s /system/etc/udhcpc.script
    # Wait for eth1 to have an IP address
    wait_for_ip eth1
else
    # Wait for eth0 to have an IP address
    wait_for_ip eth0
fi

while true; do
    if [ "`getprop breakloop`" -eq 1 ]; then
        echo 'breakloop property set, exiting loop';
        break;
    fi
    am start -n com.tencentcs.iotvideo/.MainActivity;
    sleep 10;
done;
