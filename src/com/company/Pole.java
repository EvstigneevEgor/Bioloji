package com.company;

public class Pole {

    public
    Kletka[][] matr;
    int W, H;
    String[] g = new String[]{"f", "f", "f", "f", "f", "f", "f", "f"};
    int[] bervz = new int[]{1, 3, 5, 7};

    Pole(int n, int w, int h) {
        W = w;
        H = h;
        int o = 0;
        //n - кол-во рандомных клеток
        matr = new Kletka[w][h];
        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++)
                matr[i][j] = new Kletka(i, j);
        for (int i = 0; i < n; i++) {
            int rw = (int) (Math.random() * (w - 1)), rh = (int) (Math.random() * (h - 1));
            while (matr[rw][rh].isLive()) {
                rw = (int) (Math.random() * w);
                rh = (int) (Math.random() * h);
            }
            matr[rw][rh].reviv();
            matr[rw][rh].creategen(g[o++]);

        }
    }

    void itrobn() {
        for (int i = 0; i < W; i++)
            for (int j = 0; j < H; j++)
                matr[i][j].itnew();
    }

    void itr() {
        for (int i = 0; i < W; i++)
            for (int j = 0; j < H; j++) {
                if (matr[i][j].it && matr[i][j].isLive()) {
                    if (matr[i][j].energy >= 20) {
                        if(bernvzmzn(i,j)!=-1)
                        this.bern(i, j, bernvzmzn(i,j));
                        else
                            matr[i][j].dead();
                    }
                    if(matr[i][j].isLive()){
                    int g = matr[i][j].cont % matr[i][j].gen.length();
                    char st = matr[i][j].gen.charAt(g);
                    if (st == 'f') {
                        matr[i][j].energy+=1;
                        matr[i][j].cont++;
                    } else if (st == 's') {
                        if (g + 1 < matr[i][j].gen.length()) {
                            if (matr[i][j].gen.charAt(g + 1) < '1' || matr[i][j].gen.charAt(g + 1) > '8') {

                                matr[i][j].bPerehod(st);
                            } else {
                                int t = Character.digit(matr[i][j].gen.charAt(g + 1), 10);
                                //matr[i][j].step(matr,i,j,t);
                                matr[i][j].it = false;
                                this.step(i, j, t);
                            }
                        } else {
                            matr[i][j].bPerehod(st);
                        }
                    } else {
                        if (st == 'e') {
                            if (g + 1 < matr[i][j].gen.length()) {
                                if (matr[i][j].gen.charAt(g + 1) == '1' || matr[i][j].gen.charAt(g + 1) == '7' || matr[i][j].gen.charAt(g + 1) == '3' || matr[i][j].gen.charAt(g + 1) == '5') {
                                    int t = Character.digit(matr[i][j].gen.charAt(g + 1), 10);
                                    //matr[i][j].step(matr,i,j,t);
                                    matr[i][j].it = false;
                                    this.bern(i, j, t);
                                } else {
                                    matr[i][j].bPerehod(st);
                                }
                            } else {
                                matr[i][j].bPerehod(st);
                            }
                        } else {
                            matr[i][j].bPerehod(st);
                        }
                    }


                }
                }
            }
        //this.itrobn();
    }

    private int bernvzmzn(int kx, int ky) {
        String bufst = new String();

        if (ky >= 1)
            if (!matr[kx][ky - 1].isLive()&&matr[kx][ky - 1].it)
                bufst += "1";

            int matrx = kx;
            if (kx + 1 >= W)
                matrx = 0;
            else matrx++;
            if (!matr[matrx][ky].isLive()&&matr[matrx][ky].it)
                bufst += "3";

        if (ky < H - 1) {
            matrx = kx;
            if (kx + 1 >= W)
                matrx = 0;
            if (!matr[matrx][ky + 1].isLive()&&matr[matrx][ky + 1].it)
                bufst += "5";
        }
        matrx = kx;
        if (kx - 1 < 0)
            matrx = W - 1;
        else matrx--;
        if (!matr[matrx][ky].isLive()&&matr[matrx][ky].it)
            bufst += "7";

        int d=(bufst.length()==0)?(-1):(Character.digit(bufst.charAt((int) (Math.random() * bufst.length())), 10));
        return d;
    }

    private void bern(int kx, int ky, int t) {
        matr[kx][ky].cont++;
        switch (t) {
            case (1):
                if (ky >= 1) {
                    if (!matr[kx][ky - 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[kx][ky - 1].burn(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;

            case (3): {
                int matrx = kx;
                if (kx + 1 >= W)
                    matrx = 0;
                else matrx++;
                if (!matr[matrx][ky].isLive()) {
                    matr[kx][ky].energy -= 1;
                    if (matr[kx][ky].energy > 0) {
                        matr[matrx][ky].burn(matr[kx][ky]);
                    } else
                        matr[kx][ky].dead();
                }
            }
            break;

            case (5):
                if (ky < H - 1) {
                    int matrx = kx;
                    if (kx + 1 >= W)
                        matrx = 0;
                    if (!matr[matrx][ky + 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky + 1].burn(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;

            case (7): {
                int matrx = kx;
                if (kx - 1 < 0)
                    matrx = W - 1;
                else matrx--;
                if (!matr[matrx][ky].isLive()) {
                    matr[kx][ky].energy -= 1;
                    if (matr[kx][ky].energy > 0) {
                        matr[matrx][ky].burn(matr[kx][ky]);
                    } else
                        matr[kx][ky].dead();
                }
            }
            break;

            default:
                break;
        }
    }

    private void step(int kx, int ky, int t) {
        matr[kx][ky].cont++;
        switch (t) {
            case (1):
                if (ky >= 1) {
                    if (!matr[kx][ky - 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[kx][ky - 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            case (2):
                if (ky >= 1) {
                    int matrx = kx;
                    if (kx + 1 >= W)
                        matrx = 0;
                    else matrx++;
                    if (!matr[matrx][ky - 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky - 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            case (3): {
                int matrx = kx;
                if (kx + 1 >= W)
                    matrx = 0;
                else matrx++;
                if (!matr[matrx][ky].isLive()) {
                    matr[kx][ky].energy -= 1;
                    if (matr[kx][ky].energy > 0) {
                        matr[matrx][ky].repl(matr[kx][ky]);
                    } else
                        matr[kx][ky].dead();
                }
            }
            break;
            case (4):
                if (ky < H - 1) {
                    int matrx = kx;
                    if (kx + 1 >= W)
                        matrx = 0;
                    else matrx++;
                    if (!matr[matrx][ky].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky + 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            case (5):
                if (ky < H - 1) {
                    int matrx = kx;
                    if (kx + 1 >= W)
                        matrx = 0;
                    if (!matr[matrx][ky + 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky + 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            case (6):
                if (ky < H - 1) {
                    int matrx = kx;
                    if (kx - 1 < 0)
                        matrx = W - 1;
                    else matrx--;
                    if (!matr[matrx][ky + 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky + 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            case (7): {
                int matrx = kx;
                if (kx - 1 < 0)
                    matrx = W - 1;
                else matrx--;
                if (!matr[matrx][ky].isLive()) {
                    matr[kx][ky].energy -= 1;
                    if (matr[kx][ky].energy > 0) {
                        matr[matrx][ky].repl(matr[kx][ky]);
                    } else
                        matr[kx][ky].dead();
                }
            }
            break;
            case (8):
                if (ky >= 1) {
                    int matrx = kx;
                    if (kx - 1 < 0)
                        matrx = W - 1;
                    else matrx--;
                    if (!matr[matrx][ky - 1].isLive()) {
                        matr[kx][ky].energy -= 1;
                        if (matr[kx][ky].energy > 0) {
                            matr[matrx][ky - 1].repl(matr[kx][ky]);
                        } else
                            matr[kx][ky].dead();
                    }
                }
                break;
            default:
                break;
        }
    }

    int getLive(int w, int h) {
        //todo доделать определение цвета исходя из вида клетки
        if (matr[w][h].isLive())
            return 1;
        else
            return 0;
    }
}
