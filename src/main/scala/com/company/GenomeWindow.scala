package com.company

import scala.swing.{Action, BorderPanel, BoxPanel, Button, Dimension, FlowPanel, Frame, Label, Orientation, ScrollPane}
import java.awt.Font

/**
 * Отдельное окно для просмотра генома и состояния выбранной клетки.
 * Геном показан в стиле Scratch — стопкой «пазлов»: команды f/s/e/a и
 * цифры-направления. Под стопкой — панель «Почему», объясняющая, что и
 * почему сделает клетка в текущий ход, и компас направления.
 *
 * Содержимое привязано к конкретной клетке (по её идентификатору) и
 * обновляется на каждом тике, следуя за ней при перемещении по полю.
 * Если выбрана пустая ячейка, показывается просто её содержимое.
 */
class GenomeWindow(petri: Petri) extends Frame:
  title = "Геном клетки"

  // Координаты выбранной ячейки поля (-1 — ничего не выбрано).
  private var cx = -1
  private var cy = -1
  // Идентификатор выбранной клетки (0 — следим за ячейкой, а не за клеткой).
  private var trackedId: Long = 0

  /** Колбэк «сделать один шаг симуляции» (задаёт [[Window]]). */
  var onStepOnce: () => Unit = () => ()

  private val header = new Label
  header.font = new Font("SansSerif", Font.PLAIN, 13)

  private val strip = new GenomeStrip
  private val explainView = new ExplainView

  // Сырая строка генома мелким моноширинным шрифтом — для справки/копирования.
  private val rawLabel = new Label(" ")
  rawLabel.font = new Font("Monospaced", Font.PLAIN, 11)
  rawLabel.horizontalAlignment = scala.swing.Alignment.Left

  private val btnStep = new Button(Action("\u23ED 1 тик") { onStepOnce() })
  btnStep.tooltip = "Сделать один шаг симуляции и посмотреть, как сдвинется указатель"

  contents = new BorderPanel:
    layout(header) = BorderPanel.Position.North
    layout(new ScrollPane(strip)) = BorderPanel.Position.Center
    layout(new BoxPanel(Orientation.Vertical) {
      contents += explainView
      contents += new FlowPanel(FlowPanel.Alignment.Left)(btnStep, rawLabel)
    }) = BorderPanel.Position.South

  size = new Dimension(400, 600)

  /** Выбрать клетку по координатам поля и показать её данные. */
  def select(x: Int, y: Int): Unit =
    cx = x
    cy = y
    // Запоминаем идентификатор клетки в этой ячейке, чтобы следить за ней,
    // даже когда она переедет в другую ячейку поля.
    trackedId = petri.pole.idAt(x, y)
    title = s"Геном клетки ($x, $y)"
    refresh()

  /** Обновить отображение (вызывается на каждом тике симуляции). */
  def refresh(): Unit =
    // Если следим за конкретной клеткой — обновляем её текущие координаты.
    if trackedId != 0 then
      petri.pole.findById(trackedId) match
        case Some((x, y)) =>
          cx = x
          cy = y
          title = s"Геном клетки ($x, $y)"
        case None =>
          // Клетка исчезла (съедена/вытеснена) — перестаём следить.
          trackedId = 0

    if cx < 0 || cy < 0 || cx >= petri.pole.W || cy >= petri.pole.H then
      header.text = "Клетка не выбрана"
      strip.update("", -1)
      explainView.update(None, 0)
      rawLabel.text = " "
    else
      val k = petri.pole.kletka(cx, cy)
      val kind =
        if k.isLive then "живая"
        else if k.isCorpse then "труп"
        else "пусто"
      val total = k.fedLight + k.fedPrey + k.fedCorpse
      def pct(v: Int): Int = if total > 0 then v * 100 / total else 0
      header.text =
        s"<html>($cx, $cy) — <b>$kind</b><br>" +
          s"энергия: ${k.energy} &nbsp; ресурс жизни: ${k.timeLive}<br>" +
          s"питание: свет ${pct(k.fedLight)}%, " +
          s"хищник ${pct(k.fedPrey)}%, падальщик ${pct(k.fedCorpse)}%</html>"

      if k.isLive && k.gen.nonEmpty then
        val ptr = k.cont % k.gen.length
        val ex = petri.pole.explain(cx, cy)
        strip.update(k.gen, ptr, GenomeView.jumpInfo(ex))
        explainView.update(Some(ex), k.energy)
        rawLabel.text = s"геном: ${k.gen}"
      else
        strip.update("", -1)
        explainView.update(None, k.energy)
        rawLabel.text = if k.gen.nonEmpty then s"геном: ${k.gen}" else " "
