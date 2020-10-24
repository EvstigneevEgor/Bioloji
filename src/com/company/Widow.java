package com.company;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Widow extends JFrame{
    Canvas canv=new Canvas();
    public Widow() {
        super("eee");
       // JFrame w = new JFrame("Окно с изображением");
        this.setSize(430, 450);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        /*	Менеджер определяет
         *  каким образом в окне расположены объекты.*/
        this.setLayout(new BorderLayout(1, 1));

        this.setVisible(true);

        this.add(canv);
    }
}