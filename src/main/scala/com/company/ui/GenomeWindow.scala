package com.company.ui

import scala.swing.{Action, BorderPanel, BoxPanel, Button, Dimension, FlowPanel, Frame, Label, Orientation, ScrollPane, TextField}
import scala.swing.event.EditDone
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.company.app.SimulationEngine
import com.company.domain.Genome

/**
 * Отдельное окно для просмотра генома и состояния выбранной клетки.
 * Геном показан в стиле Scratch — стопкой «пазлов»; под ней — панель «Почему»
 * и компас направления. Содержимое привязано к конкретной клетке (по её
 * идентификатору) и следует за ней при перемещении по полю. Чтение мира идёт
 * через read-only [[com.company.domain.WorldView]]; правка генома живой клетки
 * выполняется не напрямую, а через колбэк [[onEditGenome]] (его задаёт
 * [[MainWindow]], оборачивая запись в синхронизацию с тиком симуляции).
 */
class GenomeWindow(engine: SimulationEngine) extends Frame:
  title = "Геном клетки"

  private def world = engine.world

  // Координаты выбранной ячейки поля (-1 — ничего не выбрано).
  private var cx = -1
  private var cy = -1
  // Идентификатор выбранной клетки (0 — следим за ячейкой, а не за клеткой).
  private var trackedId: Long = 0

  /** Колбэк «сделать один шаг симуляции» (задаёт [[MainWindow]]). */
  var onStepOnce: () => Unit = () => ()

  /**
   * Колбэк «заменить геном живой клетки в (x, y) на code». Задаёт [[MainWindow]],
   * оборачивая запись в синхронизацию с тиком симуляции.
   * @return true, если клетка была живой и геном применён.
   */
  var onEditGenome: (Int, Int, String) => Boolean = (_, _, _) => false

  private val header = new Label
  header.font = new Font("SansSerif", Font.PLAIN, 13)

  private val strip = new GenomeStrip
  private val explainView = new ExplainView

  // Редактируемое поле генома: показывает код живой клетки и принимает правку.
  private val genomeField = new TextField(" ")
  genomeField.font = new Font("Monospaced", Font.PLAIN, 12)
  genomeField.preferredSize = new Dimension(180, genomeField.preferredSize.height)
  genomeField.tooltip = s"Геном клетки. Допустимые символы: ${Genome.Alphabet}. Enter — применить."

  private val btnStep = new Button(Action("\u23ED 1 тик") { onStepOnce() })
  btnStep.tooltip = "Сделать один шаг симуляции и посмотреть, как сдвинется указатель"

  private val btnCopy = new Button(Action("\u29C9 Копировать") { copyGenome() })
  btnCopy.tooltip = "Скопировать геном клетки в буфер обмена"

  private val btnApply = new Button(Action("Применить") { applyEdit() })
  btnApply.tooltip = "Заменить геном выбранной клетки на введённый"

  // Enter в поле = применить правку.
  genomeField.listenTo(genomeField)
  genomeField.reactions += { case EditDone(_) => applyEdit() }

  private val genomeCaption = new Label("геном:")
  genomeCaption.font = new Font("SansSerif", Font.PLAIN, 12)

  contents = new BorderPanel:
    layout(header) = BorderPanel.Position.North
    layout(new ScrollPane(strip)) = BorderPanel.Position.Center
    layout(new BoxPanel(Orientation.Vertical) {
      contents += explainView
      contents += new FlowPanel(FlowPanel.Alignment.Left)(btnStep)
      contents += new FlowPanel(FlowPanel.Alignment.Left)(genomeCaption, genomeField, btnCopy, btnApply)
    }) = BorderPanel.Position.South

  size = new Dimension(440, 600)

  /** Копировать текущий геном клетки в системный буфер обмена. */
  private def copyGenome(): Unit =
    val sel = new StringSelection(genomeField.text)
    Toolkit.getDefaultToolkit.getSystemClipboard.setContents(sel, sel)

  /**
   * Применить правку генома: очистить ввод от посторонних символов и, если
   * результат непуст и отличается от текущего генома клетки, отдать его в
   * колбэк [[onEditGenome]]. Пустой ввод и ввод «без изменений» игнорируются —
   * чтобы простой уход фокуса не сбрасывал указатель клетки. После применения
   * окно перечитывает модель и перерисовывает стек блоков.
   */
  private def applyEdit(): Unit =
    if cx < 0 || cy < 0 || cx >= world.W || cy >= world.H then return
    val sanitized = Genome.sanitize(genomeField.text)
    val current = world.cell(cx, cy).genome.code
    if sanitized.nonEmpty && sanitized != current && onEditGenome(cx, cy, sanitized) then
      genomeField.text = sanitized
    refresh()

  /** Включить/выключить элементы правки (доступны только для живой клетки). */
  private def setEditEnabled(on: Boolean): Unit =
    genomeField.enabled = on
    btnCopy.enabled = on
    btnApply.enabled = on

  /** Обновить текст поля из модели, не затирая правку, пока поле в фокусе. */
  private def syncGenomeField(code: String): Unit =
    if !genomeField.peer.isFocusOwner && genomeField.text != code then
      genomeField.text = code

  /** Выбрать клетку по координатам поля и показать её данные. */
  def select(x: Int, y: Int): Unit =
    cx = x
    cy = y
    trackedId = world.idAt(x, y)
    title = s"Геном клетки ($x, $y)"
    refresh()

  /** Обновить отображение (вызывается на каждом тике симуляции). */
  def refresh(): Unit =
    val w = world
    // Если следим за конкретной клеткой — обновляем её текущие координаты.
    if trackedId != 0 then
      w.findById(trackedId) match
        case Some((x, y)) =>
          cx = x
          cy = y
          title = s"Геном клетки ($x, $y)"
        case None =>
          trackedId = 0

    if cx < 0 || cy < 0 || cx >= w.W || cy >= w.H then
      header.text = "Клетка не выбрана"
      strip.update("", -1)
      explainView.update(None, 0)
      syncGenomeField("")
      setEditEnabled(false)
    else
      val k = w.cell(cx, cy)
      val kind =
        if k.isAlive then "живая"
        else if k.isCorpse then "труп"
        else "пусто"
      val diet = k.diet
      val total = diet.total
      def pct(v: Int): Int = if total > 0 then v * 100 / total else 0
      header.text =
        s"<html>($cx, $cy) — <b>$kind</b><br>" +
          s"энергия: ${k.energy} &nbsp; ресурс жизни: ${k.lifeLeft}<br>" +
          s"питание: свет ${pct(diet.light)}%, " +
          s"хищник ${pct(diet.prey)}%, падальщик ${pct(diet.corpse)}%</html>"

      val code = k.genome.code
      if k.isAlive && code.nonEmpty then
        val ptr = k.pointer % code.length
        val ex = w.explain(cx, cy)
        strip.update(code, ptr, GenomeView.jumpInfo(ex))
        explainView.update(Some(ex), k.energy)
      else
        strip.update("", -1)
        explainView.update(None, k.energy)
      // Геном можно править только у живой клетки (у трупов/пустых его нет).
      syncGenomeField(code)
      setEditEnabled(k.isAlive)
