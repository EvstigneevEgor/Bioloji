package com.company;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Petri extends JComponent {

    /*Метод, перерисовывающий элемент внутри окна
     *при обновлении*/
    int widhtWin = 800, heightWin = 600;
    Pole pole = new Pole(8, (widhtWin - 10) / 10, (heightWin - 10) / 10);

    long endTime = System.currentTimeMillis() + 3000;
    boolean p=false;
    public void paintComponent(Graphics g) {
        super.paintComponents(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setPaint(Color.BLACK);


        for (int i = 10; i < widhtWin; i += 10) {
            for (int j = 10; j < heightWin; j += 10) {
                g2d.drawRect(i, j, 10, 10);
                if (pole.getLive((i - 10) / 10, (j - 10) / 10) > 0) {
                    g2d.setPaint(Color.RED);
                    g2d.fillRect(i + 1, j + 1, 9, 9);
                    g2d.setPaint(Color.BLACK);
                }
            }
        }
        // задержка в 3 секунды
        if (System.currentTimeMillis() > endTime) {
            endTime = System.currentTimeMillis() + 10;
            pole.itr();
            if(p) {
                pole.itrobn();
                p=false;
            }else
                p=true;
        }

        super.repaint();
        g2d.dispose();
    }


}
