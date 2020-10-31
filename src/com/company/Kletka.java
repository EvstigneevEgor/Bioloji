package com.company;

public class Kletka {
    public
    boolean live;
    int kx, ky;
    String gen = new String();
    int energy = 5;
    boolean it = true;
    int cont=0;
    String s = "12345678sef";//ievasrz";

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
    public void bPerehod(char s) {
        it=false;
        int z=gen.indexOf(s);
        cont=(cont+((z==-1)?(1):(z)))%gen.length();
        //return ;

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
        if(kletka.it&&it){
            this.cont=0;
            this.gen = kletka.gen;
            if((int)(Math.random() * 5)==1){
                this.genMut();
            }
            else

        this.it = kletka.it;
        this.live = true;
        this.energy = kletka.energy / 2;
        kletka.energy = (kletka.energy + 1) / 2;
        //kletka.dead();
            }
    }

    private void genMut() {
        if((int)(Math.random() * 5)==1 && gen.length()<100) {
            gen+=s.charAt((int) (Math.random() * s.length()));
        }else{
            int b=(int) (Math.random() * gen.length());
            char c =  s.charAt((int) (Math.random() * s.length()));
            if(b+1<gen.length())
            gen= gen.substring(0,b)+c+gen.substring(b+1);
            else
                gen= gen.substring(0,b)+c;
        }
    }

    void dead() {
        it=false;
        live = false;
        gen = "";
        energy = 0;
    }
}
