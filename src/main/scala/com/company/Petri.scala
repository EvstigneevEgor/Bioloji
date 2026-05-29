package com.company

import scala.swing.Component
import scala.swing.event.{MousePressed, MouseReleased, MouseDragged, MouseWheelMoved}
import javax.swing.{JComponent, KeyStroke, AbstractAction}
import java.awt.{BasicStroke, Color, Cursor, Font, Graphics2D, GradientPaint, Dimension}
import java.awt.event.ActionEvent

object Petri:
  /** Размер клетки и отступ сетки в пикселях. */
  private val CellSize = 3
  private val Margin = 10
  /** Высота строки статуса под полем (две строки: статистика + легенда). */
  private val StatusBarHeight = 38

  /** Пределы и шаг масштабирования (зум колесом мыши). */
  private val MinZoom = 1.0
  private val MaxZoom = 30.0
  private val ZoomStep = 1.2

  /** Цвет трупа (мёртвая органика — пища падальщиков). */
  private val CorpseColor = new Color(120, 112, 104)
  /** Цвет «только что умершей» клетки (вспышка на один кадр). */
  private val DeadColor = new Color(25, 25, 25)

  // Базовые цвета рациона: смешиваются пропорционально съеденному.
  private val DietPrey = (225.0, 60.0, 60.0)   // хищник  -> красный
  private val DietLight = (70.0, 205.0, 70.0)  // фотосинтез -> зелёный
  private val DietCorpse = (80.0, 120.0, 245.0) // падальщик -> синий

  // Цвета фона-освещённости: тень/ночь (свет=0) -> солнце (свет=макс).
  // Приглушённые, чтобы клетки на их фоне были хорошо заметны.
  private val ShadeColor = new Color(24, 26, 38)
  private val SunColor = new Color(72, 68, 48)

  /** Линейная интерполяция между двумя цветами. */
  private def lerp(a: Color, b: Color, t: Double): Color =
    val tt = math.max(0.0, math.min(1.0, t))
    new Color(
      (a.getRed + (b.getRed - a.getRed) * tt).toInt,
      (a.getGreen + (b.getGreen - a.getGreen) * tt).toInt,
      (a.getBlue + (b.getBlue - a.getBlue) * tt).toInt
    )

/**
 * Компонент-отрисовщик «чашки Петри».
 * Логики симуляции здесь нет — шаги выполняет таймер во [[Window]]
 * (это устраняет нагрузку CPU 100% от repaint-цикла).
 */
class Petri extends Component:
  import Petri.*

  val widthWin: Int = 1100
  val heightWin: Int = 600

  var pole = new Pole(4, (widthWin - Margin) / CellSize, (heightWin - Margin) / CellSize)

  /** Сброс поля: создаёт новую симуляцию с нуля. */
  def resetPole(): Unit =
    pole = new Pole(4, (widthWin - Margin) / CellSize, (heightWin - Margin) / CellSize)
    selected = None

  preferredSize = new Dimension(widthWin, heightWin + StatusBarHeight)

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
  // Точка нажатия и признак перетаскивания — чтобы отличить «клик» (выбор
  // клетки) от «таскания» (панорамирование поля).
  private var pressX  = 0
  private var pressY  = 0
  private var dragged = false

  /**
   * Вызывается при выборе клетки кликом мыши; принимает координаты клетки
   * на поле. По умолчанию ничего не делает — обработчик задаёт [[Window]].
   */
  var onSelectCell: (Int, Int) => Unit = (_, _) => ()

  /** Текущая выбранная клетка (подсвечивается рамкой на поле). */
  private var selected: Option[(Int, Int)] = None

  /** Координаты клетки поля под точкой экрана (px, py) с учётом зума/сдвига. */
  def cellAt(px: Int, py: Int): Option[(Int, Int)] =
    val fx = (px - offsetX) / zoom
    val fy = (py - offsetY) / zoom
    val x = ((fx - Margin) / CellSize).toInt
    val y = ((fy - Margin) / CellSize).toInt
    if x >= 0 && x < pole.W && y >= 0 && y < pole.H then Some((x, y)) else None

  /** Не даёт «уехать» за край: поле всегда заполняет область просмотра. */
  private def clampOffsets(): Unit =
    val scaledW = zoom * widthWin
    val scaledH = zoom * heightWin
    offsetX = math.min(0.0, math.max(widthWin.toDouble - scaledW, offsetX))
    offsetY = math.min(0.0, math.max(heightWin.toDouble - scaledH, offsetY))

  /** Сброс к виду «всё поле целиком» (для кнопки, клавиши и двойного клика). */
  def resetView(): Unit =
    zoom = MinZoom
    offsetX = 0.0
    offsetY = 0.0
    repaint()

  /** Приблизить/отдалить к центру области просмотра (для кнопок и клавиш). */
  def zoomIn(): Unit  = zoomAt(widthWin / 2.0, heightWin / 2.0, ZoomStep)
  def zoomOut(): Unit = zoomAt(widthWin / 2.0, heightWin / 2.0, 1.0 / ZoomStep)

  /**
   * Масштабирование к точке экрана (mx, my): точка под курсором
   * остаётся на месте, изображение «наезжает» на неё.
   */
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
    // Колесо/тачпад — зум к курсору (вверх — приблизить, вниз — отдалить).
    // На трекпаде Mac прокрутка двумя пальцами шлёт частые события с дробным
    // шагом; целочисленный getWheelRotation их огрубляет и зум «дёргается».
    // Берём точное значение getPreciseWheelRotation и делаем масштаб
    // пропорциональным шагу — тогда тачпад работает плавно.
    case e: MouseWheelMoved =>
      val delta = e.peer match
        case w: java.awt.event.MouseWheelEvent => w.getPreciseWheelRotation
        case _                                 => e.rotation.toDouble
      if delta != 0.0 then
        val clamped = math.max(-4.0, math.min(4.0, delta))
        zoomAt(e.point.x.toDouble, e.point.y.toDouble, math.pow(ZoomStep, -clamped))

    // Нажатие — старт перетаскивания; двойной клик — сброс вида.
    case e: MousePressed =>
      if e.clicks >= 2 then resetView()
      lastDragX = e.point.x
      lastDragY = e.point.y
      pressX = e.point.x
      pressY = e.point.y
      dragged = false

    // Перетаскивание — панорамирование поля.
    case e: MouseDragged =>
      if math.abs(e.point.x - pressX) > 3 || math.abs(e.point.y - pressY) > 3 then
        dragged = true
      offsetX += e.point.x - lastDragX
      offsetY += e.point.y - lastDragY
      lastDragX = e.point.x
      lastDragY = e.point.y
      clampOffsets()
      repaint()

    // Отпускание без перетаскивания (одиночный клик) — выбор клетки.
    case e: MouseReleased =>
      if !dragged && e.clicks == 1 then
        cellAt(e.point.x, e.point.y).foreach { (x, y) =>
          selected = Some((x, y))
          onSelectCell(x, y)
          repaint()
        }
  }

  // ── горячие клавиши масштаба (работают, пока окно в фокусе) ───────
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

  /** Цвет среды по освещённости: тень снизу -> солнце сверху. */
  private def lightColor(light: Int): Color =
    lerp(ShadeColor, SunColor, light.toDouble / pole.maxLight)

  /** Цвет клетки: оттенок = чем питается, яркость = сколько энергии. */
  private def cellColor(energy: Int, diet: (Int, Int, Int)): Color =
    val (fl, fp, fc) = diet
    val total = fl + fp + fc
    // Базовый цвет — смесь рациона.
    val (br, bg, bb) =
      if total <= 0 then (190.0, 190.0, 190.0) // ещё не питалась
      else
        val wp = fp.toDouble / total
        val wl = fl.toDouble / total
        val wc = fc.toDouble / total
        (
          wp * DietPrey._1 + wl * DietLight._1 + wc * DietCorpse._1,
          wp * DietPrey._2 + wl * DietLight._2 + wc * DietCorpse._2,
          wp * DietPrey._3 + wl * DietLight._3 + wc * DietCorpse._3
        )
    // Яркость по энергии: голодная — тёмная, сытая — яркая (плавное насыщение).
    val frac = math.max(0, energy).toDouble / (math.max(0, energy) + 1500.0)
    val bright = 0.35 + 0.65 * frac
    new Color(
      math.min(255, (br * bright).toInt),
      math.min(255, (bg * bright).toInt),
      math.min(255, (bb * bright).toInt)
    )

  override protected def paintComponent(g: Graphics2D): Unit =
    super.paintComponent(g)

    val fieldW = pole.W * CellSize
    val fieldH = pole.H * CellSize

    popul = 0
    populB = 0
    populD = 0

    // Поле рисуем в отдельном контексте: ограничиваем область просмотра
    // (над строкой статуса) и применяем зум/сдвиг «камеры».
    val gf = g.create().asInstanceOf[Graphics2D]
    try
      gf.clipRect(0, 0, widthWin, heightWin)
      gf.translate(offsetX, offsetY)
      gf.scale(zoom, zoom)

      // Фон = питательность среды (освещённость): солнечно сверху, тень снизу;
      // зимой весь градиент тусклее, чем летом.
      gf.setPaint(
        new GradientPaint(
          0f, Margin.toFloat, lightColor(pole.lightAt(0)),
          0f, (Margin + fieldH).toFloat, lightColor(pole.lightAt(pole.H - 1))
        )
      )
      gf.fillRect(Margin, Margin, fieldW, fieldH)

      // Видимый прямоугольник в координатах поля — чтобы при увеличении
      // не перебирать отрисовку невидимых клеток.
      val visLeft   = -offsetX / zoom
      val visTop    = -offsetY / zoom
      val visRight  = (widthWin - offsetX) / zoom
      val visBottom = (heightWin - offsetY) / zoom

      var x = 0
      while x < pole.W do
        var y = 0
        while y < pole.H do
          val live = pole.getLive(x, y)
          val dead = !live && pole.getDead(x, y)
          if live then
            popul += 1
            if pole.isBern(x, y) then populB += 1
          else if dead then populD += 1

          val i = Margin + x * CellSize
          val j = Margin + y * CellSize
          if i + CellSize >= visLeft && i <= visRight &&
            j + CellSize >= visTop && j <= visBottom then
            if live then
              gf.setPaint(cellColor(pole.getEnergy(x, y), pole.getDiet(x, y)))
              gf.fillRect(i, j, CellSize, CellSize)
            else if dead then
              gf.setPaint(DeadColor)
              gf.fillRect(i, j, CellSize, CellSize)
            else if pole.isCorpse(x, y) then
              gf.setPaint(CorpseColor)
              gf.fillRect(i, j, CellSize, CellSize)
          y += 1
        x += 1

      // Рамка вокруг выбранной клетки — чтобы видеть, чей геном открыт.
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
    val season = if pole.leto then "лето" else "зима"
    g.drawString(
      s"популяция: $popul   время года: $season   " +
        s"родилось: $populB   умерло: $populD   масштаб: ${f"$zoom%.1f"}x",
      8,
      heightWin + 14
    )
    g.setFont(new Font("Arial", Font.PLAIN, 10))
    g.drawString(
      "клетки: зелёный — фотосинтез, красный — хищник, синий — падальщик, серый — труп; яркость = энергия.   " +
        "масштаб: колесо или +/−, сдвиг: перетаскивание, сброс: двойной клик / 0 / кнопка 1:1, клик по клетке — её геном.",
      8,
      heightWin + 30
    )
