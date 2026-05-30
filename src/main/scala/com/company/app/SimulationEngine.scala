package com.company.app

import com.company.domain.{Field, Rng, SeededRng, WorldView}

/**
 * Оркестрация симуляции: владеет полем [[Field]] и ГСЧ, читает актуальную
 * конфигурацию из [[SettingsStore]]. Инкапсулирует то, что раньше было
 * размазано между окном (`oneStep`) и компонентом отрисовки (`resetPole`).
 *
 * Слой отображения работает с миром только через read-only [[WorldView]].
 * Класс не потокобезопасен сам по себе — внешняя синхронизация на стороне UI.
 *
 * @param startCells число стартовых клеток
 * @param width      ширина поля в ячейках
 * @param height     высота поля в ячейках
 * @param settings   источник текущей конфигурации
 */
final class SimulationEngine(startCells: Int, width: Int, height: Int, settings: SettingsStore):
  private var rng: Rng = new SeededRng(settings.current.seed)
  private var field: Field = newField()
  private var ticks: Long = 0

  private def newField(): Field =
    new Field(startCells, width, height, settings.current, rng)

  /** Текущий мир для чтения слоем отображения. */
  def world: WorldView = field

  /** Число выполненных тиков с последнего сброса. */
  def tickCount: Long = ticks

  /** Один шаг симуляции: применить актуальный конфиг, шагнуть, продвинуть таймеры. */
  def step(): Unit =
    field.config = settings.current
    field.tickFast()
    ticks += 1
    field.advanceSeason()
    field.advanceGravity()

  /**
   * Заменить геном живой клетки в ячейке (x, y) — для ручной правки из UI.
   * Не потокобезопасен: вызывать под той же синхронизацией, что и [[step]].
   *
   * @return true, если клетка была живой и геном применён.
   */
  def setGenome(x: Int, y: Int, code: String): Boolean =
    field.setGenomeAt(x, y, code)

  /** Полный сброс: новый ГСЧ по seed и новое поле с актуальной конфигурацией. */
  def reset(): Unit =
    rng = new SeededRng(settings.current.seed)
    field = newField()
    ticks = 0
