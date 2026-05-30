package com.company.domain

/**
 * Разобранная команда генома под указателем — единый результат декодинга,
 * который раньше дублировался в `Pole.processCell` и `Pole.explain`.
 * Проверка выполнимости (хватает ли энергии) остаётся в [[Field]], так как
 * требует конфигурации и состояния клетки.
 *
 *  - [[Photosynthesis]] — `f`;
 *  - [[Step]]/[[Divide]]/[[Attack]]/[[Scavenge]] — `s`/`e`/`a`/`c` с корректной
 *    цифрой-направлением;
 *  - [[Jump]] — «прыжок» указателя: цифра, прочий символ, либо s/e/a/c без
 *    валидной цифры-направления;
 *  - [[Idle]] — пустой геном (действия нет).
 */
enum Command:
  case Photosynthesis
  case Step(dir: Int)
  case Divide(dir: Int)
  case Attack(dir: Int)
  case Scavenge(dir: Int)
  case Jump(symbol: Char)
  case Idle

/** Тип действия гена под указателем — для наглядного окна генома. */
enum GenAction:
  case Photosynthesis, Step, Divide, Attack, Scavenge, Jump, Idle
