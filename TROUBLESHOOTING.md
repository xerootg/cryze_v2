## Stalling at Startup

Sometimes, for unknown reasons (seems very deadlocky), the first camera stalls at 7 frames.
![a screenshot of the Android emulator frozen with the first camera displaying 7 frames processed and the other two at zero](https://github.com/user-attachments/assets/065abcd7-f69d-4fe2-b141-bd822eb7259a)

It seems consistent once it's started, restarting the `cryze_android_app` container does not seem to unwedge it. It seems to only happen on first boot. I can reliably get it back up by force-quitting the app. This is tricky from the UI, as the watchdog service (`ensure_running.sh`) will probably bring the activity back to the foreground before you can succeed at pressing enough buttons. 

From your adb-enabled device, run the following:
`adb shell am force-stop com.tencentcs.iotvideo`

## High CPU utilization

Make sure you're not falling back to software encoding:
![htop command showing high utilization of the CPU while decoding video](https://github.com/user-attachments/assets/805f3300-b77e-4c85-934f-fc4a3f883aa0)

Make sure your GPU is correctly mapped into redroid.

NOTE: Redroid seems to not be doing hardware encoding with Intel GPUs. I'm looking into getting a working libva/va-api OMX module setup, it seems like I'm consuming about 1 core per camera which is pretty crazy.

## Strange video proportions in stream clients

If your video is skewed or has strange colors, you're probably failing over to software encoding. Make sure your GPU is correctly mapped into redroid.
