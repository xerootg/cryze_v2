package com.tencentcs.iotvideo.utils;

import android.os.Build;
import android.text.TextUtils;
import com.tencentcs.iotvideo.IoTVideoError;
import com.tencentcs.iotvideo.IoTVideoSdkConstant;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");

    public static String getDelPlaybackErrMsg(int errorCode) {
        switch (errorCode) {
            case IoTVideoError.DEL_FILE_STROAGE_EXCEPT /* 22078 */:
                return "Storage exception: index file does not exist (cannot be opened), video file cannot be deleted, possible reasons: storage device is unplugged, no access rights, etc.";
            case IoTVideoError.DEL_FILE_RESOURCES_LACK /* 22079 */:
                return "The storage device is unplugged, no access rights, etc.";
            case IoTVideoError.DEL_FILE_DEFAULT_CODE /* 22080 */:
                return "Delete failed: delete exception, execute remove";
            case IoTVideoError.DEL_FILE_INFO_NOT_EXIST /* 22081 */:
                return "Deletion failed: the file requested to be deleted is not in the record list -> cannot be deleted";
            case IoTVideoError.DEL_RESOURCES_BUSY /* 22082 */:
                return "The resource is busy and the operation cannot be executed: the delete command cannot be executed while the resource is being played back, downloaded, or deleted";
            case IoTVideoError.DEL_APP_CANCEL /* 22083 */:
                return "The user-side APP actively canceled this deletion";
            default:
                return "";
        }
    }

    public static String getErrorDescription(int errorCode) {
        switch (errorCode) {
            case IoTVideoError.ASrv_dst_offline: /* 8000 */
                return "Target offline";
            case IoTVideoError.ASrv_dst_notfound_asrv /* 8001 */:
                return "Access server for target not found";
            case IoTVideoError.ASrv_dst_notexsit /* 8002 */:
                return "Target does not exist";
            case IoTVideoError.ASrv_dst_error_relation /* 8003 */:
                return "Illegal relationship chain";
            case IoTVideoError.ASrv_data_chkfrm_fail /* 8004 */:
                return "Frame verification failed";
            case IoTVideoError.ASrv_data_loadjson_fail /* 8005 */:
                return "Failed to load JSON from terminal to object model";
            case IoTVideoError.ASrv_data_modifytick_fail /* 8006 */:
                return "Failed to modify timestamp in object model from terminal JSON";
            case IoTVideoError.ASrv_tocsrv_timeout /* 8007 */:
                return "Communication timeout between access server and central server";
            case IoTVideoError.ASrv_url_parse_fail /* 8008 */:
                return "URL parsing failed";
            case IoTVideoError.ASrv_csrv_reply_err /* 8009 */:
                return "Central server responded with erroneous data";
            case IoTVideoError.ASrv_forward_toASrv_timeout /* 8010 */:
                return "Timeout forwarding message from access server to another access server";
            case IoTVideoError.ASrv_forward_toASrv_fail /* 8011 */:
                return "Failed to forward message from access server to another access server";
            case IoTVideoError.ASrv_forward_toTerm_timeout /* 8012 */:
                return "Timeout forwarding message from access server to device";
            case IoTVideoError.ASrv_forward_toTerm_fail /* 8013 */:
            case IoTVideoError.ASrv_handle_fail /* 8014 */:
                return "Failed to forward message from access server to device";
            case IoTVideoError.ASrv_dstid_parse_faild /* 8015 */:
                return "Access server failed to parse target ID from data frame";
            case IoTVideoError.ASrv_dstid_isuser /* 8016 */:
                return "Access server found target ID is a user";
            case IoTVideoError.ASrv_calc_leaf_fail /* 8017 */:
                return "Access server failed to calculate leaf";
            case IoTVideoError.ASrv_set_timeval_leafval_fail /* 8018 */:
                return "Access server failed to set timeval value in object model";
            case IoTVideoError.ASrv_calc_forward_json_fail /* 8019 */:
                return "Access server failed to calculate forwarding JSON";
            case IoTVideoError.ASrv_tmpsubs_parse_fail /* 8020 */:
                return "Temporary subscription frame failed to parse device ID";
            case IoTVideoError.ASrv_csrvctrl_trgtype_error /* 8021 */:
                return "Central server sent ctl frame with incorrect trgtype";
            case IoTVideoError.ASrv_binderror_dev_usr_has_bind /* 8022 */:
                return "You have already bound this device";
            case IoTVideoError.ASrv_binderror_dev_has_bind_other /* 8023 */:
            case IoTVideoError.ASrv_unformat_jsstr_fail /* 8025 */:
                return "Device is already bound to another user";
            case IoTVideoError.ASrv_binderror_customer_diffrent /* 8024 */:
                return "Device customer ID does not match user customer ID";
            case IoTVideoError.ASrv_netcfg_maketoken_fail /* 8026 */:
                return "Failed to generate token during network configuration";
            case IoTVideoError.ASrv_netcfg_verifytoken_fail /* 8027 */:
                return "Failed to verify token during network configuration";
            case IoTVideoError.ASrv_parse_json_fail /* 8028 */:
                return "JSON parsing error";
            case IoTVideoError.Term_msg_send_peer_timeout /* 20001 */:
                return "Message send to peer timed out";
            case IoTVideoError.Term_msg_calling_hangup /* 20002 */:
                return "Normal hangup message";
            case IoTVideoError.Term_msg_calling_send_timeout /* 20003 */:
                return "Calling message send timed out";
            case IoTVideoError.Term_msg_calling_no_srv_addr /* 20004 */:
                return "Server did not allocate forwarding address";
            case IoTVideoError.Term_msg_calling_handshake_timeout /* 20005 */:
                return "Handshake timed out";
            case IoTVideoError.Term_msg_calling_token_error /* 20006 */:
                return "Device token verification failed";
            case IoTVideoError.Term_msg_calling_all_chn_busy /* 20007 */:
                return "All monitoring channels are busy";
            case IoTVideoError.Term_msg_calling_timeout_disconnect /* 20008 */:
                return "Timeout disconnect";
            case IoTVideoError.Term_msg_calling_no_find_dst_id /* 20009 */:
            case IoTVideoError.Term_msg_gdm_handle_processing /* 20100 */:
                return "Device is processing, if the value changes and the processing is successful, the app will be notified later";
            case IoTVideoError.Term_msg_gdm_handle_leaf_path_error /* 20101 */:
                return "Device verification of leaf path is illegal";
            case IoTVideoError.Term_msg_gdm_handle_parse_json_fail /* 20102 */:
                return "Device JSON parsing error";
            case IoTVideoError.Term_msg_gdm_handle_fail /* 20103 */:
                return "Device action processing failed";
            case IoTVideoError.Term_msg_gdm_handle_no_cb_registered /* 20104 */:
                return "Device has not registered the corresponding action callback function";
            default:
                return "unknown error";
        }
    }

    public static String[] getSupportedAbi() {
        return Build.SUPPORTED_ABIS;
    }

    public static String getSupportedApiString() {
        return TextUtils.join(IoTVideoSdkConstant.AnonymousLogin.MULTI_DEV_SPLIT_REGEX, getSupportedAbi());
    }

    public static String printJson(String jsonStr) {
        return jsonStr;
    }

    public static String timeFormat(long date) {
        return formatter.format(new Date(date));
    }

    public static String timeFormatEndDay(long date) {
        return dayFormatter.format(new Date(date));
    }
}
