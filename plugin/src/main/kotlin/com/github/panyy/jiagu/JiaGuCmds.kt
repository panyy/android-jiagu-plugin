package com.github.panyy.jiagu

import java.io.File
import java.nio.charset.Charset

/**
 * 加固命令类，此类中包含了相关的加固命令，这些命令来源于360加固文档
 */
class JiaGuCmds(extension: JiaGuExtension) {
    //360加固的根目录 jiagu
    private val baseDir: String = extension.getJiaGuHome()

    /**
     * 是否已经进行数据初始化
     */
    private var isInitConfig: Boolean = false

    /**
     * 控制台显示的符串编码
     */
    private var charset: Charset = Charset.defaultCharset()

    init {
        require(File(baseDir).exists()) { "在$baseDir 中找不到可执行文件!" }
        // 设置编码格式
        charset = forNameCharsetOrNull(extension.charsetName) ?: charset
    }

    /**********************************命令*********************************************/
    /**
     * 加固执行的bash命令
     */
    val bashCmd = "java -jar ${baseDir}${File.separator}jiagu.jar"

    /**
     * 执行登陆命令
     */
    val loginCmd = "$bashCmd -login ${extension.username} ${extension.password}"

    /**
     * 导入签名信息命令
     */
    val importSignCmd = "$bashCmd -importsign ${extension.signingConfig.sign} "

    /**
     * 展示签名信息命令
     */
    val showSignCmd = "$bashCmd -showsign"

    /**
     * 配置可选配置
     */
    val configCmd: String? = extension.jiaGuConfig?.let { "$bashCmd -config $it" }

    /**
     * 显示配置信息
     */
    val showConfigCmd = "$bashCmd -showconfig"

    /**
     * 显示配置的版本
     */
    val showVersionCmd = "$bashCmd -version"

    /*******************************************************************************/

    /**
     * 利用控制台执行命令
     * @return 结果信息，过滤了一些不相关的信息
     */
    fun String.executeCmd(): String {
        log("开始执行命令：$this")
        val p = this.execute()
        var result = p.text(charset)
        // 无用信息，tmp中的信息控制台会输出，所以过滤
        val tmp = """
            ################################################
            #                                              #
            #        ## #   #    ## ### ### ##  ###        #
            #       # # #   #   # #  #  # # # #  #         #
            #       ### #   #   ###  #  # # ##   #         #
            #       # # ### ### # #  #  ### # # ###        #
            #                                              #
            # Obfuscation by Allatori Obfuscator v5.6 DEMO #
            #                                              #
            #           http://www.allatori.com            #
            #                                              #
            ################################################
            
        """.trimIndent()
        // 过滤到可执行文件的路径
        if (result.contains(baseDir)) {
            result = result.substring(result.indexOf(baseDir) + baseDir.length)
        }
        //过滤到tmp信息，并过滤每一行开头的回车换行
        result = result.replace(tmp, "").trimStart()
        log("输出结果：$result")
        p.waitFor()  // 用以等待外部进程调用结束
        return result
    }

    /**
     * 执行加固 [apkFile] 需要加固的原始文件, [output] 加固后文件的输出目录
     */
    fun jiagu(apkFile: File, jiaGuApk: File): Boolean {
        // 检测需要加固的Apk是否存在
        require(apkFile.exists()) { "需要加固的apk文件: ${apkFile.absoluteFile}不存在!" }
        // 如果已经登录则不再进行登录
        if (!isInitConfig) {
            // 输出当前版本号
            println()
            println("**********************************************************************************")
            log("获取当前加固程序版本号")
            showVersionCmd.executeCmd()
            // 登录用户
            log("登录到360加固服务器")
            val result = loginCmd.executeCmd()
            if (!result.contains("success")) {
                require(!bashCmd.executeCmd().contains("\"errCode\":0")) {
                    "用户登录失败，检查用户名和密码或请先用助手登录后再重新尝试!"
                }
            }
            // 导入签名信息
            log("导入签名信息")
            importSignCmd.executeCmd()

            // 导入的签名信息回显
            showSignCmd.executeCmd()

            // 配置可选服务：支持x86和奔溃日志记录
            log("配置可选服务")
            configCmd?.executeCmd()

            // 显示配置的可选服务
            showConfigCmd.executeCmd()

            isInitConfig = true
            println("**********************************************************************************")
        }

        // 开始加固
        log("开始对${apkFile.name}进行加固")
        val jiaGuCmd = "$bashCmd -jiagu $apkFile ${jiaGuApk.parent} -autosign"
        val ret = jiaGuCmd.executeCmd().contains("任务完成_已签名")
        log("加固后的路径为：${jiaGuApk.absolutePath}")
        return ret
    }
}