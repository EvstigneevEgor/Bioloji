package com.company

import scala.swing.{Frame, BorderPanel, ScrollPane, TextArea, Label, Dimension}
import java.awt.Font

/**
 * Отдельное окно для просмотра генома и состояния выбранной клетки.
 * Содержимое привязано к координатам поля и обновляется на каждом тике,
 * показывая ту клетку, что сейчас находится в выбранной ячейке.
 */
class GenomeWindow(petri: Petri) extends Frame:
  title = "Геном клетки"

  // Координаты выбранной ячейки поля (-1 — ничего не выбрано).
  private var cx = -1
  private var cy = -1

  private val header = new Label
  header.font = new Font("SansSerif", Font.PLAIN, 13)

  private val genArea = new TextArea:
    editable = false
    lineWrap = true
    wordWrap = true
    font = new Font("Monospaced", Font.PLAIN, 14)

  contents = new BorderPanel:
    layout(header) = BorderPanel.Position.North
    layout(new ScrollPane(genArea)) = BorderPanel.Position.Center

  size = new Dimension(380, 340)

  /** Выбрать клетку по координатам поля и показать её данные. */
  def select(x: Int, y: Int): Unit =
    cx = x
    cy = y
    title = s"Геном клетки ($x, $y)"
    refresh()

  /** Обновить отображение (вызывается на каждом тике симуляции). */
  def refresh(): Unit =
    if cx < 0 || cy < 0 || cx >= petri.pole.W || cy >= petri.pole.H then
      header.text = "Клетка не выбрана"
      genArea.text = ""
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

      if k.gen.nonEmpty then
        val ptr = k.cont % k.gen.length
        val marked = k.gen.zipWithIndex
          .map((ch, i) => if i == ptr then s"[$ch]" else ch.toString)
          .mkString
        genArea.text =
          s"длина генома: ${k.gen.length}\n" +
            s"указатель: $ptr (отмечен квадратными скобками)\n\n$marked"
      else
        genArea.text = "(нет генома)"
