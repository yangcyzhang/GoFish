import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = MainViewControllerKt.MainViewController()
        // 关键修复：强制设置原生视图背景，彻底消除白边
        vc.view.backgroundColor = UIColor(red: 7/255, green: 17/255, blue: 24/255, alpha: 1)
        return vc
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // 使用 ZStack 确保背景色 100% 覆盖，不留任何白缝
        ZStack {
            Color(red: 7/255, green: 17/255, blue: 24/255)
                .ignoresSafeArea(.all)

            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
