package com.company.domain

/**
 * Read-only представление мира для слоя отображения. UI работает с полем
 * только через этот трейт (чтение клеток, освещённости, расшифровки шага),
 * не имея доступа к мутаторам [[Field]].
 */
trait WorldView:
  def W: Int
  def H: Int
  def isSummer: Boolean

  /** Read-only проекция клетки в ячейке (x, y). */
  def cell(x: Int, y: Int): CellView

  /** Идентификатор клетки в ячейке (0 — пусто). */
  def idAt(x: Int, y: Int): Long

  /** Найти текущие координаты клетки по её идентификатору. */
  def findById(id: Long): Option[(Int, Int)]

  /** Освещённость (питательность среды) в строке h. */
  def lightAt(h: Int): Int

  /** Максимально возможная освещённость (летний потолок) — для нормировки. */
  def maxLight: Int

  /** Read-only расшифровка текущего шага клетки (x, y) — для GenomeWindow. */
  def explain(x: Int, y: Int): StepExplanation
