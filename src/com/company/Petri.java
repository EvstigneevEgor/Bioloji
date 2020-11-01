package com.company;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Petri extends JComponent {

    /*Метод, перерисовывающий элемент внутри окна
     *при обновлении*/
    int widhtWin = 1100, heightWin = 600;
    Pole pole = new Pole(4, (widhtWin - 10) / 3, (heightWin - 10) / 3);

    long endTime = System.currentTimeMillis() + 120;
    long endTimeYear = System.currentTimeMillis() + 1000*120*2;

    public void paintComponent(Graphics g) {
        super.paintComponents(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setPaint(Color.BLACK);

        int popul=0;
        int populB=0;
        int populD=0;
        for (int i = 10; i < widhtWin-3; i += 3) {
            for (int j = 10; j < heightWin-3; j += 3) {
                //g2d.drawRect(i, j, 3, 3);
                if (pole.getLive((i - 10) / 3, (j - 10) / 3) > 0) {
                    if (pole.isBern((i - 10) / 3, (j - 10) / 3)) {
                        g2d.setPaint(Color.MAGENTA);
                        populB++;
                    }else
                        g2d.setPaint(Color.RED);
                    popul++;
                    g2d.fillRect(i , j, 3, 3);
                    g2d.setPaint(Color.BLACK);
                }else if (pole.getDead((i - 10) / 3, (j - 10) / 3) > 0){
                    populD++;
                g2d.fillRect(i , j, 3, 3);
                }
                else if (pole.isCorpse((i - 10) / 3, (j - 10) / 3) ){
                    g2d.setPaint(Color.GRAY);
                    g2d.fillRect(i , j, 3, 3);

                    g2d.setPaint(Color.black);
                }
            }
        }
        // задержка в 3 секунды
        if (System.currentTimeMillis() > endTime) {

            endTime = System.currentTimeMillis() + 120/20;
            pole.itr();
            //pole.itrobn();
               // pole.itrobn();

        }
        if (System.currentTimeMillis() > endTimeYear) {
            pole.year();
            System.out.println("Зима близко");
            endTimeYear = System.currentTimeMillis() + 1000*120;
        }
        JTextArea textArea = new JTextArea("111");

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("общая популяция - "+String.valueOf(popul)+". время года : "+((pole.leto)?("лето"):("Зима"))+". радилось за этот ход - "+String.valueOf(populB)+" умерло за этот ход - "+String.valueOf(populD), 8, heightWin+10);

        super.repaint();
        g2d.dispose();

    }


}
