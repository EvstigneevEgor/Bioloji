/* Класс, который будет рисовать элементы*/
package com.company;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

class Canvas extends JComponent {

    /*Метод, перерисовывающий элемент внутри окна
     *при обновлении*/
    public void paintComponent(Graphics g){
        super.paintComponents(g);

        Graphics2D g2d=(Graphics2D)g;

        /* 	Устанавливает цвет рисования в зелёный*/
        g2d.setPaint(Color.BLACK);

        /* 	Рисует текущим цветом прямоугольник	*/
        //int i=
        for(int i=10;i<400;i+=10) {
            for(int j=10;j<400;j+=10){
                g2d.drawRect(j , i , 10, 10);
             }}
        g2d.setPaint(Color.RED);
        super.repaint();
        g2d.dispose();
    }

}