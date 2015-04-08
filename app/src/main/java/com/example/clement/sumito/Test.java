package com.example.clement.sumito;

/**
 * Created by Clement on 08/04/2015.
 */
public class Test {
    public static void main(String[] args) {

        Board board=new Board();
        board.display();
        Bot bot1=new Bot(0,0,1,board);
        Bot bot2=new Bot(1,0,1,board);
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
        bot1.play(board);
        board.display();
        bot2.play(board);
        board.display();
    }
}
