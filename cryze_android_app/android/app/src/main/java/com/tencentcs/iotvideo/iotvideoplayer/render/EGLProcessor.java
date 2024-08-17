package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.view.Surface;
import com.tencentcs.iotvideo.utils.LogUtils;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
/* loaded from: classes2.dex */
public class EGLProcessor {
    private static final String TAG = "EGLProcessor";
    private EGL10 egl;
    private Surface mSurface;
    private EGLDisplay display = EGL10.EGL_NO_DISPLAY;
    private EGLSurface eglSurface = EGL10.EGL_NO_SURFACE;
    private EGLContext context = EGL10.EGL_NO_CONTEXT;

    public EGLProcessor(Surface surface) {
        this.mSurface = surface;
    }

    public void destroyEGL() {
        EGLContext eGLContext = EGL10.EGL_NO_CONTEXT;
        if (eGLContext == this.context) {
            LogUtils.e(TAG, "destroyEGL error, has destroyed egl");
            return;
        }
        EGL10 egl10 = this.egl;
        EGLDisplay eGLDisplay = this.display;
        EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
        egl10.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, eGLContext);
        this.egl.eglDestroySurface(this.display, this.eglSurface);
        this.egl.eglDestroyContext(this.display, this.context);
        this.egl.eglTerminate(this.display);
        this.display = EGL10.EGL_NO_DISPLAY;
        this.eglSurface = EGL10.EGL_NO_SURFACE;
        this.context = EGL10.EGL_NO_CONTEXT;
        LogUtils.i(TAG, "destroyEGL");
    }

    public void prepareEGL() {
        EGL10 egl10 = (EGL10) EGLContext.getEGL();
        this.egl = egl10;
        EGLDisplay eglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        this.display = eglGetDisplay;
        this.egl.eglInitialize(eglGetDisplay, null);
        EGLConfig[] eGLConfigArr = new EGLConfig[1];
        this.egl.eglChooseConfig(this.display, new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, 12344, 0, 12344}, eGLConfigArr, 1, new int[1]);
        EGLConfig eGLConfig = eGLConfigArr[0];
        this.eglSurface = this.egl.eglCreateWindowSurface(this.display, eGLConfig, this.mSurface, new int[]{12344});
        EGLContext eglCreateContext = this.egl.eglCreateContext(this.display, eGLConfig, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
        this.context = eglCreateContext;
        EGL10 egl102 = this.egl;
        EGLDisplay eGLDisplay = this.display;
        EGLSurface eGLSurface = this.eglSurface;
        egl102.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, eglCreateContext);
        LogUtils.i(TAG, "prepareEGL successful");
    }

    public void swapBuffers() {
        this.egl.eglSwapBuffers(this.display, this.eglSurface);
    }
}
