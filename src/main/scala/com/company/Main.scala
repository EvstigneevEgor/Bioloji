package com.company

import scala.swing.{SimpleSwingApplication, Frame}

object Main extends SimpleSwingApplication:
  // Засеваем ГСЧ до создания поля — детерминированный режим воспроизводим
  // от запуска к запуску при одинаковом Pole.Seed.
  Rng.seed(Pole.Seed)
  def top: Frame = new Window
