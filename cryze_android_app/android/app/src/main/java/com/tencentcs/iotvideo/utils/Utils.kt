package com.tencentcs.iotvideo.utils

import android.annotation.SuppressLint
import android.os.Build
import com.tencentcs.iotvideo.IoTVideoError
import java.text.SimpleDateFormat
import java.util.Date

object Utils {
    @SuppressLint("SimpleDateFormat")
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    @SuppressLint("SimpleDateFormat")
    private val dayFormatter = SimpleDateFormat("yyyy-MM-dd")

    fun getDelPlaybackErrMsg(errorCode: Int): String {

        return when (IoTVideoError.fromValue(errorCode)) {
            IoTVideoError.DEL_FILE_STROAGE_EXCEPT -> "Storage exception: index file does not exist (cannot be opened), video file cannot be deleted, possible reasons: storage device is unplugged, no access rights, etc."
            IoTVideoError.DEL_FILE_RESOURCES_LACK -> "The storage device is unplugged, no access rights, etc."
            IoTVideoError.DEL_FILE_DEFAULT_CODE -> "Delete failed: delete exception, execute remove"
            IoTVideoError.DEL_FILE_INFO_NOT_EXIST -> "Deletion failed: the file requested to be deleted is not in the record list -> cannot be deleted"
            IoTVideoError.DEL_RESOURCES_BUSY -> "The resource is busy and the operation cannot be executed: the delete command cannot be executed while the resource is being played back, downloaded, or deleted"
            IoTVideoError.DEL_APP_CANCEL -> "The user-side APP actively canceled this deletion"
            else -> IoTVideoError.fromValue(errorCode).name
        }
    }

    fun getErrorDescription(errorCode: Int): String {
        return when (IoTVideoError.fromValue(errorCode)) {
            IoTVideoError.ASrv_dst_offline -> "Target offline"
            IoTVideoError.ASrv_dst_notfound_asrv -> "Access server for target not found"
            IoTVideoError.ASrv_dst_notexsit -> "Target does not exist"
            IoTVideoError.ASrv_dst_error_relation -> "Illegal relationship chain"
            IoTVideoError.ASrv_data_chkfrm_fail -> "Frame verification failed"
            IoTVideoError.ASrv_data_loadjson_fail -> "Failed to load JSON from terminal to object model"
            IoTVideoError.ASrv_data_modifytick_fail -> "Failed to modify timestamp in object model from terminal JSON"
            IoTVideoError.ASrv_tocsrv_timeout -> "Communication timeout between access server and central server"
            IoTVideoError.ASrv_url_parse_fail -> "URL parsing failed"
            IoTVideoError.ASrv_csrv_reply_err -> "Central server responded with erroneous data"
            IoTVideoError.ASrv_forward_toASrv_timeout -> "Timeout forwarding message from access server to another access server"
            IoTVideoError.ASrv_forward_toASrv_fail -> "Failed to forward message from access server to another access server"
            IoTVideoError.ASrv_forward_toTerm_timeout -> "Timeout forwarding message from access server to device"
            IoTVideoError.ASrv_forward_toTerm_fail, IoTVideoError.ASrv_handle_fail -> "Failed to forward message from access server to device"
            IoTVideoError.ASrv_dstid_parse_faild -> "Access server failed to parse target ID from data frame"
            IoTVideoError.ASrv_dstid_isuser -> "Access server found target ID is a user"
            IoTVideoError.ASrv_calc_leaf_fail -> "Access server failed to calculate leaf"
            IoTVideoError.ASrv_set_timeval_leafval_fail -> "Access server failed to set timeval value in object model"
            IoTVideoError.ASrv_calc_forward_json_fail -> "Access server failed to calculate forwarding JSON"
            IoTVideoError.ASrv_tmpsubs_parse_fail -> "Temporary subscription frame failed to parse device ID"
            IoTVideoError.ASrv_csrvctrl_trgtype_error -> "Central server sent ctl frame with incorrect trgtype"
            IoTVideoError.ASrv_binderror_dev_usr_has_bind -> "You have already bound this device"
            IoTVideoError.ASrv_binderror_dev_has_bind_other, IoTVideoError.ASrv_unformat_jsstr_fail -> "Device is already bound to another user"
            IoTVideoError.ASrv_binderror_customer_diffrent -> "Device customer ID does not match user customer ID"
            IoTVideoError.ASrv_netcfg_maketoken_fail -> "Failed to generate token during network configuration"
            IoTVideoError.ASrv_netcfg_verifytoken_fail -> "Failed to verify token during network configuration"
            IoTVideoError.ASrv_parse_json_fail -> "JSON parsing error"
            IoTVideoError.Term_msg_send_peer_timeout -> "Message send to peer timed out"
            IoTVideoError.Term_msg_calling_hangup -> "Normal hangup message"
            IoTVideoError.Term_msg_calling_send_timeout -> "Calling message send timed out"
            IoTVideoError.Term_msg_calling_no_srv_addr -> "Server did not allocate forwarding address"
            IoTVideoError.Term_msg_calling_handshake_timeout -> "Handshake timed out"
            IoTVideoError.Term_msg_calling_token_error -> "Device token verification failed"
            IoTVideoError.Term_msg_calling_all_chn_busy -> "All monitoring channels are busy"
            IoTVideoError.Term_msg_calling_timeout_disconnect -> "Timeout disconnect"
            IoTVideoError.Term_msg_calling_no_find_dst_id, IoTVideoError.Term_msg_gdm_handle_processing -> "Device is processing, if the value changes and the processing is successful, the app will be notified later"
            IoTVideoError.Term_msg_gdm_handle_leaf_path_error -> "Device verification of leaf path is illegal"
            IoTVideoError.Term_msg_gdm_handle_parse_json_fail -> "Device JSON parsing error"
            IoTVideoError.Term_msg_gdm_handle_fail -> "Device action processing failed"
            IoTVideoError.Term_msg_gdm_handle_no_cb_registered -> "Device has not registered the corresponding action callback function"
            else -> IoTVideoError.fromValue(errorCode).name
        }
    }

    @JvmStatic
    val supportedAbi: Array<String>
        get() = Build.SUPPORTED_ABIS

    @JvmStatic
    fun timeFormat(date: Long): String {
        return formatter.format(Date(date))
    }

    @JvmStatic
    fun timeFormatEndDay(date: Long): String {
        return dayFormatter.format(Date(date))
    }
}
