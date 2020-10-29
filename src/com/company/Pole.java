package com.company;

public class Pole {

    public
    Kletka[][] matr;
    int W, H;
    String[] g = new String[]{"e1", "s2", "e3", "s4", "e5", "e7", "s6", "s8"};

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
                    for (int g = 0; g < matr[i][j].gen.length(); g++) {
                        char st = matr[i][j].gen.charAt(g);
                        if (st == 's') {
                            if (g + 1 < matr[i][j].gen.length()) {
                                if (matr[i][j].gen.charAt(g + 1) < '1' || matr[i][j].gen.charAt(g + 1) > '8') {
                                    continue;
                                } else {
                                    int t = Character.digit(matr[i][j].gen.charAt(g + 1), 10);
                                    //matr[i][j].step(matr,i,j,t);
                                    matr[i][j].it = false;
                                    this.step(i, j, t);
                                }
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
                                        continue;
                                    }
                                }
                            }
                        }

                    }

                }
            }
        this.itrobn();
    }

    private void bern(int kx, int ky, int t) {
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
