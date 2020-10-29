package com.company;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Widow extends JFrame{
    Petri petri = new Petri();
    public Widow() {
        super("eee");
       // JFrame w = new JFrame("Окно с изображением");
        this.setSize(petri.widhtWin + 30, petri.heightWin + 50);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        /*	Менеджер определяет
         *  каким образом в окне расположены объекты.*/
        this.setLayout(new BorderLayout(1, 1));

        this.setVisible(true);

        this.add(petri);
    }
}