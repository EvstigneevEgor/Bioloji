package com.company.app

import java.util.concurrent.atomic.AtomicReference
import com.company.domain.SimulationConfig

/**
 * Хранилище текущей конфигурации симуляции. Держит иммутабельное значение
 * [[SimulationConfig]] в атомарной ссылке: UI читает его и обновляет
 * функциональным преобразованием, а движок забирает актуальный снимок.
 *
 * Это заменяет прежнее прямое изменение глобальных `var` из окна настроек:
 * теперь «настройка» — атомарная замена целого значения.
 */
final class SettingsStore(initial: SimulationConfig = SimulationConfig.Default):
  private val ref = new AtomicReference[SimulationConfig](initial)

  /** Текущая конфигурация. */
  def current: SimulationConfig = ref.get()

  /** Атомарно обновить конфигурацию преобразованием. */
  def update(f: SimulationConfig => SimulationConfig): Unit =
    ref.updateAndGet(c => f(c))
    ()

  /** Сбросить к значениям по умолчанию. */
  def reset(): Unit = ref.set(SimulationConfig.Default)
