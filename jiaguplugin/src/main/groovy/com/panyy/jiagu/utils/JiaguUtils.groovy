package com.panyy.jiagu.utils

import com.android.build.gradle.api.BaseVariantOutput
import com.panyy.jiagu.entity.JiaGuPluginExtension

class JiaguUtils {

    private static String commandJiaGu
    private static String commandExt = ""

    static void jiagu(JiaGuPluginExtension jiaGuExtension, BaseVariantOutput variantOutput) {
        commandExt = ""
        commandJiaGu = "${jiaGuExtension.jiaGuDir}${File.separator}java${File.separator}bin${File.separator}java -jar ${jiaGuExtension.jiaGuDir}${File.separator}jiagu.jar "
        // 登录
        String result = login(jiaGuExtension)
        if (result.contains("success")) {
            Logger.debug("login success")
            // 导入签名keystore信息
            result = importSign(jiaGuExtension)
            if (result.contains("succeed")) {
                result = "导入签名 succeed"
            }
            Logger.debug(result)
            // 导入渠道信息
            Logger.debug(importMulPkg(jiaGuExtension))
            // 配置加固服务
            result = setConfig(jiaGuExtension)
            if (result.contains("config saving succeed.")) {
                def indexOf = result.indexOf("已选增强服务")
                if (indexOf > -1) {
                    result = result.substring(indexOf).trim()
                } else {
                    result = "已选增强服务：${jiaGuExtension.config}"
                }
            }
            Logger.debug(result)
            Logger.debug("加固中... " + variantOutput.outputFile.path)
            // 加固
            result = jiaguStart(jiaGuExtension, variantOutput)
            if (result.contains("任务完成_已签名")) {
                result = "任务完成_已签名"
            }
            Logger.debug(result)
            Logger.debug("输出目录：${jiaGuExtension.outputFileDir}")
        } else {
            Logger.debug(result)
            throw new RuntimeException("登录失败")
        }
    }

    /**
     * 1.加固登录
     */
    private static String login(JiaGuPluginExtension jiaGuExtension) {
        return ProcessUtils.exec(commandJiaGu + " -login ${jiaGuExtension.username} ${jiaGuExtension.password}")
    }

    /**
     * 2.导入签名信息
     */
    private static String importSign(JiaGuPluginExtension jiaGuExtension) {
        if (jiaGuExtension.signingConfig != null) {
            commandExt += " -autosign "
            return ProcessUtils.exec(commandJiaGu + " -importsign ${jiaGuExtension.getSign()}")
        }
        return "未导入签名信息"
    }

    /**
     * 3.导入渠道信息
     */
    private static String importMulPkg(JiaGuPluginExtension jiaGuExtension) {
        if (jiaGuExtension.channelFile != null && jiaGuExtension.channelFile.exists()) {
            commandExt += " -automulpkg "
            return ProcessUtils.exec(commandJiaGu + " -importmulpkg ${jiaGuExtension.channelFile}")
        }
        return "未导入渠道信息"
    }

    /**
     * 4.配置加固服务
     */
    private static String setConfig(JiaGuPluginExtension jiaGuExtension) {
        // 配置加固服务
        return ProcessUtils.exec(commandJiaGu + " -config ${jiaGuExtension.config}")
    }

    /**
     * 5.加固
     */
    private static String jiaguStart(JiaGuPluginExtension jiaGuExtension, BaseVariantOutput variantOutput) {
        // 应用加固
        String cmd = commandJiaGu + " -jiagu ${variantOutput.outputFile.absolutePath} ${jiaGuExtension.outputFileDir}"
        return ProcessUtils.exec(cmd + commandExt)
    }
}