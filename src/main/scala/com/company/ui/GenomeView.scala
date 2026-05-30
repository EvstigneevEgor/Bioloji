package com.company.ui

import scala.swing.Component
import java.awt.{BasicStroke, Color, Dimension, Font, Graphics2D, Rectangle, RenderingHints}
import java.awt.geom.{Path2D, RoundRectangle2D}
import com.company.domain.{GenAction, StepExplanation}

/**
 * Наглядное представление генома в стиле Scratch: каждый ген — «пазл»-блок,
 * команды (f/s/e/a) — блоки с выемкой/выступом, цифры-направления — овальные
 * «значения» со стрелками. Текущий ген (под указателем) подсвечивается.
 */
object GenomeView:
  // ── геометрия стека блоков ──────────────────────────────
  val ContentW = 360
  val BlockX   = 40            // левый отступ блока (слева — гаттер с ▶ и №)
  val BlockW   = ContentW - BlockX - 14
  val BlockH   = 34
  val TabH     = 6             // глубина выемки/выступа пазла
  val Stride   = BlockH        // шаг по вертикали (выступ входит в следующий блок)
  val PadTop   = 12
  val PadBottom = TabH + 12

  // ── палитра (согласована с окраской поля в FieldColors) ─
  val ColPhoto = new Color(90, 190, 90)    // f — фотосинтез (зелёный)
  val ColStep  = new Color(80, 150, 235)   // s — шаг (синий)
  val ColDiv   = new Color(170, 110, 215)  // e — деление (фиолетовый)
  val ColAtk   = new Color(225, 95, 95)    // a — атака (красный)
  val ColScav  = new Color(80, 120, 245)   // c — падальщик (синий, как DietCorpse)
  val ColDir   = new Color(70, 175, 175)   // 1..8 — направление (бирюзовый)
  val ColOther = new Color(150, 150, 150)  // прочее

  val PanelBg      = new Color(245, 246, 248)
  val SlotBg       = new Color(0, 0, 0, 60)
  val Pointer      = new Color(255, 200, 0)
  val HiCell       = new Color(255, 170, 40)
  val JumpArc      = new Color(210, 70, 90)   // дуга «прыжка» указателя (jump)
  val GridCell     = new Color(225, 228, 234)
  val SelfCell     = new Color(120, 124, 134)
  val TextDark     = new Color(40, 42, 50)
  val TextWarn     = new Color(200, 90, 40)
  val TextMuted    = new Color(110, 114, 124)

  val IdxFont   = new Font("SansSerif", Font.BOLD, 11)
  val GlyphFont = new Font("SansSerif", Font.BOLD, 18)
  val LabelFont = new Font("SansSerif", Font.BOLD, 13)
  val SmallFont = new Font("SansSerif", Font.PLAIN, 12)
  val SlotFont  = new Font("SansSerif", Font.BOLD, 14)

  case class Spec(color: Color, glyph: String, label: String, isCommand: Boolean)

  /** Описание блока по символу генома. Глиф — сам символ гена. */
  def specFor(c: Char): Spec = c match
    case 'f' => Spec(ColPhoto, "f", "Фотосинтез", true)
    case 's' => Spec(ColStep,  "s", "Шаг", true)
    case 'e' => Spec(ColDiv,   "e", "Деление", true)
    case 'a' => Spec(ColAtk,   "a", "Атака", true)
    case 'c' => Spec(ColScav,  "c", "Падальщик", true)
    case d if d >= '1' && d <= '8' =>
      Spec(ColDir, DirectionView.arrowOf(d), s"$d — ${DirectionView.name(d - '0')}", false)
    case _ => Spec(ColOther, c.toString, "—", false)

  /**
   * Если в этот тик случится «прыжок» указателя (jump) — вернуть цель прыжка
   * и краткое условие, по которому он происходит. None — команда выполняется
   * обычным образом, перехода по геному нет.
   */
  def jumpInfo(e: StepExplanation): Option[(Int, String)] =
    e.jumpTo.map { target =>
      val cond = e.action match
        case GenAction.Step | GenAction.Divide => "не хватает энергии"
        case GenAction.Jump =>
          if "seac".contains(e.symbol) then "нет цифры-направления"
          else "ген — не команда"
        case _ => "переход"
      (target, cond)
    }

/**
 * Вертикальный стек блоков генома (помещается в ScrollPane).
 */
class GenomeStrip extends Component:
  import GenomeView.*

  private var gen = ""
  private var pointer = -1
  // Если в этот тик будет «прыжок» указателя — цель и краткое условие.
  private var jump: Option[(Int, String)] = None

  preferredSize = new Dimension(ContentW, 200)

  /** Обновить геном и позицию указателя; прокрутить указатель в зону видимости. */
  def update(g: String, ptr: Int, jmp: Option[(Int, String)] = None): Unit =
    gen = g
    pointer = ptr
    jump = jmp
    val h = math.max(160, PadTop + g.length * Stride + PadBottom)
    preferredSize = new Dimension(ContentW, h)
    peer.revalidate()
    repaint()
    if ptr >= 0 && ptr < g.length then
      val y = PadTop + ptr * Stride
      peer.scrollRectToVisible(new Rectangle(0, y - 24, ContentW, BlockH + 56))

  override def paintComponent(g2: Graphics2D): Unit =
    super.paintComponent(g2)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(PanelBg)
    g2.fillRect(0, 0, size.width, size.height)

    if gen.isEmpty then
      g2.setColor(TextMuted)
      g2.setFont(LabelFont)
      g2.drawString("(нет генома)", 16, 30)
      return

    var i = 0
    while i < gen.length do
      drawBlock(g2, i)
      i += 1

    // Поверх блоков — дуга «прыжка» указателя (если он будет в этот тик).
    jump.foreach { (target, cond) =>
      if pointer >= 0 && pointer < gen.length && target >= 0 && target < gen.length then
        drawJump(g2, pointer, target, cond)
    }

  /**
   * Изогнутая стрелка-«прыжок» от блока under-pointer к целевому блоку.
   * Рисуется в левом гаттере; рядом — короткая подпись условия перехода.
   */
  private def drawJump(g2: Graphics2D, from: Int, to: Int, label: String): Unit =
    val fromY = PadTop + from * Stride + BlockH / 2.0
    val toY   = PadTop + to * Stride + BlockH / 2.0
    val sx    = BlockX - 4.0
    val leftX = 8.0

    val path = new Path2D.Double
    path.moveTo(sx, fromY)
    path.curveTo(leftX, fromY, leftX, toY, sx, toY)
    g2.setColor(JumpArc)
    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
    g2.draw(path)

    // точка у источника
    g2.fill(new java.awt.geom.Ellipse2D.Double(sx - 3, fromY - 3, 6, 6))

    // стрелка у цели — указывает вправо, «в» блок
    val head = new Path2D.Double
    head.moveTo(sx + 4, toY)
    head.lineTo(sx - 5, toY - 6)
    head.lineTo(sx - 5, toY + 6)
    head.closePath()
    g2.fill(head)

    // подпись условия прыжка (пилюля у середины дуги)
    val midY = ((fromY + toY) / 2.0).toInt
    g2.setFont(SmallFont)
    val fm = g2.getFontMetrics
    val tw = fm.stringWidth(label)
    val px = 4
    val py = midY - 9
    g2.setColor(new Color(255, 255, 255, 232))
    g2.fillRoundRect(px, py, tw + 10, 18, 9, 9)
    g2.setColor(JumpArc)
    g2.drawRoundRect(px, py, tw + 10, 18, 9, 9)
    g2.setColor(TextDark)
    g2.drawString(label, px + 5, py + 13)

  private def drawBlock(g2: Graphics2D, i: Int): Unit =
    val sym = gen.charAt(i)
    val spec = specFor(sym)
    val x = BlockX.toDouble
    val y = (PadTop + i * Stride).toDouble
    val w = BlockW.toDouble
    val h = BlockH.toDouble

    val shape: java.awt.Shape =
      if spec.isCommand then puzzlePath(x, y, w, h)
      else new RoundRectangle2D.Double(x + 8, y + 3, w - 16, h - 6, 16, 16)

    g2.setColor(spec.color)
    g2.fill(shape)

    val isPtr = i == pointer
    if isPtr then
      g2.setColor(Pointer)
      g2.setStroke(new BasicStroke(3f))
      g2.draw(shape)
      // маркер ▶ слева
      val mp = new Path2D.Double
      val my = y + h / 2
      mp.moveTo(x - 22, my - 7); mp.lineTo(x - 22, my + 7); mp.lineTo(x - 9, my)
      mp.closePath()
      g2.setColor(Pointer)
      g2.fill(mp)
    else
      g2.setColor(spec.color.darker)
      g2.setStroke(new BasicStroke(1f))
      g2.draw(shape)

    // номер гена
    g2.setColor(new Color(255, 255, 255, 200))
    g2.setFont(IdxFont)
    g2.drawString(i.toString, BlockX.toInt + 8, (y + h - 9).toInt)

    // глиф
    g2.setColor(Color.WHITE)
    g2.setFont(GlyphFont)
    g2.drawString(spec.glyph, BlockX.toInt + 24, (y + h - 9).toInt)

    // подпись
    g2.setFont(LabelFont)
    g2.drawString(spec.label, BlockX.toInt + 50, (y + h - 11).toInt)

    // у команд s/e/a/c — слот-аргумент: ген, из которого берётся направление
    if "seac".contains(sym) && i + 1 < gen.length then
      val argCh = gen.charAt(i + 1)
      val sw = 40
      val sx = (x + w - sw - 8).toInt
      val sy = (y + 6).toInt
      g2.setColor(SlotBg)
      g2.fillRoundRect(sx, sy, sw, BlockH - 12, 12, 12)
      g2.setColor(Color.WHITE)
      g2.setFont(SlotFont)
      val txt = if argCh >= '1' && argCh <= '8' then DirectionView.arrowOf(argCh) else argCh.toString
      g2.drawString(txt, sx + 14, sy + BlockH - 18)

  /** Путь «пазла»: сверху вогнутая выемка, снизу выпуклый выступ. */
  private def puzzlePath(x: Double, y: Double, w: Double, h: Double): Path2D =
    val r = 8.0
    val tabW = 18.0
    val tabX = x + 26
    val p = new Path2D.Double
    p.moveTo(x + r, y)
    p.lineTo(tabX, y)
    p.lineTo(tabX + 3, y + TabH)
    p.lineTo(tabX + tabW - 3, y + TabH)
    p.lineTo(tabX + tabW, y)
    p.lineTo(x + w - r, y)
    p.quadTo(x + w, y, x + w, y + r)
    p.lineTo(x + w, y + h - r)
    p.quadTo(x + w, y + h, x + w - r, y + h)
    p.lineTo(tabX + tabW, y + h)
    p.lineTo(tabX + tabW - 3, y + h + TabH)
    p.lineTo(tabX + 3, y + h + TabH)
    p.lineTo(tabX, y + h)
    p.lineTo(x + r, y + h)
    p.quadTo(x, y + h, x, y + h - r)
    p.lineTo(x, y + r)
    p.quadTo(x, y, x + r, y)
    p.closePath()
    p

/**
 * Панель «Почему»: текст-объяснение текущего шага + компас направления.
 */
class ExplainView extends Component:
  import GenomeView.*

  private var exp: Option[StepExplanation] = None
  private var energy = 0

  preferredSize = new Dimension(ContentW, 150)

  def update(e: Option[StepExplanation], en: Int): Unit =
    exp = e
    energy = en
    repaint()

  override def paintComponent(g2: Graphics2D): Unit =
    super.paintComponent(g2)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(PanelBg)
    g2.fillRect(0, 0, size.width, size.height)
    g2.setColor(new Color(220, 222, 228))
    g2.drawLine(8, 2, size.width - 8, 2)

    exp match
      case None =>
        g2.setColor(TextMuted)
        g2.setFont(LabelFont)
        g2.drawString("Клетка не выбрана", 12, 26)
      case Some(e) =>
        drawText(g2, e)
        drawCompass(g2, e)

  private def drawText(g2: Graphics2D, e: StepExplanation): Unit =
    var y = 22
    val x = 12
    e.preNote.foreach { n =>
      g2.setColor(TextWarn)
      g2.setFont(LabelFont)
      g2.drawString("\u26A0 " + n, x, y)
      y += 22
    }
    for (line, col, bold) <- lines(e) do
      g2.setColor(col)
      g2.setFont(if bold then LabelFont else SmallFont)
      g2.drawString(line, x, y)
      y += 20

  private def lines(e: StepExplanation): Seq[(String, Color, Boolean)] =
    val dir = e.direction.map(d => s"${DirectionView.arrow(d)} ${DirectionView.name(d)}").getOrElse("")
    e.action match
      case GenAction.Photosynthesis =>
        Seq(
          ("Фотосинтез (f)", ColPhoto, true),
          (s"+${e.energyGain} энергии от света", TextDark, false),
          (s"указатель → блок ${e.nextPointer}", TextMuted, false)
        )
      case GenAction.Step =>
        val head = (s"Шаг (s) — $dir", ColStep, true)
        if !e.feasible then
          Seq(head,
            (s"мало энергии: $energy < ${e.energyCost} \u2717", TextWarn, false),
            (s"прыжок указателя → блок ${e.jumpTo.getOrElse(e.nextPointer)}", TextMuted, false))
        else if e.targetReady.contains(false) then
          Seq(head, ("сосед занят — стоит на месте", TextMuted, false),
            (s"энергия не тратится, указатель → блок ${e.nextPointer}", TextMuted, false))
        else
          Seq(head, (s"энергия $energy \u2265 ${e.energyCost} \u2713 — переезд", TextDark, false),
            (s"\u2212${e.energyCost} энергии, указатель → блок ${e.nextPointer}", TextMuted, false))
      case GenAction.Divide =>
        val head = (s"Деление (e) — $dir", ColDiv, true)
        if !e.feasible then
          Seq(head, (s"мало энергии: $energy < ${e.energyCost} \u2717", TextWarn, false),
            (s"прыжок указателя → блок ${e.jumpTo.getOrElse(e.nextPointer)}", TextMuted, false))
        else if e.targetReady.contains(false) then
          Seq(head, ("сосед занят — потомка некуда поместить", TextMuted, false),
            (s"указатель → блок ${e.nextPointer}", TextMuted, false))
        else
          Seq(head, (s"энергия $energy \u2265 ${e.energyCost} \u2713 — рождается потомок", TextDark, false),
            (s"\u2212${e.energyCost} энергии, указатель → блок ${e.nextPointer}", TextMuted, false))
      case GenAction.Attack =>
        val head = (s"Атака (a) — $dir", ColAtk, true)
        if e.targetReady.contains(true) then
          Seq(head, (s"есть цель — атакует (\u2212${e.energyCost})", TextDark, false),
            (s"указатель → блок ${e.nextPointer} (\u2248)", TextMuted, false))
        else
          Seq(head, ("рядом никого — промах", TextMuted, false),
            (s"указатель → блок ${e.nextPointer} (\u2248)", TextMuted, false))
      case GenAction.Scavenge =>
        val head = (s"Падальщик (c) — ищет труп (предпочёт $dir)", ColScav, true)
        if e.targetReady.contains(true) then
          Seq(head, (s"рядом есть труп — съедает (\u2212${e.energyCost})", TextDark, false),
            (s"указатель → блок ${e.nextPointer} (\u2248)", TextMuted, false))
        else
          Seq(head, ("трупов рядом нет — промах без затрат", TextMuted, false),
            (s"указатель → блок ${e.nextPointer} (\u2248)", TextMuted, false))
      case GenAction.Jump =>
        Seq(("Цифра/прыжок", ColDir, true),
          (s"указатель → блок ${e.jumpTo.getOrElse(e.nextPointer)}", TextMuted, false))
      case GenAction.Idle =>
        Seq(("нет активной команды", TextMuted, true))

  /** Компас 3×3: клетка в центре, выбранное направление подсвечено. */
  private def drawCompass(g2: Graphics2D, e: StepExplanation): Unit =
    val cs = 26
    val ox = size.width - 3 * cs - 16
    val oy = 30
    val hi = e.direction.flatMap(DirectionView.compassCell)
    for row <- 0 until 3; col <- 0 until 3 do
      val cx = ox + col * cs
      val cy = oy + row * cs
      val center = row == 1 && col == 1
      val isHi = hi.contains((col, row))
      g2.setColor(if isHi then HiCell else if center then SelfCell else GridCell)
      g2.fillRoundRect(cx + 2, cy + 2, cs - 4, cs - 4, 6, 6)
      if center then
        g2.setColor(Color.WHITE)
        g2.setFont(IdxFont)
        g2.drawString("•", cx + cs / 2 - 2, cy + cs / 2 + 4)
      else if isHi then
        g2.setColor(Color.WHITE)
        g2.setFont(GlyphFont)
        e.direction.foreach(d => g2.drawString(DirectionView.arrow(d), cx + 5, cy + cs - 7))
    g2.setColor(TextMuted)
    g2.setFont(new Font("SansSerif", Font.PLAIN, 10))
    g2.drawString("куда", ox + 2, oy - 6)
