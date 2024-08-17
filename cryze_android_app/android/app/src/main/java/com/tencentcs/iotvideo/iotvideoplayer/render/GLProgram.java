package com.tencentcs.iotvideo.iotvideoplayer.render;

import android.content.Context;
import android.opengl.GLES20;
import android.text.TextUtils;
import com.tencentcs.iotvideo.utils.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes2.dex */
public class GLProgram {
    private static final String ASSET_PATH_FRAGMENT_SHADER_FILE;
    private static final String ASSET_PATH_VERTEX_SHADER_FILE;
    private static final String FRAGMENT_SHADER = "precision mediump float;\nuniform sampler2D tex_y;\nuniform sampler2D tex_u;\nuniform sampler2D tex_v;\nvarying vec2 v_color;\nvoid main() {\nvec4 c = vec4((texture2D(tex_y, v_color).r - 16./255.) * 1.164);\nvec4 U = vec4(texture2D(tex_u, v_color).r - 128./255.);\nvec4 V = vec4(texture2D(tex_v, v_color).r - 128./255.);\nc += V * vec4(1.596, -0.813, 0, 0);\nc += U * vec4(0, -0.392, 2.017, 0);\nc.a = 1.0;\ngl_FragColor = c;\n}\n";
    private static final String VERTEX_SHADER = "attribute vec4 a_position;\nattribute vec2 a_texCoord;\nvarying vec2 v_color;\nuniform mat4 u_mvp;\nvoid main() {\ngl_Position = u_mvp * a_position;\nv_color = a_texCoord;\n}\n";
    private ByteBuffer _coord_buffer;
    private int _distortionFactor;
    private int _program;
    private ByteBuffer _vertice_buffer;
    private float[] _vertices;
    private boolean mIsFisheye;
    public final float[] squareVertices;
    private final String TAG = "GLProgram";
    private final float[] coordVertices = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private final float[] mvpMatrix = {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private int _mvpHandle = -1;
    private int _positionHandle = -1;
    private int _coordHandle = -1;
    private int _yhandle = -1;
    private int _uhandle = -1;
    private int _vhandle = -1;
    private int _ytid = -1;
    private int _utid = -1;
    private int _vtid = -1;
    private int _closeUpRect = -1;
    private int _video_width = -1;
    private int _video_height = -1;
    private float[] closeUpRect = {0.0f, 0.0f, 1.0f, 1.0f};
    private int _textureI = 33984;
    private int _textureII = 33985;
    private int _textureIII = 33986;
    private int _tIindex = 0;
    private int _tIIindex = 1;
    private int _tIIIindex = 2;

    static {
        String asset_vertex_file = "glsl" + File.separator + "IoTVideoVertexShader.glsl";
        ASSET_PATH_VERTEX_SHADER_FILE = asset_vertex_file;
        String asset_fragment_file = "glsl" + File.separator + "IoTVideoFragmentShader.glsl";
        ASSET_PATH_FRAGMENT_SHADER_FILE = asset_fragment_file;
    }

    public GLProgram() {
        float[] fArr = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
        this.squareVertices = fArr;
        this._vertices = fArr;
        initBuffers();
    }

    private int checkGlError(String str) {
        int i10;
        try {
            i10 = GLES20.glGetError();
            if (i10 != 0) {
                try {
                    LogUtils.e("GLProgram", " " + str + ": glError " + i10);
                } catch (Exception e10) {
                    LogUtils.e("GLProgram", "carsh e" + e10.getMessage());
                    e10.printStackTrace();
                    return i10;
                }
            }
        } catch (Exception e11) {
            e11.printStackTrace();
            i10 = 0;
        }
        return i10;
    }

    private void initBuffers() {
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(this.squareVertices.length * 4);
        this._vertice_buffer = allocateDirect;
        allocateDirect.order(ByteOrder.nativeOrder());
        this._vertice_buffer.asFloatBuffer().put(this.squareVertices);
        this._vertice_buffer.position(0);
        ByteBuffer allocateDirect2 = ByteBuffer.allocateDirect(this.coordVertices.length * 4);
        this._coord_buffer = allocateDirect2;
        allocateDirect2.order(ByteOrder.nativeOrder());
        this._coord_buffer.asFloatBuffer().put(this.coordVertices);
        this._coord_buffer.position(0);
    }

    private String loadGLFileContent(Context context, String str) {
        if (context == null) {
            LogUtils.e("GLProgram", "loadGLFileContent failure:context is null");
            return null;
        } else if (TextUtils.isEmpty(str)) {
            LogUtils.e("GLProgram", "loadGLFileContent failure:glFilePath is null");
            return null;
        } else {
            StringBuffer stringBuffer = new StringBuffer();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open(str)));
                while (true) {
                    String readLine = bufferedReader.readLine();
                    if (readLine == null) {
                        break;
                    }
                    stringBuffer.append(readLine);
                    stringBuffer.append("\n");
                }
            } catch (IOException e10) {
                LogUtils.e("GLProgram", "loadGLFileContent exception:" + e10.getMessage());
            }
            return stringBuffer.toString();
        }
    }

    private int loadShader(int i10, String str) {
        int glCreateShader = GLES20.glCreateShader(i10);
        if (glCreateShader != 0) {
            GLES20.glShaderSource(glCreateShader, str);
            GLES20.glCompileShader(glCreateShader);
            int[] iArr = new int[1];
            GLES20.glGetShaderiv(glCreateShader, 35713, iArr, 0);
            if (iArr[0] == 0) {
                LogUtils.e("GLProgram", "Could not compile shader " + i10);
                LogUtils.e("GLProgram", "" + GLES20.glGetShaderInfoLog(glCreateShader));
                GLES20.glDeleteShader(glCreateShader);
                return 0;
            }
            return glCreateShader;
        }
        return glCreateShader;
    }

    public void buildProgram() {
        buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public int buildTextures(Buffer buffer, Buffer buffer2, Buffer buffer3, int i10, int i11) {
        boolean z10;
        if (i10 == this._video_width && i11 == this._video_height) {
            z10 = false;
        } else {
            z10 = true;
        }
        if (z10) {
            this._video_width = i10;
            this._video_height = i11;
            LogUtils.d("GLProgram", "buildTextures videoSizeChanged: w=" + this._video_width + " h=" + this._video_height);
        }
        int i12 = this._ytid;
        if (i12 < 0 || z10) {
            if (i12 >= 0) {
                LogUtils.d("GLProgram", "glDeleteTextures Y");
                GLES20.glDeleteTextures(1, new int[]{this._ytid}, 0);
                if (checkGlError("glDeleteTextures") != 0) {
                    return -1;
                }
            }
            int[] iArr = new int[1];
            GLES20.glGenTextures(1, iArr, 0);
            if (checkGlError("glGenTextures") != 0) {
                return -1;
            }
            this._ytid = iArr[0];
            LogUtils.d("GLProgram", "glGenTextures Y = " + this._ytid);
        }
        GLES20.glBindTexture(3553, this._ytid);
        if (checkGlError("glBindTexture") != 0) {
            return -1;
        }
        GLES20.glTexImage2D(3553, 0, 6409, this._video_width, this._video_height, 0, 6409, 5121, buffer);
        int checkGlError = checkGlError("glTexImage2D");
        if (checkGlError != 0) {
            return -1;
        }
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        int i13 = this._utid;
        if (i13 < 0 || z10) {
            if (i13 >= 0) {
                LogUtils.d("GLProgram", "glDeleteTextures U");
                GLES20.glDeleteTextures(1, new int[]{this._utid}, 0);
                if (checkGlError("glDeleteTextures") != 0) {
                    return -1;
                }
            }
            int[] iArr2 = new int[1];
            GLES20.glGenTextures(1, iArr2, 0);
            int checkGlError2 = checkGlError("glGenTextures");
            if (checkGlError2 != 0) {
                return -1;
            }
            this._utid = iArr2[0];
            LogUtils.d("GLProgram", "glGenTextures U = " + this._utid);
            checkGlError = checkGlError2;
        }
        GLES20.glBindTexture(3553, this._utid);
        GLES20.glTexImage2D(3553, 0, 6409, this._video_width / 2, this._video_height / 2, 0, 6409, 5121, buffer2);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        int i14 = this._vtid;
        if (i14 < 0 || z10) {
            if (i14 >= 0) {
                LogUtils.d("GLProgram", "glDeleteTextures V");
                GLES20.glDeleteTextures(1, new int[]{this._vtid}, 0);
                if (checkGlError("glDeleteTextures") != 0) {
                    return -1;
                }
            }
            int[] iArr3 = new int[1];
            GLES20.glGenTextures(1, iArr3, 0);
            int checkGlError3 = checkGlError("glGenTextures");
            this._vtid = iArr3[0];
            LogUtils.d("GLProgram", "glGenTextures V = " + this._vtid);
            checkGlError = checkGlError3;
        }
        GLES20.glBindTexture(3553, this._vtid);
        GLES20.glTexImage2D(3553, 0, 6409, this._video_width / 2, this._video_height / 2, 0, 6409, 5121, buffer3);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        return checkGlError;
    }

    public int createProgram(String str, String str2) {
        int loadShader = loadShader(35633, str);
        int loadShader2 = loadShader(35632, str2);
        LogUtils.d("GLProgram", "vertexShader = " + loadShader);
        LogUtils.d("GLProgram", "pixelShader = " + loadShader2);
        int glCreateProgram = GLES20.glCreateProgram();
        if (glCreateProgram != 0) {
            GLES20.glAttachShader(glCreateProgram, loadShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(glCreateProgram, loadShader2);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(glCreateProgram);
            int[] iArr = new int[1];
            GLES20.glGetProgramiv(glCreateProgram, 35714, iArr, 0);
            if (iArr[0] != 1) {
                LogUtils.e("GLProgram", "Could not link program");
                LogUtils.e("GLProgram", GLES20.glGetProgramInfoLog(glCreateProgram));
                GLES20.glDeleteProgram(glCreateProgram);
                return 0;
            }
            return glCreateProgram;
        }
        return glCreateProgram;
    }

    public void drawFrame() {
        float f10;
        GLES20.glUseProgram(this._program);
        checkGlError("glUseProgram");
        GLES20.glVertexAttribPointer(this._positionHandle, 2, 5126, false, 8, (Buffer) this._vertice_buffer);
        checkGlError("glVertexAttribPointer mPositionHandle");
        GLES20.glEnableVertexAttribArray(this._positionHandle);
        GLES20.glVertexAttribPointer(this._coordHandle, 2, 5126, false, 8, (Buffer) this._coord_buffer);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(this._coordHandle);
        GLES20.glUniformMatrix4fv(this._mvpHandle, 1, false, this.mvpMatrix, 0);
        GLES20.glActiveTexture(this._textureI);
        GLES20.glBindTexture(3553, this._ytid);
        GLES20.glUniform1i(this._yhandle, this._tIindex);
        GLES20.glActiveTexture(this._textureII);
        GLES20.glBindTexture(3553, this._utid);
        GLES20.glUniform1i(this._uhandle, this._tIIindex);
        GLES20.glActiveTexture(this._textureIII);
        GLES20.glBindTexture(3553, this._vtid);
        GLES20.glUniform1i(this._vhandle, this._tIIIindex);
        int i10 = this._closeUpRect;
        if (-1 != i10) {
            float[] fArr = this.closeUpRect;
            GLES20.glUniform4f(i10, fArr[0], fArr[1], fArr[2], fArr[3]);
        }
        int i11 = this._distortionFactor;
        if (this.mIsFisheye) {
            f10 = -0.7071f;
        } else {
            f10 = 0.0f;
        }
        GLES20.glUniform1f(i11, f10);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(this._positionHandle);
        GLES20.glDisableVertexAttribArray(this._coordHandle);
    }

    public void release() {
        if (this._ytid >= 0) {
            LogUtils.d("GLProgram", "glDeleteTextures Y");
            GLES20.glDeleteTextures(1, new int[]{this._ytid}, 0);
            checkGlError("glDeleteTextures");
        }
        if (this._utid >= 0) {
            LogUtils.d("GLProgram", "glDeleteTextures u");
            GLES20.glDeleteTextures(1, new int[]{this._utid}, 0);
            checkGlError("glDeleteTextures");
        }
        if (this._vtid >= 0) {
            LogUtils.d("GLProgram", "glDeleteTextures v");
            GLES20.glDeleteTextures(1, new int[]{this._vtid}, 0);
            checkGlError("glDeleteTextures");
        }
    }

    public void setCloseUpRect(float[] fArr) {
        for (int i10 = 0; i10 < fArr.length; i10++) {
            float[] fArr2 = this.closeUpRect;
            if (i10 < fArr2.length) {
                fArr2[i10] = fArr[i10];
            } else {
                return;
            }
        }
    }

    public void setFisheye(boolean z10) {
        this.mIsFisheye = z10;
    }

    public void updateMvp(float[] fArr) {
        int length = this.mvpMatrix.length;
        for (int i10 = 0; i10 < length; i10++) {
            this.mvpMatrix[i10] = fArr[i10];
        }
    }

    public void buildProgram(Context context) {
        String loadGLFileContent = loadGLFileContent(context, ASSET_PATH_VERTEX_SHADER_FILE);
        String loadGLFileContent2 = loadGLFileContent(context, ASSET_PATH_FRAGMENT_SHADER_FILE);
        LogUtils.d("GLProgram", "vertexShaderContent:\n" + loadGLFileContent);
        LogUtils.d("GLProgram", "fragmentShaderContent:\n" + loadGLFileContent2);
        if (!TextUtils.isEmpty(loadGLFileContent) && !TextUtils.isEmpty(loadGLFileContent2)) {
            buildProgram(loadGLFileContent, loadGLFileContent2);
        } else {
            buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        }
    }

    private void buildProgram(String str, String str2) {
        this._program = createProgram(str, str2);
        LogUtils.d("GLProgram", "_program = " + this._program);
        this._mvpHandle = GLES20.glGetUniformLocation(this._program, "u_mvp");
        LogUtils.d("GLProgram", "_mvpHandle = " + this._mvpHandle);
        checkGlError("glGetUniformLocation u_mvp");
        if (this._mvpHandle != -1) {
            this._positionHandle = GLES20.glGetAttribLocation(this._program, "a_position");
            LogUtils.d("GLProgram", "_positionHandle = " + this._positionHandle);
            checkGlError("glGetAttribLocation a_position");
            if (this._positionHandle != -1) {
                this._coordHandle = GLES20.glGetAttribLocation(this._program, "a_texCoord");
                LogUtils.d("GLProgram", "_coordHandle = " + this._coordHandle);
                checkGlError("glGetAttribLocation a_texCoord");
                if (this._coordHandle != -1) {
                    this._yhandle = GLES20.glGetUniformLocation(this._program, "tex_y");
                    LogUtils.d("GLProgram", "_yhandle = " + this._yhandle);
                    checkGlError("glGetUniformLocation tex_y");
                    if (this._yhandle != -1) {
                        this._uhandle = GLES20.glGetUniformLocation(this._program, "tex_u");
                        LogUtils.d("GLProgram", "_uhandle = " + this._uhandle);
                        checkGlError("glGetUniformLocation tex_u");
                        if (this._uhandle != -1) {
                            this._vhandle = GLES20.glGetUniformLocation(this._program, "tex_v");
                            LogUtils.d("GLProgram", "_vhandle = " + this._vhandle);
                            checkGlError("glGetUniformLocation tex_v");
                            if (this._vhandle != -1) {
                                this._closeUpRect = GLES20.glGetUniformLocation(this._program, "closeupRect");
                                LogUtils.d("GLProgram", "_closeUpRect = " + this._closeUpRect);
                                checkGlError("glGetUniformLocation closeupRect");
                                if (this._closeUpRect != -1) {
                                    this._distortionFactor = GLES20.glGetUniformLocation(this._program, "distortionFactor");
                                    LogUtils.d("GLProgram", "_distortionFactor = " + this._distortionFactor);
                                    checkGlError("glGetUniformLocation distortionFactor");
                                    if (this._distortionFactor == -1) {
                                        throw new RuntimeException("Could not get uniform location for distortionFactor");
                                    }
                                    return;
                                }
                                throw new RuntimeException("Could not get uniform location for closeupRect");
                            }
                            throw new RuntimeException("Could not get uniform location for tex_v");
                        }
                        throw new RuntimeException("Could not get uniform location for tex_u");
                    }
                    throw new RuntimeException("Could not get uniform location for tex_y");
                }
                throw new RuntimeException("Could not get attribute location for a_texCoord");
            }
            throw new RuntimeException("Could not get attribute location for a_position");
        }
        throw new RuntimeException("Could not get uniform location for u_mvp");
    }
}
