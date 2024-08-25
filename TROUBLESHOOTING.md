## High CPU utilization

Make sure you're not falling back to software encoding:
![htop command showing high utilization of the CPU while decoding video](https://github.com/user-attachments/assets/805f3300-b77e-4c85-934f-fc4a3f883aa0)

NOTE: Redroid seems to not be doing hardware encoding with Intel GPUs. I'm looking into getting a working libva/va-api OMX module setup, it seems like I'm consuming about 1 core per camera which is pretty crazy.

## Strange video proportions in stream clients when using RTSP mode

If your video is skewed or has strange colors, you're probably failing over to software encoding. Make sure your GPU is correctly mapped into redroid.
