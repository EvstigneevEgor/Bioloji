package com.company.ui

/**
 * Презентация направлений 1..8: стрелки-глифы, русские названия и положение в
 * компасе 3×3. Это слой отображения доменного [[com.company.domain.Direction]]
 * (геометрия живёт в домене, внешний вид — здесь).
 */
object DirectionView:
  /** Стрелка для направления 1..8 (прочее — точка «стоять»). */
  def arrow(d: Int): String = d match
    case 1 => "\u2191" // ↑
    case 2 => "\u2197" // ↗
    case 3 => "\u2192" // →
    case 4 => "\u2198" // ↘
    case 5 => "\u2193" // ↓
    case 6 => "\u2199" // ↙
    case 7 => "\u2190" // ←
    case 8 => "\u2196" // ↖
    case _ => "\u00B7" // ·  (стоять)

  /** Стрелка по символу-цифре генома. */
  def arrowOf(c: Char): String = arrow(c - '0')

  def name(d: Int): String = d match
    case 1 => "вверх"
    case 2 => "вверх-вправо"
    case 3 => "вправо"
    case 4 => "вниз-вправо"
    case 5 => "вниз"
    case 6 => "вниз-влево"
    case 7 => "влево"
    case 8 => "вверх-влево"
    case _ => "стоять"

  /** Позиция направления в компасе 3×3: (колонка, строка), либо None (стоять). */
  def compassCell(d: Int): Option[(Int, Int)] = d match
    case 1 => Some((1, 0))
    case 2 => Some((2, 0))
    case 3 => Some((2, 1))
    case 4 => Some((2, 2))
    case 5 => Some((1, 2))
    case 6 => Some((0, 2))
    case 7 => Some((0, 1))
    case 8 => Some((0, 0))
    case _ => None
