package com.company.ui

import scala.swing.{SimpleSwingApplication, Frame}

/**
 * Точка входа: открывает главное окно. Конфигурация и ГСЧ создаются внутри
 * движка ([[com.company.app.SimulationEngine]]) — глобального состояния больше нет.
 */
object Main extends SimpleSwingApplication:
  def top: Frame = new MainWindow
