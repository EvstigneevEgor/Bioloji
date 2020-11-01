package com.company;

public class Kletka {
    public
    boolean live;
    int kx, ky;
    String gen = new String();
    int energy = 1;
    boolean it = true;
    boolean isdead=false;
    boolean isbern=false;

    int timeLive=1000;
    int cont=0;
    String s = "12345678seaf";//ievasrz";
    boolean corpse=false;
    private static final int retribution=20;
    public Kletka(int x, int y) {
        kx = x;
        ky = y;
        live = false;
        corpse=false;
        isbern=true;
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
    public int hash(int s) {
        int z=gen.indexOf(gen.charAt(s));

            return (cont+((z==-1)?(1):(z)))%gen.length();

        //return ;

    }

    boolean isLive() {
        return live;
    }
    boolean isParents(Kletka k){
        int razn=Math.abs(gen.length()-k.gen.length());
        for(int i = 0 ; i < Math.min(gen.length(),k.gen.length());i++){
            if(gen.charAt(i)!=k.gen.charAt(i))
               razn++;
        }
       // System.out.println((razn*100)/Math.min(gen.length(),k.gen.length()));
        return ((razn*100)/Math.min(gen.length(),k.gen.length())<20);
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
        isbern=false;
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
            if((int)(Math.random() * 6)==1){
                this.genMut();
            }
            else

        this.it = kletka.it;
        this.live = true;
        this.isdead = false;
        this.energy += kletka.energy / 3;
        this.isbern=true;
        kletka.energy = kletka.energy - this.energy;
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
        if(!corpse){
            if(!isParents(kletka))
        if(kletka.energy-10>energy){

            repl(kletka);
        }else{
            kletka.energy-=retribution;
            if (energy<=0)
                dead();
        }else
            kletka.cont++;
        }else{
            kletka.energy+=200;
            repl(kletka);
        }

    }

    public boolean isCorpse() {
        return corpse;
    }
}
