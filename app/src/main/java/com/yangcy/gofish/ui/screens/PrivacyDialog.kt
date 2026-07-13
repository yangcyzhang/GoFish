package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class DocType {
    USER_AGREEMENT, PRIVACY_POLICY
}

@Composable
fun PrivacyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var viewDetail by remember { mutableStateOf<DocType?>(null) }

    Box {
        MainPrivacyDialog(
            onViewDetail = { viewDetail = it },
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )

        viewDetail?.let { type ->
            DocDetailView(type = type, onBack = { viewDetail = null })
        }
    }
}

@Composable
private fun MainPrivacyDialog(
    onViewDetail: (DocType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "用户协议与隐私政策",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1A1B)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val annotatedString = buildAnnotatedString {
                    append("感谢您信任并使用 GoFish (爆护)！\n\n")
                    append("我们非常重视您的个人信息保护。在您开始使用前，请务必仔细阅读")
                    
                    withLink(LinkAnnotation.Clickable(tag = "DOC") { 
                        onViewDetail(DocType.USER_AGREEMENT) 
                    }) {
                        withStyle(style = SpanStyle(color = Color(0xFF00ADB5), fontWeight = FontWeight.Bold)) {
                            append("《用户协议》")
                        }
                    }
                    
                    append("与")
                    
                    withLink(LinkAnnotation.Clickable(tag = "DOC") { 
                        onViewDetail(DocType.PRIVACY_POLICY) 
                    }) {
                        withStyle(style = SpanStyle(color = Color(0xFF00ADB5), fontWeight = FontWeight.Bold)) {
                            append("《隐私政策》")
                        }
                    }
                    
                    append("。我们将根据您的授权收集如下核心信息：\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF333333))) {
                        append("1. 地理位置：")
                    }
                    append("由高德地图SDK提供，用于钓点标记、导航及气象建议。\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF333333))) {
                        append("2. 设备信息：")
                    }
                    append("由友盟+SDK收集，用于统计 App 稳定性、捕获崩溃日志及分析功能使用热度。\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF333333))) {
                        append("3. 本地存储：")
                    }
                    append("用于保存您的垂钓点位和渔获记录。\n\n")
                    
                    append("您可以选择拒绝，但可能无法正常使用地图标记等核心功能。点击“同意”即代表您已阅读并接受前述协议。")
                }

                Box(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D6)),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF666666))
                    ) {
                        Text("拒绝并退出", fontSize = 13.sp, maxLines = 1)
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1))
                    ) {
                        Text("同意并继续", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocDetailView(type: DocType, onBack: () -> Unit) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                if (type == DocType.USER_AGREEMENT) "用户协议" else "隐私政策",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val content = if (type == DocType.USER_AGREEMENT) {
                        getUserAgreementText()
                    } else {
                        getPrivacyPolicyText()
                    }
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getUserAgreementText(): String = """
    《GoFish (爆护) 用户协议》
    
    版本更新日期：2026年7月13日
    版本生效日期：2026年7月13日
    
    【首部提示】
    欢迎您使用 GoFish (爆护) 软件及相关服务！在您使用本软件前，请务必审慎阅读并充分理解本协议各条款。
    
    一、 服务内容与账号
    1. 本软件提供钓点标记、渔获日记记录、气象查询及鱼类图鉴浏览等垂钓辅助服务。
    2. 本软件目前采用本地化数据存储方案。您的所有记录均保存在手机本地。如需删除，您可以通过 App 内的“删除”功能或清除 App 数据来完成。
    
    二、 用户行为规范
    1. 您承诺不利用本软件进行任何违法违规行为，包括但不限于标记军事禁区、非法进入受保护的自然保护区或传播违法信息。
    2. 您在使用地图及导航功能时，应严格遵守交通法规及户外安全准则。
    
    三、 风险与免责声明
    1. 垂钓属于高风险户外运动。本软件提供的气象数据、水位及钓点信息仅供参考，不作为安全决策依据。因用户自身行为导致的溺水、触电等意外，开发者不承担法律责任。
    2. 对于因不可抗力、第三方 SDK 故障或网络问题导致的服务中断，开发者在法律允许范围内免责。
    
    四、 未成年人保护
    若您是未满 18 周岁的未成年人，请在监护人的陪同下阅读本协议，并确保在使用本软件前已获得监护人的同意。
    
    五、 如何联系我们
    如对本协议有任何疑问，请通过以下方式联系，我们将在48小时内回复您：
    邮箱：3231573593@qq.com
    GitHub：https://github.com/yangcy/gofish
    
    六、 其他条款
    本协议受中华人民共和国法律管辖。如发生纠纷，各方应友好协商解决。
""".trimIndent()

private fun getPrivacyPolicyText(): String = """
    《GoFish (爆护) 隐私政策》
    
    版本更新日期：2026年7月13日
    版本生效日期：2026年7月13日
    
    引言
    GoFish (爆护)（以下简称“我们”）深知个人信息对您的重要性。我们将严格遵守《中华人民共和国个人信息保护法》等相关法律法规，采取相应安全保护措施。
    
    一、 个人信息收集清单与使用目的
    1. 地理位置信息：为了实现【钓点精准标记、周边导航及针对性的气象实战建议】，我们需要获取您的【精确位置信息】。该功能由高德地图 SDK 提供支持。
    2. 设备标识信息：为了统计 App 稳定性、捕获崩溃日志及分析核心功能（如分享口令）的使用热度，我们会收集您的【设备序列号、Android ID、OAID】。该功能由友盟+ SDK 提供。
    3. 本地存储权限：为了将您拍摄的【渔获图片】、记录的【文字心得】及【坐标点】安全保存在您的设备中，我们需要申请存储/相册访问权限。
    
    二、 第三方 SDK 共享说明
    为了提供地图与统计服务，我们接入了如下 SDK。您可以点击链接查看其隐私政策：
    1. 高德地图 SDK：用于地图展示与定位。
       隐私政策：https://lbs.amap.com/pages/privacy/
    2. 友盟+ SDK：用于 App 稳定性监控与基础统计。
       隐私政策：https://www.umeng.com/policy
    
    三、 您的权利（访问、更正、删除、注销）
    1. 查阅与更正：您可以在地图 or 日记列表中随时查看并编辑您的记录。
    2. 删除与注销：由于目前应用采用本地存储，删除某条记录即为永久删除。如需“注销”所有数据，您只需在手机系统设置中“清除应用数据”或卸载应用，届时所有本地个人信息将彻底清除，无法找回。
    
    四、 未成年人信息保护
    我们非常重视对未成年人信息的保护。本应用不包含针对儿童的诱导性内容。若您是未成年人的监护人，当您对所监护的未成年人是否使用本应用有疑问时，请及时与我们联系。
    
    五、 如何联系我们
    如对本政策有任何疑问、意见或投诉建议，请通过以下方式联系，我们将在48小时内回复您：
    邮箱：3231573593@qq.com
    GitHub：https://github.com/yangcy/gofish
""".trimIndent()
