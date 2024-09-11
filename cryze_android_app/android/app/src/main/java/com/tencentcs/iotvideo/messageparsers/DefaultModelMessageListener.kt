package com.tencentcs.iotvideo.messageparsers

import com.github.xerootg.cryze.httpclient.CryzeHttpClient
import com.tencentcs.iotvideo.messagemgr.IModelListener
import com.tencentcs.iotvideo.messagemgr.MessageType
import com.tencentcs.iotvideo.messagemgr.ModelMessage
import com.tencentcs.iotvideo.utils.LogUtils

// TODO: flush these metrics to the rest API to render nicely
class DefaultModelMessageListener(private val cameraId: String) : IModelListener {
    private val TAG = DefaultModelMessageListener::class.java.simpleName + "::$cameraId"

    override fun onNotify(modelMessage: ModelMessage?)
    {
        if (modelMessage == null) return

        if (modelMessage.device != cameraId) return

        // Send the message to the Cryze backend. This is a yeet and forget operation.
        CryzeHttpClient.postCameraModelMessage(cameraId, modelMessage)

        var logMessage = modelMessage.getPrettyMessage()

        try {
            when (modelMessage.type) {
                MessageType.MSG_TYPE_PRO_CONST -> {
                    // ProConst is the only type we have a class for right now
                    when (modelMessage.path) {
                        ProConstDeviceMessage.MessagePath -> logMessage =
                            ProConstDeviceMessage.fromModelMessage(modelMessage).toString()

                        else -> {}
                    }

                }

                MessageType.MSG_TYPE_PRO_READONLY -> {
                    // ProReadonly is the only type we have a class for right now
                    when (modelMessage.path) {
                        ProReadonlyModelMessage.MessagePath -> logMessage =
                            ProReadonlyModelMessage.fromModelMessage(modelMessage).toString()

                        ProReadonlyPowerModelMessage.MessagePath -> logMessage =
                            ProReadonlyPowerModelMessage.fromModelMessage(modelMessage).toString()

                        ProReadonlyNetInfoModelMessage.MessagePath -> logMessage =
                            ProReadonlyNetInfoModelMessage.fromModelMessage(modelMessage).toString()

                        else -> {}
                    }
                }

                MessageType.MSG_TYPE_PRO_WRITABLE -> {
                    // Action
                    when (modelMessage.path) {
                        ProWriteActionModelMessage.MessagePath -> logMessage =
                            ProWriteActionModelMessage.fromModelMessage(modelMessage).toString()

                        else -> {}
                    }
                    //
                }

                else -> {}
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error parsing ModelMessage message: $modelMessage", e)
        }

        LogUtils.i(TAG, logMessage)
    }
}