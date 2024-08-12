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

    public static String getDelPlaybackErrMsg(int i10) {
        switch (i10) {
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

    public static String getErrorDescription(int i10) {
        switch (i10) {
            case 8000:
                return "目标离线";
            case IoTVideoError.ASrv_dst_notfound_asrv /* 8001 */:
                return "没有找到目标所在的接入服务器";
            case IoTVideoError.ASrv_dst_notexsit /* 8002 */:
                return "目标不存在";
            case IoTVideoError.ASrv_dst_error_relation /* 8003 */:
                return "非法关系链";
            case IoTVideoError.ASrv_data_chkfrm_fail /* 8004 */:
                return "校验帧失败";
            case IoTVideoError.ASrv_data_loadjson_fail /* 8005 */:
                return "终端上传的json;加载到物模型失败";
            case IoTVideoError.ASrv_data_modifytick_fail /* 8006 */:
                return "终端上传的json;修改物模型相关的时间戳失败";
            case IoTVideoError.ASrv_tocsrv_timeout /* 8007 */:
                return "接入服务器与中心服务器通信超时";
            case IoTVideoError.ASrv_url_parse_fail /* 8008 */:
                return "url地址解析失败";
            case IoTVideoError.ASrv_csrv_reply_err /* 8009 */:
                return " 中心服务器响应错误的数据";
            case IoTVideoError.ASrv_forward_toASrv_timeout /* 8010 */:
                return "接入服务器转发消息到其他接入服务器超时";
            case IoTVideoError.ASrv_forward_toASrv_fail /* 8011 */:
                return "接入服务器转发消息到其他接入服务器失败";
            case IoTVideoError.ASrv_forward_toTerm_timeout /* 8012 */:
                return "接入服务器转发消息到设备超时";
            case IoTVideoError.ASrv_forward_toTerm_fail /* 8013 */:
            case IoTVideoError.ASrv_handle_fail /* 8014 */:
                return "接入服务器转发消息到设备失败";
            case IoTVideoError.ASrv_dstid_parse_faild /* 8015 */:
                return "接入服务器没有从数据帧中解析出目标ID";
            case IoTVideoError.ASrv_dstid_isuser /* 8016 */:
                return "接入服务器发现目标ID是个用户";
            case IoTVideoError.ASrv_calc_leaf_fail /* 8017 */:
                return "接入服务器计算leaf失败";
            case IoTVideoError.ASrv_set_timeval_leafval_fail /* 8018 */:
                return "接入服务器设置物模型的timeval值失败";
            case IoTVideoError.ASrv_calc_forward_json_fail /* 8019 */:
                return "接入服务器计算转发json失败";
            case IoTVideoError.ASrv_tmpsubs_parse_fail /* 8020 */:
                return "临时订阅帧没有解析出设备ID";
            case IoTVideoError.ASrv_csrvctrl_trgtype_error /* 8021 */:
                return "中心服务器发来的ctl帧，trgtype不对";
            case IoTVideoError.ASrv_binderror_dev_usr_has_bind /* 8022 */:
                return "您已经绑定该设备";
            case IoTVideoError.ASrv_binderror_dev_has_bind_other /* 8023 */:
            case IoTVideoError.ASrv_unformat_jsstr_fail /* 8025 */:
                return "设备已经绑定其他用户";
            case IoTVideoError.ASrv_binderror_customer_diffrent /* 8024 */:
                return "设备的客户ID与用户的客户ID不一致";
            case IoTVideoError.ASrv_netcfg_maketoken_fail /* 8026 */:
                return "配网时生成token失败";
            case IoTVideoError.ASrv_netcfg_verifytoken_fail /* 8027 */:
                return "配网时校验token失败";
            case IoTVideoError.ASrv_parse_json_fail /* 8028 */:
                return "解析JSON错误";
            default:
                switch (i10) {
                    case IoTVideoError.Term_msg_send_peer_timeout /* 20001 */:
                        return "消息发送给对方超时";
                    case IoTVideoError.Term_msg_calling_hangup /* 20002 */:
                        return "普通挂断消息";
                    case IoTVideoError.Term_msg_calling_send_timeout /* 20003 */:
                        return "calling消息发送超时";
                    case IoTVideoError.Term_msg_calling_no_srv_addr /* 20004 */:
                        return "服务器未分配转发地址";
                    case IoTVideoError.Term_msg_calling_handshake_timeout /* 20005 */:
                        return "握手超时";
                    case IoTVideoError.Term_msg_calling_token_error /* 20006 */:
                        return "设备端token校验失败";
                    case IoTVideoError.Term_msg_calling_all_chn_busy /* 20007 */:
                        return "监控通道数满";
                    case IoTVideoError.Term_msg_calling_timeout_disconnect /* 20008 */:
                        return "超时断开";
                    case IoTVideoError.Term_msg_calling_no_find_dst_id /* 20009 */:
                        return "未找到目的id";
                    default:
                        switch (i10) {
                            case IoTVideoError.Term_msg_gdm_handle_processing /* 20100 */:
                                return "设备正在处理中，如果值由变化并且处理成功，稍后会通知APP";
                            case IoTVideoError.Term_msg_gdm_handle_leaf_path_error /* 20101 */:
                                return "设备端校验叶子路径非法";
                            case IoTVideoError.Term_msg_gdm_handle_parse_json_fail /* 20102 */:
                                return "设备端解析JSON出错";
                            case IoTVideoError.Term_msg_gdm_handle_fail /* 20103 */:
                                return "设备处理Action失败";
                            case IoTVideoError.Term_msg_gdm_handle_no_cb_registered /* 20104 */:
                                return "设备未注册相应的Action回调函数";
                            default:
                                return "unknown error";
                        }
                }
        }
    }

    public static String[] getSupportedAbi() {
        return Build.SUPPORTED_ABIS;
    }

    public static String getSupportedApiString() {
        return TextUtils.join(IoTVideoSdkConstant.AnonymousLogin.MULTI_DEV_SPLIT_REGEX, getSupportedAbi());
    }

    public static String printJson(String str) {
        return str;
    }

    public static String timeFormat(long j10) {
        return formatter.format(new Date(j10));
    }

    public static String timeFormatEndDay(long j10) {
        return dayFormatter.format(new Date(j10));
    }
}
