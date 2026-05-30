package com.company.ui

import scala.swing.Component
import scala.swing.event.{MousePressed, MouseReleased, MouseDragged, MouseWheelMoved}
import javax.swing.{JComponent, KeyStroke, AbstractAction}
import java.awt.{BasicStroke, Color, Cursor, Font, Graphics2D, GradientPaint, Dimension}
import java.awt.event.ActionEvent
import com.company.app.SimulationEngine

object FieldView:
  /** Размер поля в пикселях. */
  val WidthWin: Int = 1100
  val HeightWin: Int = 600

  /** Размер клетки и отступ сетки в пикселях. */
  private val CellSize = 3
  private val Margin = 10
  /** Высота строки статуса под полем (две строки: статистика + легенда). */
  private val StatusBarHeight = 38

  /** Пределы и шаг масштабирования (зум колесом мыши). */
  private val MinZoom = 1.0
  private val MaxZoom = 30.0
  private val ZoomStep = 1.2

  /** Число столбцов/строк сетки поля, выводимое из пиксельных размеров. */
  def cols: Int = (WidthWin - Margin) / CellSize
  def rows: Int = (HeightWin - Margin) / CellSize

/**
 * Компонент-отрисовщик «чашки Петри». Логики симуляции здесь нет — шаги делает
 * движок ([[SimulationEngine]]); компонент только читает мир через read-only
 * [[com.company.domain.WorldView]] и рисует его.
 */
class FieldView(engine: SimulationEngine) extends Component:
  import FieldView.*

  private def world = engine.world

  /** Сброс поля: создаёт новую симуляцию с нуля. */
  def resetPole(): Unit =
    engine.reset()
    selected = None
    selectedId = 0

  preferredSize = new Dimension(WidthWin, HeightWin + StatusBarHeight)

  // Статистика последнего отрисованного кадра.
  private var popul = 0
  private var populB = 0
  private var populD = 0

  // ── «камера»: масштаб и сдвиг для зума/панорамирования мышью ──────
  private var zoom      = MinZoom
  private var offsetX   = 0.0
  private var offsetY   = 0.0
  private var lastDragX = 0
  private var lastDragY = 0
  private var pressX  = 0
  private var pressY  = 0
  private var dragged = false

  /** Вызывается при выборе клетки кликом мыши; обработчик задаёт [[MainWindow]]. */
  var onSelectCell: (Int, Int) => Unit = (_, _) => ()

  /** Текущая выбранная клетка (подсвечивается рамкой на поле). */
  private var selected: Option[(Int, Int)] = None
  /** Идентификатор выбранной клетки (0 — выбрана пустая ячейка). */
  private var selectedId: Long = 0

  /** Координаты клетки поля под точкой экрана (px, py) с учётом зума/сдвига. */
  def cellAt(px: Int, py: Int): Option[(Int, Int)] =
    val fx = (px - offsetX) / zoom
    val fy = (py - offsetY) / zoom
    val x = ((fx - Margin) / CellSize).toInt
    val y = ((fy - Margin) / CellSize).toInt
    if x >= 0 && x < world.W && y >= 0 && y < world.H then Some((x, y)) else None

  /** Не даёт «уехать» за край: поле всегда заполняет область просмотра. */
  private def clampOffsets(): Unit =
    val scaledW = zoom * WidthWin
    val scaledH = zoom * HeightWin
    offsetX = math.min(0.0, math.max(WidthWin.toDouble - scaledW, offsetX))
    offsetY = math.min(0.0, math.max(HeightWin.toDouble - scaledH, offsetY))

  /** Сброс к виду «всё поле целиком». */
  def resetView(): Unit =
    zoom = MinZoom
    offsetX = 0.0
    offsetY = 0.0
    repaint()

  def zoomIn(): Unit  = zoomAt(WidthWin / 2.0, HeightWin / 2.0, ZoomStep)
  def zoomOut(): Unit = zoomAt(WidthWin / 2.0, HeightWin / 2.0, 1.0 / ZoomStep)

  /** Масштабирование к точке экрана: точка под курсором остаётся на месте. */
  private def zoomAt(mx: Double, my: Double, factor: Double): Unit =
    val newZoom = math.max(MinZoom, math.min(MaxZoom, zoom * factor))
    if newZoom != zoom then
      offsetX = mx - (mx - offsetX) * (newZoom / zoom)
      offsetY = my - (my - offsetY) * (newZoom / zoom)
      zoom = newZoom
      clampOffsets()
      repaint()

  cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)

  listenTo(mouse.clicks, mouse.moves, mouse.wheel)
  reactions += {
    case e: MouseWheelMoved =>
      val delta = e.peer match
        case w: java.awt.event.MouseWheelEvent => w.getPreciseWheelRotation
        case _                                 => e.rotation.toDouble
      if delta != 0.0 then
        val clamped = math.max(-4.0, math.min(4.0, delta))
        zoomAt(e.point.x.toDouble, e.point.y.toDouble, math.pow(ZoomStep, -clamped))

    case e: MousePressed =>
      if e.clicks >= 2 then resetView()
      lastDragX = e.point.x
      lastDragY = e.point.y
      pressX = e.point.x
      pressY = e.point.y
      dragged = false

    case e: MouseDragged =>
      if math.abs(e.point.x - pressX) > 3 || math.abs(e.point.y - pressY) > 3 then
        dragged = true
      offsetX += e.point.x - lastDragX
      offsetY += e.point.y - lastDragY
      lastDragX = e.point.x
      lastDragY = e.point.y
      clampOffsets()
      repaint()

    case e: MouseReleased =>
      if !dragged && e.clicks == 1 then
        cellAt(e.point.x, e.point.y).foreach { (x, y) =>
          selected = Some((x, y))
          selectedId = world.idAt(x, y)
          onSelectCell(x, y)
          repaint()
        }
  }

  // ── горячие клавиши масштаба ───────
  private def bindKeys(name: String, strokes: Seq[String])(action: => Unit): Unit =
    val im = peer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val am = peer.getActionMap
    for ks <- strokes do im.put(KeyStroke.getKeyStroke(ks), name)
    am.put(name, new AbstractAction {
      def actionPerformed(e: ActionEvent): Unit = action
    })

  bindKeys("zoom-in", Seq("EQUALS", "shift EQUALS", "PLUS", "ADD"))(zoomIn())
  bindKeys("zoom-out", Seq("MINUS", "SUBTRACT"))(zoomOut())
  bindKeys("zoom-reset", Seq("0", "NUMPAD0"))(resetView())

  override protected def paintComponent(g: Graphics2D): Unit =
    super.paintComponent(g)
    val w = world

    val fieldW = w.W * CellSize
    val fieldH = w.H * CellSize

    popul = 0
    populB = 0
    populD = 0

    val gf = g.create().asInstanceOf[Graphics2D]
    try
      gf.clipRect(0, 0, WidthWin, HeightWin)
      gf.translate(offsetX, offsetY)
      gf.scale(zoom, zoom)

      // Фон = питательность среды (освещённость): солнечно сверху, тень снизу.
      gf.setPaint(
        new GradientPaint(
          0f, Margin.toFloat, FieldColors.light(w.lightAt(0), w.maxLight),
          0f, (Margin + fieldH).toFloat, FieldColors.light(w.lightAt(w.H - 1), w.maxLight)
        )
      )
      gf.fillRect(Margin, Margin, fieldW, fieldH)

      // Видимый прямоугольник в координатах поля.
      val visLeft   = -offsetX / zoom
      val visTop    = -offsetY / zoom
      val visRight  = (WidthWin - offsetX) / zoom
      val visBottom = (HeightWin - offsetY) / zoom

      var x = 0
      while x < w.W do
        var y = 0
        while y < w.H do
          val c = w.cell(x, y)
          val live = c.isAlive
          val dead = !live && c.diedThisTick
          if live then
            popul += 1
            if c.bornThisTick then populB += 1
          else if dead then populD += 1

          val i = Margin + x * CellSize
          val j = Margin + y * CellSize
          if i + CellSize >= visLeft && i <= visRight &&
            j + CellSize >= visTop && j <= visBottom then
            if live then
              gf.setPaint(FieldColors.cell(c.energy, c.diet))
              gf.fillRect(i, j, CellSize, CellSize)
            else if dead then
              gf.setPaint(FieldColors.Dead)
              gf.fillRect(i, j, CellSize, CellSize)
            else if c.isCorpse then
              gf.setPaint(FieldColors.Corpse)
              gf.fillRect(i, j, CellSize, CellSize)
          y += 1
        x += 1

      // Следим за конкретной клеткой — рамка переезжает вслед за ней.
      if selectedId != 0 then
        w.findById(selectedId) match
          case Some(pos) => selected = Some(pos)
          case None =>
            selected = None
            selectedId = 0

      selected.foreach { (sx, sy) =>
        val i = Margin + sx * CellSize
        val j = Margin + sy * CellSize
        val pad = 1
        gf.setStroke(new BasicStroke((2.0 / zoom).toFloat))
        gf.setPaint(Color.WHITE)
        gf.drawRect(i - pad, j - pad, CellSize + 2 * pad, CellSize + 2 * pad)
      }
    finally gf.dispose()

    g.setPaint(Color.BLACK)
    g.setFont(new Font("Arial", Font.PLAIN, 11))
    val season = if w.isSummer then "лето" else "зима"
    g.drawString(
      s"популяция: $popul   время года: $season   " +
        s"родилось: $populB   умерло: $populD   масштаб: ${f"$zoom%.1f"}x",
      8,
      HeightWin + 14
    )
    g.setFont(new Font("Arial", Font.PLAIN, 10))
    g.drawString(
      "клетки: зелёный — фотосинтез, красный — хищник, синий — падальщик, серый — труп; яркость = энергия.   " +
        "масштаб: колесо или +/−, сдвиг: перетаскивание, сброс: двойной клик / 0 / кнопка 1:1, клик по клетке — её геном.",
      8,
      HeightWin + 30
    )
