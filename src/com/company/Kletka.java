package com.company;

public class Kletka {
    public
    boolean live;
    int kx, ky;
    String gen = new String();
    int energy = 5;
    boolean it = true;
    private
    String s = "12345678ssss";//ievasrz";

    public Kletka(int x, int y) {
        kx = x;
        ky = y;
        live = false;
    }

    private void createRandGen() {
        for (int i = 0; i < 1 + ((int) (Math.random() * 30)); i++) {
            gen += s.charAt((int) (Math.random() * s.length()));

        }

    }

    public void creategen(String s) {
        gen = s;
        System.out.println(gen);
    }

    boolean isLive() {
        return live;
    }

    public void reviv() {
        live = true;
        energy = 5;

        this.createRandGen();
    }


    //todo сделать главную функцию 
    public Kletka[][] it(Kletka[][] buf, int w, int h) {

        return buf;
    }

    void itnew() {
        it = true;
    }

    void repl(Kletka kletka) {
        this.gen = kletka.gen;
        this.it = kletka.it;
        this.live = true;
        this.energy += kletka.energy;
        kletka.dead();
    }

    void burn(Kletka kletka) {
        this.gen = kletka.gen;
        this.it = kletka.it;
        this.live = true;
        this.energy = kletka.energy / 2;
        kletka.energy = (kletka.energy + 1) / 2;
        //kletka.dead();
    }

    void dead() {
        live = false;
        gen = "";
        energy = 0;
    }
}
