// SPDX-License-Identifier: BSD-3-Clause-Clear
/*
 *Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 */
package com.android.camera.util;

import com.android.camera.util.PersistUtil;

public class Log {

    public enum logType
    {
        verbose,
        debug,
        info,
        warn,
        error;
    }
    private static StackTraceElement stackTrace;
    public static void v(String tag, String msg) {
        if(PersistUtil.getCamera2Debug() >= PersistUtil.CAMERA2_DEBUG_DUMP_LOG) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.verbose, msg, null);
        }
    }
    public static void v(String tag,int loglevel, String msg) {
        if(PersistUtil.getCamera2Debug() >= loglevel) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.verbose, msg, null);
        }
    }
    public static void d(String tag, String msg) {
        if(PersistUtil.getCamera2Debug() >= PersistUtil.CAMERA2_DEBUG_DUMP_LOG) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.debug, msg, null);
        }
    }
    public static void d(String tag,int loglevel, String msg) {
        if(PersistUtil.getCamera2Debug() >= loglevel) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.debug, msg, null);
        }
    }
    public static void d(String tag,String msg, Throwable tr) {
        if(PersistUtil.getCamera2Debug() >= PersistUtil.CAMERA2_DEBUG_DUMP_LOG) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.debug, msg, tr);
        }
    }


    public static void i(String tag, String msg) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.info, msg,null);
    }
    public static void i(String tag,String msg, Throwable tr) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.info, msg, tr);
    }

    public static void w(String tag, String msg) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.warn, msg,null);
    }
    public static void w(String tag, int loglevel,String msg) {
        if(PersistUtil.getCamera2Debug() >= loglevel) {
            stackTrace = Thread.currentThread().getStackTrace()[3];
            LogPrint(tag, logType.warn, msg, null);
        }
    }
    public static void w(String tag, Throwable tr) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.warn, "", tr);
    }
    public static void w(String tag,String msg, Throwable tr) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.warn, msg, tr);
    }
    public static void e(String tag, String msg) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.error, msg,null);
    }
    public static void e(String tag, Throwable tr) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.error, "", tr);
    }
    public static void e(String tag,String msg, Throwable tr) {
        stackTrace = Thread.currentThread().getStackTrace()[3];
        LogPrint(tag,logType.error, msg, tr);
    }

    private static String LogPrint(String tag,logType type,String msg, Throwable tr) {

        msg = stackTrace.getMethodName() + "[" + stackTrace.getLineNumber() + "] - " + msg;
            switch(type)
            {
                case verbose:
                    android.util.Log.v(tag, msg, tr);
                    break;
                case debug:
                    android.util.Log.d(tag,  msg, tr);
                    break;
                case info:
                    android.util.Log.i(tag,  msg, tr);
                    break;
                case warn:
                    android.util.Log.w(tag,  msg, tr);
                    break;
                case error:
                    android.util.Log.e(tag,  msg, tr);
                    break;
                default:
                    break;
            }
        return stackTrace.getFileName() + ":" + stackTrace.getMethodName() + "[" + stackTrace.getLineNumber() + "]";
    }
}
