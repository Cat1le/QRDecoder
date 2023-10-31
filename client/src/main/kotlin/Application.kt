import java.awt.*
import kotlin.system.exitProcess

fun main() {
    assert(SystemTray.isSupported()) { "System tray is not supported" }
    val mgr = ServiceManager("C:\\Users\\ohmyc\\Projects\\qdecoder\\service").apply { up() }
    val trayIcon = TrayIcon(
        Toolkit
            .getDefaultToolkit()
            .createImage(ServiceManager::class.java.getResource("icon.png")),
        "QR Decoder"
    )
    val popup = PopupMenu()
    val recognizeItem = MenuItem("Распознать буфер обмена").also { item ->
        item.isEnabled = mgr.isUp()
        item.addActionListener {
            when (val r = mgr.recognize()) {
                is ServiceManager.RecognizeResult.Succeed -> trayIcon.displayMessage(
                    "Распознанно, перемещено в буфер обмена",
                    r.value,
                    TrayIcon.MessageType.INFO
                )
                is ServiceManager.RecognizeResult.Failed -> trayIcon.displayMessage(
                    "Ошибка", when (r.reason) {
                        ServiceManager.RecognizeResult.Failed.Reason.NO_IMAGE -> "В буфере обмена нет изображения"
                        ServiceManager.RecognizeResult.Failed.Reason.NO_CODE -> "На изображении не обнаружен код"
                        ServiceManager.RecognizeResult.Failed.Reason.MANY_CODES -> "На изображении несколько кодов"
                    }, TrayIcon.MessageType.ERROR
                )
            }
        }
    }
    val upItem = CheckboxMenuItem("Запущен").also { item ->
        item.state = mgr.isUp()
        item.addItemListener {
            if (mgr.isUp()) {
                mgr.down()
            } else {
                mgr.up()
            }
            item.state = mgr.isUp()
            recognizeItem.isEnabled = mgr.isUp()
        }
    }
    popup.add(upItem)
    popup.add(recognizeItem)
    popup.addSeparator()
    popup.add(MenuItem("Закрыть").apply { addActionListener { exitProcess(0) } })
    trayIcon.popupMenu = popup
    SystemTray.getSystemTray().add(trayIcon)
}
