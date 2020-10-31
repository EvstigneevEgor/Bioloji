package com.company;

public class Kletka {
    public
    boolean live;
    int kx, ky;
    String gen = new String();
    int energy = 2;
    boolean it = true;
    boolean isdead=false;
    int timeLive=1000;
    int cont=0;
    String s = "12345678seaaa12345678";//ievasrz";
    boolean corpse=false;
    private static final int retribution=200;
    public Kletka(int x, int y) {
        kx = x;
        ky = y;
        live = false;
        corpse=false;
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
        energy--;
        int z=gen.indexOf(s);
        if((cont+((z==-1)?(1):(z)))%gen.length()!=cont)
        cont=(cont+((z==-1)?(1):(z)))%gen.length();
        else
            cont=(cont+((z+3==-1)?(1):(z+3)))%gen.length();
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
        isdead=false;
    }

    void repl(Kletka kletka) {
        this.gen = kletka.gen;
        this.it = kletka.it;
        this.live = true;
        this.isdead = false;
        this.cont=cont;
        this.energy += kletka.energy;
        corpse=false;
        kletka.corpse=false;
        kletka.del();
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
        this.isdead = false;
        this.energy = kletka.energy / 6;
        kletka.energy = ((kletka.energy + 1) / 5)*4;
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
        energy = 1;
        isdead=true;
        timeLive=100;
        corpse=true;
    }
    void del() {
        it=false;
        live = false;
        gen = "";
        energy = 1;
        //isdead=true;
        timeLive=100;
    }

    public void atack(Kletka kletka) {
        if(!kletka.corpse){
        if(energy-10>kletka.energy){

            kletka.repl(this);
        }else{
            energy-=retribution;
            if (energy<=0)
                this.dead();
        }}else{
            energy+=300;
            kletka.repl(this);
        }

    }

    public boolean isCorpse() {
        return corpse;
    }
}
