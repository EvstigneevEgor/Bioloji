package com.company;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Petri extends JComponent {

    /*Метод, перерисовывающий элемент внутри окна
     *при обновлении*/
    int widhtWin = 1100, heightWin = 600;
    Pole pole = new Pole(2, (widhtWin - 10) / 3, (heightWin - 10) / 3);

    long endTime = System.currentTimeMillis() + 8000;
    long endTimedead = System.currentTimeMillis() + 8100;

    public void paintComponent(Graphics g) {
        super.paintComponents(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setPaint(Color.BLACK);


        for (int i = 10; i < widhtWin-3; i += 3) {
            for (int j = 10; j < heightWin-3; j += 3) {
                //g2d.drawRect(i, j, 3, 3);
                if (pole.getLive((i - 10) / 3, (j - 10) / 3) > 0) {
                    g2d.setPaint(Color.RED);
                    g2d.fillRect(i , j, 3, 3);
                    g2d.setPaint(Color.BLACK);
                }else if (pole.getDead((i - 10) / 3, (j - 10) / 3) > 0){
                g2d.fillRect(i , j, 3, 3);}
                else if (pole.isCorpse((i - 10) / 3, (j - 10) / 3) ){
                    g2d.setPaint(Color.GRAY);
                    g2d.fillRect(i , j, 3, 3);
                    g2d.setPaint(Color.black);
                }
            }
        }
        // задержка в 3 секунды
        if (System.currentTimeMillis() > endTime) {

            endTime = System.currentTimeMillis() + 2;
            pole.itr();
            //pole.itrobn();
               // pole.itrobn();

        }

        super.repaint();
        g2d.dispose();

    }


}
