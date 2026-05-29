package com.company

import scala.swing.{MainFrame, Dimension, BorderPanel, FlowPanel, BoxPanel, Orientation, Button, Label, Action}
import javax.swing.Timer
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.Font

object Window:
  /** Уровни скорости: (задержка мс, шагов за тик, название). */
  private val SpeedLevels: Array[(Int, Int, String)] = Array(
    (240, 1, "0.25x"),
    (120, 1, "0.5x"),
    (60,  1, "1x"),
    (30,  1, "2x"),
    (15,  1, "4x"),
    (15,  2, "8x"),
    (15,  4, "16x"),
    (15,  8, "32x"),
    (15,  1, "64x"),
    (15,  8, "128x")
  )
  /** Индекс скорости по умолчанию (1x). */
  private val DefaultSpeedIdx = 2

/** Главное окно с панелью управления симуляцией. */
class Window extends MainFrame:
  import Window.*

  title = "Bioloji"
  resizable = false

  private val petri = new Petri

  // Отдельное окно с геномом выбранной клетки.
  private val genomeWindow = new GenomeWindow(petri)
  petri.onSelectCell = (x, y) =>
    genomeWindow.select(x, y)
    genomeWindow.visible = true
    genomeWindow.peer.toFront()
  // Кнопка «1 тик» в окне генома — один шаг симуляции на паузе.
  genomeWindow.onStepOnce = () => stepOnce()

  // ── состояние симуляции ──────────────────────────────────
  private var speedIdx  = DefaultSpeedIdx
  private var running   = true
  private var tickCount = 0

  // ── индикатор скорости ──────────────────────────────────
  private val speedLabel = new Label(s" Скорость: ${SpeedLevels(speedIdx)._3} ")
  speedLabel.font = new Font("SansSerif", Font.BOLD, 13)

  private def updateSpeedLabel(): Unit =
    speedLabel.text = s" Скорость: ${SpeedLevels(speedIdx)._3} "

  private def setSpeed(idx: Int): Unit =
    speedIdx = math.max(0, math.min(idx, SpeedLevels.length - 1))
    timer.setDelay(SpeedLevels(speedIdx)._1)
    updateSpeedLabel()

  // ── кнопки управления ───────────────────────────────────
  private val btnFont = new Font("SansSerif", Font.PLAIN, 18)

  private val btnPlay = new Button(Action("\u25B6") {
    running = true
    if !timer.isRunning then timer.start()
  })

  private val btnPause = new Button(Action("\u23F8") {
    running = false
  })

  private val btnStop = new Button(Action("\u23F9") {
    running = false
    timer.stop()
    tickCount = 0
    petri.resetPole()
    petri.repaint()
  })

  private val btnFast = new Button(Action("\u23E9") { setSpeed(speedIdx + 1) })
  private val btnSlow = new Button(Action("\u23EA") { setSpeed(speedIdx - 1) })
  private val btnMax  = new Button(Action("\u23EB") { setSpeed(SpeedLevels.length - 1) })
  private val btnMin  = new Button(Action("\u23EC") { setSpeed(0) })

  // Сброс масштаба/сдвига поля к виду «всё целиком».
  private val btnResetView = new Button(Action("1:1") { petri.resetView() })

  // Отдельное окно настроек всех параметров симуляции.
  private val settingsWindow = new SettingsWindow
  private val btnSettings = new Button(Action("\u2699") {
    settingsWindow.visible = true
    settingsWindow.peer.toFront()
  })

  locally {
    for btn <- Seq(btnPlay, btnPause, btnStop, btnFast, btnSlow, btnMax, btnMin, btnResetView, btnSettings) do
      btn.font = btnFont
    btnPlay.tooltip      = "Запуск"
    btnPause.tooltip     = "Пауза"
    btnStop.tooltip      = "Стоп (сброс симуляции)"
    btnFast.tooltip      = "Ускорить"
    btnSlow.tooltip      = "Замедлить"
    btnMax.tooltip       = "Макс. скорость"
    btnMin.tooltip       = "Мин. скорость"
    btnResetView.tooltip = "Сброс вида (масштаб 1:1)"
    btnSettings.tooltip  = "Настройки симуляции (сезоны, солнце, энергия, …)"
  }

  // ── панель управления ───────────────────────────────────
  private val controlPanel = new FlowPanel(
    btnSlow, btnMin, btnPlay, btnPause, btnStop, btnMax, btnFast, speedLabel, btnResetView, btnSettings
  )

  // ── компоновка ──────────────────────────────────────────
  private val southPanel = new BoxPanel(Orientation.Vertical) {
    contents += controlPanel
  }

  contents = new BorderPanel {
    layout(petri) = BorderPanel.Position.Center
    layout(southPanel) = BorderPanel.Position.South
  }

  size = new Dimension(petri.widthWin + 30, petri.heightWin + 120)

  // ── продвижение симуляции ───────────────────────────────
  /** Выполнить `steps` шагов симуляции, сменить сезон при необходимости, перерисовать. */
  private def advance(steps: Int): Unit =
    var s = 0
    while s < steps do
      petri.pole.itr()
      tickCount += 1
      petri.pole.advanceSeason()
      s += 1
    petri.repaint()
    if genomeWindow.visible then genomeWindow.refresh()

  /** Один шаг симуляции по запросу (ставит на паузу — для пошагового разбора). */
  def stepOnce(): Unit =
    running = false
    advance(1)

  // ── таймер симуляции ────────────────────────────────────
  private val timer = new Timer(
    SpeedLevels(speedIdx)._1,
    new ActionListener:
      def actionPerformed(e: ActionEvent): Unit =
        if running then advance(SpeedLevels(speedIdx)._2)
  )
  timer.start()
