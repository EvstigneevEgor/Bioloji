package com.company;

public class Pole {

    public
    Kletka[][] matr;

    Pole(int n, int w, int h) { //n - кол-во рандомных клеток
        matr = new Kletka[w][h];
        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++)
                matr[i][j] = new Kletka();
        for (int i = 0; i < n; i++) {
            int rw = (int) (Math.random() * (w - 1)) - 1, rh = (int) (Math.random() * (h - 1));
            while (matr[rw][rh].isLive()) {
                rw = (int) (Math.random() * w);
                rh = (int) (Math.random() * h);
            }
            matr[rw][rh].reviv();
        }
    }

    public int getLive(int w, int h) {
        //todo доделать определение цвета исходя из вида клетки
        if (matr[w][h].isLive())
            return 1;
        else
            return 0;
    }
}
