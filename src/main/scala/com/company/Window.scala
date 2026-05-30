package com.company

import scala.swing.{MainFrame, Dimension, BorderPanel, FlowPanel, BoxPanel, Orientation, Button, Label, Action}
import javax.swing.Timer
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.Font

object Window:
  /** Базовая частота «1x» в шагах/сек (исторически 60 мс на тик). */
  private val BaseRate = 1000.0 / 60.0

  /** Уровни скорости: (название, целевая частота шагов/сек; ∞ = без ограничения). */
  private val SpeedLevels: Array[(String, Double)] = Array(
    ("0.25x", 0.25 * BaseRate),
    ("0.5x",  0.5 * BaseRate),
    ("1x",    1 * BaseRate),
    ("2x",    2 * BaseRate),
    ("4x",    4 * BaseRate),
    ("8x",    8 * BaseRate),
    ("16x",   16 * BaseRate),
    ("32x",   32 * BaseRate),
    ("64x",   64 * BaseRate),
    ("128x",  128 * BaseRate),
    ("256x",  256 * BaseRate),
    ("512x",  512 * BaseRate),
    ("1024x", 1024 * BaseRate),
    ("2048x", 2048 * BaseRate),
    ("\u221E", Double.PositiveInfinity)
  )
  /** Индекс скорости по умолчанию (1x). */
  private val DefaultSpeedIdx = 2

  /** Частота перерисовки экрана (кадров/сек) — независима от скорости симуляции. */
  private val RenderFps = 30

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
  @volatile private var speedIdx  = DefaultSpeedIdx
  @volatile private var running   = true
  @volatile private var tickCount = 0

  // Замок для безопасной замены/сброса поля между шагами симуляции.
  private val simLock = new AnyRef

  // ── индикатор скорости ──────────────────────────────────
  private val speedLabel = new Label(s" Скорость: ${SpeedLevels(speedIdx)._1} ")
  speedLabel.font = new Font("SansSerif", Font.BOLD, 13)

  private def updateSpeedLabel(): Unit =
    speedLabel.text = s" Скорость: ${SpeedLevels(speedIdx)._1} "

  private def setSpeed(idx: Int): Unit =
    speedIdx = math.max(0, math.min(idx, SpeedLevels.length - 1))
    updateSpeedLabel()

  /** Текущая целевая частота шагов/сек (∞ — без ограничения). */
  private def currentRate: Double = SpeedLevels(speedIdx)._2

  // ── кнопки управления ───────────────────────────────────
  private val btnFont = new Font("SansSerif", Font.PLAIN, 18)

  private val btnPlay = new Button(Action("\u25B6") { running = true })
  private val btnPause = new Button(Action("\u23F8") { running = false })

  private val btnStop = new Button(Action("\u23F9") {
    running = false
    simLock.synchronized {
      tickCount = 0
      petri.resetPole()
    }
    petri.repaint()
  })

  private val btnFast = new Button(Action("\u23E9") { setSpeed(speedIdx + 1) })
  private val btnSlow = new Button(Action("\u23EA") { setSpeed(speedIdx - 1) })
  private val btnMax  = new Button(Action("\u23EB") { setSpeed(SpeedLevels.length - 1) })
  private val btnMin  = new Button(Action("\u23EC") { setSpeed(0) })

  // Сброс масштаба/сдвига поля к виду «всё целиком».
  private val btnResetView = new Button(Action("1:1") { petri.resetView() })

  // Отдельное окно настроек всех параметров симуляции.
  private val settingsWindow = new SettingsWindow(() => simLock.synchronized { petri.resetPole() })
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
    btnMax.tooltip       = "Макс. скорость (∞)"
    btnMin.tooltip       = "Мин. скорость"
    btnResetView.tooltip = "Сброс вида (масштаб 1:1)"
    btnSettings.tooltip  = "Настройки симуляции (режим, потоки, сезоны, энергия, …)"
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

  // ── один шаг симуляции ──────────────────────────────────
  /** Выполнить один быстрый (индексированный) шаг под замком. */
  private def oneStep(): Unit = simLock.synchronized {
    petri.pole.itrFast()
    tickCount += 1
    petri.pole.advanceSeason()
    petri.pole.advanceGravity()
  }

  /** Один шаг симуляции по запросу (ставит на паузу — для пошагового разбора). */
  def stepOnce(): Unit =
    running = false
    oneStep()
    petri.repaint()
    if genomeWindow.visible then genomeWindow.refresh()

  // ── поток симуляции ─────────────────────────────────────
  // Симуляция крутится в отдельном потоке (не на EDT) и не привязана к
  // частоте отрисовки. Темп задаётся «кредитным» ограничителем: за прошедшее
  // время накапливаем разрешённое число шагов и выполняем их.
  private val simThread: Thread = new Thread("bioloji-sim"):
    setDaemon(true)
    override def run(): Unit =
      var last = System.nanoTime()
      var credit = 0.0
      while true do
        if !running then
          Thread.sleep(5)
          last = System.nanoTime()
          credit = 0.0
        else
          val rate = currentRate
          if rate.isInfinite then
            oneStep() // максимальная скорость
          else
            val now = System.nanoTime()
            credit += (now - last) / 1e9 * rate
            last = now
            if credit >= 1.0 then
              // Ограничиваем «догоняющий» всплеск после паузы/лагов.
              if credit > rate.max(1.0) then credit = rate.max(1.0)
              while credit >= 1.0 && running do
                oneStep()
                credit -= 1.0
            else Thread.sleep(1)
  simThread.start()

  // ── таймер отрисовки (на EDT) ───────────────────────────
  private val renderTimer = new Timer(
    1000 / RenderFps,
    new ActionListener:
      def actionPerformed(e: ActionEvent): Unit =
        petri.repaint()
        if genomeWindow.visible then genomeWindow.refresh()
  )
  renderTimer.start()
