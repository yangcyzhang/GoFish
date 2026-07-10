package com.yangcy.gofish.data.model

data class Fish(
    val id: String,
    val name: String,
    val scientificName: String,
    val imageUrl: String,
    val protectionLevel: String, // e.g. "无保护", "国家一级保护", "国家二级保护"
    val isProtected: Boolean, // True if Level is One or Two (strict warning UI)
    val description: String, // General description and body features (精准分辨)
    val habitat: String, // 栖息规律 (detailed)
    val feedingHabit: String, // 觅食规律 (detailed)
    val distribution: String, // 分布区域
    val anglingStrategy: String, // 实战作钓技巧 (bait, water depth, seasonal patterns)
    val rodRecommendation: String, // 推荐钓竿
    val lineRecommendation: String, // 推荐线组
    val baitRecommendation: String, // 推荐饵料
    val defaultWeatherTips: String // 默认天气作钓建议
)

object FishData {
    val fishList = listOf(
        Fish(
            id = "crucian_carp",
            name = "鲫鱼 (Crucian Carp)",
            scientificName = "Carassius auratus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Crucian_carp_2011_G1.jpg/640px-Crucian_carp_2011_G1.jpg",
            protectionLevel = "无保护 (低危广布)",
            isProtected = false,
            description = "体侧扁而双侧对称，背部青褐色，腹部银白。鳞片较小，侧线鳞一般为28-30枚。具有两对须的退化印记（实际无须）。生命力极强，对低氧环境耐受度高。体型随栖息水域深度和食物量变化明显。",
            habitat = "底栖性鱼类。喜好栖息在湖泊、河流、池塘的中下层，尤其是在水草茂密、淤泥深厚、有倒木等能遮蔽阳光的静水或缓流水域活动。",
            feedingHabit = "温和杂食性。主要以底栖无脊椎动物（如红虫、摇蚊幼虫）、水生昆虫、藻类、水草嫩芽以及底层的有机碎屑、腐殖质为食。四季皆有食欲，春秋最活跃。清晨、傍晚是沿草边觅食的高峰期。",
            distribution = "除青藏高原外，全国各大江河湖库、池塘沟渠均有广泛分布，是野钓最常见的对象鱼之一。",
            anglingStrategy = "最适水温为15℃-25℃。春钓浅滩，夏钓深潭/背阴，秋钓阴凉，冬钓阳。调漂以偏灵敏为主（如调四钓二），抓稳健的下顿漂相或缓慢送漂。打窝用酒米，能持久留鱼。",
            rodRecommendation = "3.6米至4.5米轻量软调手竿（37调），主打手感与精细度。",
            lineRecommendation = "主线：0.8-1.2号；子线：0.4-0.6号（搭配2-4号袖钩）。",
            baitRecommendation = "春腥（红虫/蚯蚓/腥面饵）、夏淡（麦香/谷物面饵）、秋香、冬浓腥。",
            defaultWeatherTips = "最喜高气压的微风晴天 or 阴天。气压低于1005hPa或闷热天时易浮头，食欲极低。"
        ),
        Fish(
            id = "grass_carp",
            name = "草鱼 (Grass Carp)",
            scientificName = "Ctenopharyngodon idella",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Ctenopharyngodon_idella.jpg/640px-Ctenopharyngodon_idella.jpg",
            protectionLevel = "无保护 (经济鱼类)",
            isProtected = false,
            description = "体呈圆筒形，头部稍扁平，无须。体色呈茶黄色，背部青褐色，腹部灰白，鳞片边缘带黑褐色，排列整齐。食量惊人，生长极快，是著名的淡水“四大家鱼”之一。",
            habitat = "栖息在江河、湖泊、水库的中下层水域。活性受水温影响极大，水温高于20℃时非常活跃，常游至岸边近草区或入水口，冬季水温低时在深水区聚群越冬。",
            feedingHabit = "典型草食性。幼鱼期摄食浮游动物，成鱼则专食高等水生植物（苦草、轮叶黑藻等）和陆生禾本科嫩草。具有明显的“逐温、逐草”觅食规律。晴天光照强时，喜浮于中上层啃食水面飘浮的嫩草叶。",
            distribution = "原产于中国各大江河，现作为重要经济鱼类已推广至全国各地，在各大水库和大型湖泊中极其常见。",
            anglingStrategy = "草鱼体型大、爆发力强。作钓常采用“浮钓草”（将嫩草捆扎浮于水面） or “底钓玉米”。钓点应选在水库乱石或乱草丛生处、入水口、风口。中鱼后必须耐住性子遛鱼，防范其前三次极其凶猛的“打桩”挣扎。",
            rodRecommendation = "5.4米至7.2米强力硬调手竿（28调），或使用大导环抛竿/矶竿。",
            lineRecommendation = "主线：3.0-5.0号；子线：2.0-3.0号（搭配7-10号伊势尼钩）。",
            baitRecommendation = "新鲜嫩草芽、芦苇心、煮熟的嫩玉米粒、酸甜发酵麦粒、水果香型商品饵。",
            defaultWeatherTips = "喜高温。夏秋晴朗、气温25-32℃、微风拂面时活性最高。气温骤降、阴冷或低压闷热天活性极差。"
        ),
        Fish(
            id = "largemouth_bass",
            name = "大口黑鲈 (Largemouth Bass)",
            scientificName = "Micropterus salmoides",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/f/f0/Largemouth_bass_Micropterus_salmoides_USFWS.jpg",
            protectionLevel = "无保护 (广布引入种)",
            isProtected = false,
            description = "体侧扁，背部黑绿色，腹部灰白。口裂极大，下颌突出。体侧从吻端至尾鳍基有一条明显的暗黑色宽横带。鳞片坚硬。视力及侧线极其敏锐，能感知极细微的水流震动。",
            habitat = "喜暖水，最适水温20℃-30℃。主要栖息于湖泊、水库、缓流河川的中层及岸边障碍物区（如树枝倒木、乱石堆、桥墩、水草丛边缘）。极少出现在泥底无障碍物的开阔死水区。",
            feedingHabit = "典型凶猛肉食性。终生以捕食活体小鱼、虾、蟹、蛙类及落水昆虫为食。具有强烈伏击天性，常静止在树荫或暗礁旁，一旦发现目标便如离弦之箭般弹射吞噬。清晨、傍晚、或者阴雨天活性最高。",
            distribution = "原产北美，我国自20世纪80年代引入，现华南、华东、华中等大部分地区的水库、路亚基地均有大量分布和自繁种群。",
            anglingStrategy = "主流玩法为“路亚钓法（Lure）”。利用拟饵的泳姿和声光震动诱发其攻击天性。作钓讲究“找障碍物”。清晨用波扒（Popper）或铅笔钓水面系，能欣赏到震撼的“炸水”一幕；中午光照强时，用软虫配德州钓组（Texas Rig）搜索结构深水区。",
            rodRecommendation = "路亚竿 M（Medium）或 ML（Medium Light）调性，快速/超快速率，纺车轮或水滴轮。",
            lineRecommendation = "PE线：0.8号 - 1.5号，搭配10lb-16lb碳素前导线（耐磨防咬断）。",
            baitRecommendation = "波扒、米诺、无铅软虫、德州钓组、亮片、软饵卷尾蛆。",
            defaultWeatherTips = "最爱微风、多云或细雨天气，此时它们会离开障碍深处主动出击，全水层活性极高。"
        ),
        Fish(
            id = "culter",
            name = "翘嘴红鲌 (Culther / 翘嘴)",
            scientificName = "Erythroculter ilishaeformis",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/Culter_alburnus_by_Shedd_Aquarium.jpg",
            protectionLevel = "无保护 (淡水佳品)",
            isProtected = false,
            description = "体细长，侧扁，背部平直。口上位，下颌坚厚并向上急剧翘起。侧线非常直。体色背部呈青灰色，腹部及身体两侧呈耀眼的亮银白色。游动极速，成群活动，是水体中上层的掠食者。",
            habitat = "典型开阔水域中上层鱼类。极度喜好大水面（大水库、大湖泊），喜在风浪大、含氧量高、水流湍急的地方（如大坝前、入水口、主航道风口）聚群活动。极少在小死水潭或泥浊浅滩停留。",
            feedingHabit = "肉食性。针对上层小杂鱼（餐条、麦穗鱼）实施高速拦截追逐。视觉极佳，具有明显的夜行性，在夏秋夜间，常游到岸边路灯照耀的浅水区或者岸边乱石坡狙击小鱼。",
            distribution = "广泛分布于我国各大江河湖泊，以太湖、千岛湖、丹江口水库、万峰湖等地出产的翘嘴最为著名。",
            anglingStrategy = "翘嘴嘴唇薄易撕裂，中鱼后不可强拉。由于其游动性大，常需要远投。路亚作钓常采用亮片或斜口亮片，收线时轻轻抖动竿尖模仿受伤小鱼。若使用手竿，需打“泥鳅窝”或用活河虾，采用“飞铅”钓浮，抓下接或横漂的截杀口。",
            rodRecommendation = "路亚竿 ML（中软）或 M（中调），长度2.1m-2.4m以便远投；手竿需5.4m-6.3m轻量竿。",
            lineRecommendation = "PE线：0.6号 - 1.0号，搭配2.0号-3.0号碳素前导线。手竿线组宜轻巧。",
            baitRecommendation = "亮片（Spoon）、斜口亮片、米诺（Minnow）、铅笔、活虾、活泥鳅、商品腥香饵钓浮。",
            defaultWeatherTips = "极度喜风。刮风天（2-4级西北风或东北风）、阴雨天、大坝泄洪流水时，是捕食狂欢期，极好钓。"
        ),
        Fish(
            id = "mandarin_fish",
            name = "鳜鱼 (Mandarin Fish / 桂花)",
            scientificName = "Siniperca chuatsi",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e9/Siniperca_chuatsi_at_Beijing_Zoo.jpg",
            protectionLevel = "无保护 (名贵淡水鱼)",
            isProtected = false,
            description = "体高，侧扁，口大，下颌突出。体色呈棕黄色或黄绿色，遍布不规则的黑褐色斑纹、斑块。身体各鳍均有坚硬的毒棘（被刺中红肿痛剧烈，需极小心）。底栖伪装猎手，能在碎石缝间保持静止隐蔽。",
            habitat = "底栖性肉食鱼类。喜栖息于缓流江河、湖泊、水库的乱石缝、桥墩、乱石堆、树根、陡坎等障碍物丰富的底层。不喜欢强光，白天基本躲藏在障碍深处不动，黄昏和夜间才游出伏击。",
            feedingHabit = "专性肉食性. 终生只捕食活体鱼虾（餐条、麦穗鱼、虾），几乎不食死饵。伏击猎手，利用体表色斑完美融入环境，等待猎物游过时突然一口吞入，吞咽能力极强。",
            distribution = "遍布我国各大主要水系（黑龙江、海河、黄河、长江、珠江等），其中长江、珠江流域分布最密，品质最高。",
            anglingStrategy = "鳜鱼作钓以“敲底”为主。路亚采用铅头钩配双尾或卷尾软虫、宽身胖子（Crankbait）、米诺等。钓点必须死死扣住大乱石缝或桥墩侧面，拟饵要贴底慢收。中鱼后必须迅速提竿，防止其钻回石缝导致卡线挂底。",
            rodRecommendation = "高感度路亚竿 L（轻调）或 ML（中轻调），调性偏快，碳素纤维竿，能清晰传递敲底振动。",
            lineRecommendation = "PE线：0.8号 - 1.2号；配合2.5号 - 4.0号高强碳素前导线（防止乱石磨线）。",
            baitRecommendation = "铅头钩软虫（Jig Head with Grub）、宽体胖子、深潜米诺、活虾、泥鳅底钓。",
            defaultWeatherTips = "阴雨连绵、气压平稳、黄昏或夜间是鳜鱼捕食的黄金时间，极易开口。"
        ),
        Fish(
            id = "chinese_sturgeon",
            name = "中华鲟 (Chinese Sturgeon)",
            scientificName = "Acipenser sinensis",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Acipenser_sinensis_by_Shedd_Aquarium.JPG",
            protectionLevel = "国家一级保护动物 (极危)",
            isProtected = true,
            description = "体呈纺锤形，吻尖突，口特化在腹面。体表无鳞，而是排列着五行硬骨板（骨甲）。古老的活化石，体长可达4-5米，重可达千斤。是中国特有的珍稀濒危物种。",
            habitat = "大江大河的中下层深水区及近海。具有长距离洄游习性，在海洋成长，成年后历经万里洄游至长江上游产卵。",
            feedingHabit = "底栖食性. 口特化在下侧，用敏锐的吻部在江底沙石中拱挖，捕食底栖无脊椎动物、甲壳类、软件动物及部分底层小鱼。",
            distribution = "主要分布于长江干流，产卵场集中于葛洲坝下游。偶见于沿海及珠江口。",
            anglingStrategy = "【严禁作钓】作为国家一级保护野生动物，具有极高的科研和生态价值。我国法律严禁任何形式的针对性捕捉、伤害和作钓。若在野钓中误钓，千万不可用力拉扯、不可使鱼体离开水面过久，必须小心剪断钓线、轻柔取钩，将伤害降到最低，并立即原地放生。若鱼体受伤，应拍照记录并立即联系当地渔政部门协同救治。",
            rodRecommendation = "法律禁钓：严禁针对该物种进行钓鱼活动！",
            lineRecommendation = "法律禁钓：误钓须无条件第一时间释放！",
            baitRecommendation = "法律禁钓：禁止使用任何管制品或诱饵针对其作钓！",
            defaultWeatherTips = "任何天气均不可钓！保护中国古老生灵，文明合法垂钓。"
        ),
        Fish(
            id = "banded_shark",
            name = "胭脂鱼 (Chinese High-Fin Banded Shark)",
            scientificName = "Myxocyprinus asiaticus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Myxocyprinus_asiaticus.JPG/640px-Myxocyprinus_asiaticus.JPG",
            protectionLevel = "国家二级保护动物",
            isProtected = true,
            description = "体侧扁，背鳍极高呈帆状，被称为“一帆风顺”。幼鱼身体具有三条黑色宽斜带，体色艳丽；随着生长发育，成鱼体色逐渐转为紫红色或红褐色，体侧现胭脂色宽带。性温顺。",
            habitat = "喜栖息于大江大河的中下层急流 and 微流水中，喜在砾石、沙底或乱石底的水域游动，不喜泥污死水。",
            feedingHabit = "杂食性偏底栖。主要用下位口吻在砾石表面刮食藻类、着生生物，或摄食昆虫幼虫、软件动物、底栖无脊椎动物及水草碎屑。",
            distribution = "长江水系及闽江水系，目前野生群落数量稀少，处于极度受保护状态。",
            anglingStrategy = "【严禁作钓】胭脂鱼属于国家二级保护动物，禁止垂钓。若在长江野钓中误钓，请立刻轻轻起钩并在水边原地放归大自然，不可带离现场或进行买卖，否则将触犯国家野生动物保护法。",
            rodRecommendation = "法律禁钓：严禁针对该物种进行钓鱼活动！",
            lineRecommendation = "法律禁钓：误钓须立即放生！",
            baitRecommendation = "法律禁钓：禁止使用任何饵料针对其作钓！",
            defaultWeatherTips = "任何天气均不可钓！珍爱江河生灵，守护绿水青山."
        ),
        Fish(
            id = "common_carp",
            name = "鲤鱼 (Common Carp)",
            scientificName = "Cyprinus carpio",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Common_carp_2011_G2.jpg/640px-Common_carp_2011_G2.jpg",
            protectionLevel = "无保护 (经济鱼类)",
            isProtected = false,
            description = "体呈纺锤形，侧扁，口下位，有两对须（这是与鲫鱼最明显的区分特征）。鳞片大且带金黄色光泽，侧线鳞36-39枚。尾鳍下叶常呈红色。极其聪明，对钓线阻力有极强的警惕性。",
            habitat = "底栖温水性鱼类。喜栖息于水草丛生、池底富含腐殖质的湖泊、水库、池塘、缓流河川的中下层底泥中。",
            feedingHabit = "杂食性。用吻部拱泥摄食摇蚊幼虫、螺类、蚌类、水生昆虫、植物碎屑及种子。食量极大，拱泥时水面常泛起密集的“鲤鱼星”（碎泡）。",
            distribution = "全国各大水系、湖泊和水库皆有广泛分布。",
            anglingStrategy = "“鲤鱼爱拱泥，钓鲤要钓底”。选择泥底、乱石堆、深浅交界处。打窝用发酵五谷杂粮，作钓抓下顿或黑漂（鲤鱼吃饵常有试探，浮漂先抖动，后沉重下顿或拉走）。中鱼后提竿要稳，防范其强力的冲刺。",
            rodRecommendation = "4.5米至6.3米中硬调手竿（19调或28调），或海竿/前打竿。",
            lineRecommendation = "主线：2.5-4.0号；子线：1.5-2.5号（搭配5-8号新关东或伊势尼钩）。",
            baitRecommendation = "熟甜玉米、发酵红薯、螺蛳肉、甜香或薯香型商品饵。",
            defaultWeatherTips = "最喜温和天气。风浪2-3级、多云微风或雨后放晴时食欲最旺盛。"
        ),
        Fish(
            id = "catfish",
            name = "鲶鱼 (Amur Catfish)",
            scientificName = "Silurus asotus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Amur_catfish_%28Silurus_asotus%29.jpg/640px-Amur_catfish_%28Silurus_asotus%29.jpg",
            protectionLevel = "无保护 (肉食对象鱼)",
            isProtected = false,
            description = "体长形，头部扁平，口裂极宽. 周身无鳞，体表多黏液。有两对长须（颌须和颐须），对触觉和化学感知极其灵敏。背鳍退化，臀鳍极长并与尾鳍相连。",
            habitat = "底栖性鱼类。极度喜暗避光，白天常成群潜伏在水底乱石缝、桥墩下、树根孔洞或底层深潭中，几乎不活动。",
            feedingHabit = "凶猛肉食性。夜行性强，日落后开始异常活跃。捕食小鱼、虾、蛙类、泥鳅、水生昆虫，甚至水面游动的小型水鸟或腐肉。",
            distribution = "全国江河湖库、池塘均有广泛分布，尤以长江、珠江及东北黑龙江流域为多。",
            anglingStrategy = "“钓鲶要钓暗，钓夜不钓白”。选择风雨天、泥浊水、黄昏或夜间。在乱石堆、陡坝下、入水口旁作钓。鲶鱼咬钩极其凶猛，常直接黑漂或拉竿，提竿宜稍迟以确保吞钩。",
            rodRecommendation = "抛竿（海竿）、重草洞插竿，或5.4米以上强力硬调手竿。",
            lineRecommendation = "主线：3.0-5.0号；防咬子线或大力马线：2.0号以上（搭配8-12号伊势尼钩）。",
            baitRecommendation = "大青蚯蚓、小泥鳅、活河虾、新鲜鸡肝、猪肝。",
            defaultWeatherTips = "最爱暴雨过后水质浑浊之时、或者闷热雷雨前夕的夜间，此时全员出洞觅食。"
        ),
        Fish(
            id = "snakehead",
            name = "黑鱼 (Snakehead / 乌鳢)",
            scientificName = "Channa argus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Northern_snakehead_from_Virginia_waterways.jpg/640px-Northern_snakehead_from_Virginia_waterways.jpg",
            protectionLevel = "无保护 (掠食性路亚鱼)",
            isProtected = false,
            description = "体呈圆筒形，头部扁平如蛇，体侧有两行黑色斑块。口大，具尖锐细齿。具有辅助呼吸器官（鳃上腔），能直接呼吸空气，在无水潮湿环境能存活数天。具有强烈的护仔本能。",
            habitat = "栖息在水草丛生、淤泥底质、静水或缓流的浅水区，如湖泊沿岸、沼泽、池塘及水库湾汊中。对恶劣缺氧环境适应力极强。",
            feedingHabit = "极其凶猛的掠食性鱼类。捕食各种淡水小鱼、虾、青蛙、泥鳅、落水昆虫。常隐蔽在浮萍、荷叶下伏击，捕食瞬间爆发力惊人，声如闷雷。",
            distribution = "全国各主要水系均有分布，长江、淮河、黑龙江水系产量最高。",
            anglingStrategy = "主要采用路亚钓法（“雷强”玩法）。用雷蛙在密草区、荷叶堆、浮萍表面拖拽抖动。黑鱼咬钩会发出巨大的“炸水”声，听到声响后需延时1-2秒大力扬竿刺鱼，并以最快速度将鱼强行拉出重草区。",
            rodRecommendation = "雷强专用路亚竿（H调或XH超硬调），搭配强力鼓轮或水滴轮。",
            lineRecommendation = "PE线：4.0-8.0号（50lb-80lb），无需前导线，直接直结雷蛙。",
            baitRecommendation = "各种颜色的雷蛙（Thunder Frog）、双尾软虫、雷强铅笔、活泥鳅、小青蛙。",
            defaultWeatherTips = "最喜晴朗高温夏日。阳光暴晒、水面浮萍密布、气温28-35℃时，黑鱼最喜欢浮于草洞边缘晒太阳和捕食。"
        )
    )
}
