package com.tencentcs.iotvideo.messagemgr;

import android.text.TextUtils;
import com.tencentcs.iotvideo.IoTVideoErrors;
import com.tencentcs.iotvideo.utils.ByteUtils;
import com.tencentcs.iotvideo.utils.LogUtils;

/* loaded from: classes2.dex */
public class InnerUserDataHandler {
    private static final String TAG = "InnerUserDataHandler";
    private IFileDownloadInnerDataListener mFileDownloadInnerDataListener;
    private IMonitorInnerUserDataLister mMonitorInnerUserDataLister;
    private IPlaybackInnerUserDataLister mPlaybackInnerUserDataLister;

    private void onDownloadFileError(byte[] bArr) {
        int i10;
        LogUtils.i(TAG, "onDownloadFileError");
        if (bArr != null && bArr.length >= 12) {
            long bytesTolong = ByteUtils.bytesToLong(bArr, 0);
            int bytesToInt = ByteUtils.bytesToInt(bArr, 8);
            if (bytesToInt != 1) {
                if (bytesToInt != 2) {
                    if (bytesToInt != 3) {
                        if (bytesToInt != 4) {
                            if (bytesToInt != 5) {
                                i10 = IoTVideoErrors.DOWNLOAD_ERROR_UNKNOWN_ERROR;
                            } else {
                                i10 = IoTVideoErrors.DOWNLOAD_ERROR_PROCESS_EXITED;
                            }
                        } else {
                            i10 = IoTVideoErrors.DOWNLOAD_ERROR_FILE_NOT_FOUND;
                        }
                    } else {
                        i10 = IoTVideoErrors.DOWNLOAD_ERROR_FILE_OPEN_FAILED;
                    }
                } else {
                    i10 = IoTVideoErrors.DOWNLOAD_ERROR_INCORRECT_OFFSET;
                }
            } else {
                i10 = IoTVideoErrors.DOWNLOAD_ERROR_FILE_UNAVAILABLE;
            }
            LogUtils.i(TAG, "onDownloadFileError fileStartFile:" + bytesTolong + "; sdkErrorCode:" + i10);
            IFileDownloadInnerDataListener iFileDownloadInnerDataListener = this.mFileDownloadInnerDataListener;
            if (iFileDownloadInnerDataListener != null) {
                iFileDownloadInnerDataListener.onDownloadError(bytesTolong, i10);
            }
        }
    }

    private void parseDownloadFileInfo(byte[] bArr) {
        String str;
        if (bArr != null && bArr.length >= 18) {
            long bytesTolong = ByteUtils.bytesToLong(bArr, 0);
            int bytesToInt = ByteUtils.bytesToInt(bArr, 8);
            int bytesToInt2 = ByteUtils.bytesToInt(bArr, 12);
            short byte2ToShort = ByteUtils.byte2ToShort(bArr, 16);
            if (bArr.length < byte2ToShort + 18) {
                LogUtils.e(TAG, "parseDownloadFileInfo failure:data length < 18 + fileNameLen");
                return;
            }
            str = new String(bArr, 18, byte2ToShort);
            if (!TextUtils.isEmpty(str)) {
                DownloadFileHead downloadFileHead = new DownloadFileHead(bytesTolong, bytesToInt, bytesToInt2, str);
                LogUtils.i(TAG, "parseDownloadFileInfo ret:" + downloadFileHead.toString());
                IFileDownloadInnerDataListener iFileDownloadInnerDataListener = this.mFileDownloadInnerDataListener;
                if (iFileDownloadInnerDataListener != null) {
                    iFileDownloadInnerDataListener.onReceiveAvFileHead(downloadFileHead);
                    return;
                }
                return;
            }
            LogUtils.e(TAG, "parseDownloadFileInfo failure");
            return;
        }
        LogUtils.e(TAG, "parseDownloadFileInfo failure:data length < 18");
    }

    private static byte[] removeHeader(byte[] bArr) {
        if (bArr.length <= 8) {
            return null;
        }
        byte[] bArr2 = new byte[bArr.length - 8];
        System.arraycopy(bArr, 8, bArr2, 0, bArr.length - 8);
        return bArr2;
    }

    public void handleInnerUserData(byte[] bArr) {
        if (bArr == null || bArr.length < 8) {
            LogUtils.e(TAG, "data length is Unlawful");
            return;
        }

        if (bArr[0] != 0) {
            LogUtils.e(TAG, "is not inner user data");
            return;
        }

        LogUtils.i(TAG, "handleInnerUserData cmd:" + bArr[1]);
        byte b10 = bArr[1];

        switch (b10) {
            case 4:
                if (bArr.length < 16) {
                    LogUtils.e(TAG, "IVBuildInCmd_PlaybackStartTime data length is invalid");
                    return;
                }
                long startTime = ByteUtils.bytesToLong(bArr, 8);
                LogUtils.i(TAG, "handleInnerUserData file startTime:" + startTime);
                if (this.mPlaybackInnerUserDataLister != null) {
                    this.mPlaybackInnerUserDataLister.onPlayFileStart(startTime);
                }
                break;

            case 17:
                if (bArr.length < 16) {
                    LogUtils.e(TAG, "IVBuildInCmd_PlaybackFinished data length is invalid");
                    return;
                }
                long finishedTime = ByteUtils.bytesToLong(bArr, 8);
                LogUtils.i(TAG, "handleInnerUserData file finished:" + finishedTime);
                if (this.mPlaybackInnerUserDataLister != null) {
                    this.mPlaybackInnerUserDataLister.onPlayFileFinished(finishedTime);
                }
                break;

            case 19:
                parseDownloadFileInfo(removeHeader(bArr));
                break;

            case 22:
                onDownloadFileError(removeHeader(bArr));
                break;

            case 23:
                if (this.mFileDownloadInnerDataListener != null) {
                    this.mFileDownloadInnerDataListener.onReceiveAVFileData(removeHeader(bArr));
                }
                break;

            case 48:
                if (this.mMonitorInnerUserDataLister != null) {
                    this.mMonitorInnerUserDataLister.onViewerNumberChanged(bArr[8]);
                }
                break;

            case 49:
                if (this.mMonitorInnerUserDataLister != null) {
                    this.mMonitorInnerUserDataLister.onTalkerNumberChanged(bArr[8]);
                }
                break;

            default:
                LogUtils.e(TAG, "Unknown command: " + b10);
                break;
        }
    }


    public void setFileDownloadInnerDataListener(IFileDownloadInnerDataListener iFileDownloadInnerDataListener) {
        this.mFileDownloadInnerDataListener = iFileDownloadInnerDataListener;
    }

    public void setMonitorInnerUserDataLister(IMonitorInnerUserDataLister iMonitorInnerUserDataLister) {
        this.mMonitorInnerUserDataLister = iMonitorInnerUserDataLister;
    }

    public void setPlaybackInnerUserDataLister(IPlaybackInnerUserDataLister iPlaybackInnerUserDataLister) {
        this.mPlaybackInnerUserDataLister = iPlaybackInnerUserDataLister;
    }
}
