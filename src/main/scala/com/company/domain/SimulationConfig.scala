package com.company.domain

/**
 * Иммутабельная конфигурация симуляции. Заменяет прежнюю россыпь
 * глобальных `var` в companion-объектах `Pole`/`Kletka`, которые правились
 * напрямую из UI. Теперь все настраиваемые параметры — поля одного значения,
 * которое целиком инжектируется в [[Field]]; «живое» изменение настроек —
 * это атомарная замена всего значения (см. `app.SettingsStore`).
 *
 * @param energyForDel        стоимость деления `e`
 * @param energyForStep       стоимость шага/атаки `s`/`a`
 * @param reproduceThreshold  порог энергии для принудительного деления
 * @param forcedReproduceCost цена принудительного деления
 * @param startEnergy         энергия стартовых клеток
 * @param summerLight         максимальное «солнце» летом
 * @param winterLight         максимальное «солнце» зимой
 * @param summerTicks         длительность лета в тиках
 * @param winterTicks         длительность зимы в тиках
 * @param gravityTicks        период смещения вниз «гравитацией» (0 — выкл.)
 * @param seed                seed ГСЧ (для воспроизводимых прогонов)
 * @param retribution         штраф атакующему при неудачной атаке
 * @param preyAdvantage       насколько добыча должна уступать по энергии
 * @param weakPreyBonus       бонус энергии за поедание живой добычи
 * @param corpseBonus         бонус энергии за поедание трупа
 * @param scavengeCost        стоимость команды падальщика `c` (дешевле атаки)
 * @param corpseDecayTicks    через сколько тиков труп исчезает (-1 — никогда)
 * @param kinshipThresholdPct порог различия геномов (%) для «родни»
 * @param maxGenLength        максимальная длина генома
 * @param mutationChance      шанс мутации «1 из N»
 * @param insertChance        внутри мутации: шанс вставки гена «1 из N»
 */
final case class SimulationConfig(
    energyForDel: Int,
    energyForStep: Int,
    reproduceThreshold: Int,
    forcedReproduceCost: Int,
    startEnergy: Int,
    summerLight: Int,
    winterLight: Int,
    summerTicks: Int,
    winterTicks: Int,
    gravityTicks: Int,
    seed: Long,
    retribution: Int,
    preyAdvantage: Int,
    weakPreyBonus: Int,
    corpseBonus: Int,
    scavengeCost: Int,
    corpseDecayTicks: Int,
    kinshipThresholdPct: Int,
    maxGenLength: Int,
    mutationChance: Int,
    insertChance: Int
)

object SimulationConfig:
  /** Исходные значения симуляции (как было в companion-объектах до рефакторинга). */
  val Default: SimulationConfig = SimulationConfig(
    energyForDel = 150,
    energyForStep = 250,
    reproduceThreshold = 150 * 50,
    forcedReproduceCost = 150 * 40,
    startEnergy = 150,
    summerLight = 100,
    winterLight = 25,
    summerTicks = 200,
    winterTicks = 200,
    gravityTicks = 400,
    seed = 42L,
    retribution = 20,
    preyAdvantage = 10,
    weakPreyBonus = 400,
    corpseBonus = 800,
    scavengeCost = 50,
    corpseDecayTicks = -1,
    kinshipThresholdPct = 20,
    maxGenLength = 100,
    mutationChance = 6,
    insertChance = 5
  )
