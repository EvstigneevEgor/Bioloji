package com.company;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Petri extends JComponent {

    /*Метод, перерисовывающий элемент внутри окна
     *при обновлении*/
    Pole pole = new Pole(4, (400 - 10) / 10, (400 - 10) / 10);
    int widhtWin = 400, heightWin = 400;

    public void paintComponent(Graphics g) {
        super.paintComponents(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setPaint(Color.BLACK);


        for (int i = 10; i < widhtWin; i += 10) {
            for (int j = 10; j < heightWin; j += 10) {
                g2d.drawRect(i, j, 10, 10);
                if (pole.getLive((i - 10) / 10, (j - 10) / 10) > 0) {
                    g2d.setPaint(Color.green);
                    g2d.fillRect(i + 1, j + 1, 9, 9);
                    g2d.setPaint(Color.BLACK);
                }
            }
        }
        g2d.setPaint(Color.RED);
        super.repaint();
        g2d.dispose();
    }


}
