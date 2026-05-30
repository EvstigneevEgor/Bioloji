package com.company.domain

/**
 * Read-only «расшифровка» текущего шага клетки: что за команда стоит под
 * указателем генома и что она сделает в этот тик. Зеркалит логику [[Field]],
 * но НИЧЕГО не меняет в состоянии — read-модель для UI (GenomeWindow).
 *
 *  - `pointer`     — индекс текущего гена (`cont % len`);
 *  - `direction`   — НАСТОЯЩЕЕ направление 1..8 для s/e/a;
 *  - `targetReady` — выполнимо ли действие по соседу (s/e: есть свободная
 *                    ячейка; a: есть кого атаковать);
 *  - `feasible`    — хватает ли энергии на команду;
 *  - `energyCost`  — стоимость команды; `energyGain` — доход (фотосинтез);
 *  - `nextPointer` — куда уйдёт указатель (для боя — приблизительно);
 *  - `jumpTo`      — цель «прыжка» bPerehod;
 *  - `preNote`     — предупреждение до выполнения команды (смерть/перенаселение).
 */
final case class StepExplanation(
    pointer: Int,
    symbol: Char,
    action: GenAction,
    direction: Option[Int],
    targetReady: Option[Boolean],
    feasible: Boolean,
    energyCost: Int,
    energyGain: Int,
    nextPointer: Int,
    jumpTo: Option[Int],
    preNote: Option[String]
)

object StepExplanation:
  /** Расшифровка «делать нечего» (пустая/мёртвая клетка). */
  val Idle: StepExplanation =
    StepExplanation(0, ' ', GenAction.Idle, None, None, false, 0, 0, 0, None, None)
