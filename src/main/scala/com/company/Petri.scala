package com.company

import scala.swing.Component
import java.awt.{Color, Font, Graphics2D, GradientPaint, Dimension}

object Petri:
  /** Размер клетки и отступ сетки в пикселях. */
  private val CellSize = 3
  private val Margin = 10
  /** Высота строки статуса под полем (две строки: статистика + легенда). */
  private val StatusBarHeight = 38

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

  val pole = new Pole(4, (widthWin - Margin) / CellSize, (heightWin - Margin) / CellSize)

  preferredSize = new Dimension(widthWin, heightWin + StatusBarHeight)

  // Статистика последнего отрисованного кадра.
  private var popul = 0
  private var populB = 0
  private var populD = 0

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

    // Фон = питательность среды (освещённость): солнечно сверху, тень снизу;
    // зимой весь градиент тусклее, чем летом.
    g.setPaint(
      new GradientPaint(
        0f, Margin.toFloat, lightColor(pole.lightAt(0)),
        0f, (Margin + fieldH).toFloat, lightColor(pole.lightAt(pole.H - 1))
      )
    )
    g.fillRect(Margin, Margin, fieldW, fieldH)

    popul = 0
    populB = 0
    populD = 0

    var i = Margin
    while i < widthWin - CellSize do
      var j = Margin
      while j < heightWin - CellSize do
        val x = (i - Margin) / CellSize
        val y = (j - Margin) / CellSize
        if pole.getLive(x, y) then
          popul += 1
          if pole.isBern(x, y) then populB += 1
          g.setPaint(cellColor(pole.getEnergy(x, y), pole.getDiet(x, y)))
          g.fillRect(i, j, CellSize, CellSize)
        else if pole.getDead(x, y) then
          populD += 1
          g.setPaint(DeadColor)
          g.fillRect(i, j, CellSize, CellSize)
        else if pole.isCorpse(x, y) then
          g.setPaint(CorpseColor)
          g.fillRect(i, j, CellSize, CellSize)
        j += CellSize
      i += CellSize

    g.setPaint(Color.BLACK)
    g.setFont(new Font("Arial", Font.PLAIN, 11))
    val season = if pole.leto then "лето" else "зима"
    g.drawString(
      s"популяция: $popul   время года: $season   " +
        s"родилось: $populB   умерло: $populD",
      8,
      heightWin + 14
    )
    g.setFont(new Font("Arial", Font.PLAIN, 10))
    g.drawString(
      "фон — освещённость (солнечно сверху, тень снизу).   " +
        "клетки: зелёный — фотосинтез, красный — хищник, синий — падальщик, серый — труп.   " +
        "яркость = энергия.",
      8,
      heightWin + 30
    )
