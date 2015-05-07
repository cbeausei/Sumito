/*
Class DrawView
Written by Bruno Quercia

This class implements the core of the graphic interface: it handles the drawing of the board, and the
selection of the user

Since the user will have brand different behaviours from the moment he / she starts to select balls
to the moment he / she finishes a move, this class will have to adapt its behaviour accordingly.

Therefore it is virtually equivalent to a finite automaton, whose transitions can either be an event
coming from the user (basically a touch event), or the achievement of an intern algorithm.

Originally, 5 states were planned, but in order to handle the possibles evolutions, the current state
is stored in a 1-byte variable.
 */

package axelindustry.sumito4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

import axelindustry.sumito4.IA.Ball;
import axelindustry.sumito4.IA.Board;

public class DrawView extends View {
    Canvas canvas = new Canvas();

    /* The following constant values will be used in the coordinates formulae
    /!\ THE TERM "RELATIVE" MUST BE UNDERSTOOD AS "RELATIVELY TO THE HEIGHT OF THE BOARD IMAGE" /!\
    rel_span_x indicates the relative space between a position and its nearest lateral neighbour
    rel_offset_x indicates the relative offset of the board in the picture, that is the lateral offset of the extreme left position
    rel_b_h indicates the relative ball height ; it is also rhe relative ball width
    rel_span_y indicates the relative distance between two lines
    /!\ rel_span_y is not a distance between neighbour positions /!\
    rel_offset_y indicates the relative offset of the board in the picture, that is the vertical offset of the extreme top line

     */

    private static final double rel_span_x = 45/516.0, rel_offset_x = 29/129.0, rel_b_h = 1/12.0, rel_span_y = 78/8.0/129.0, rel_offset_y = 26/129.0;

    //Let us now define the states we will be using. Names are more eloquent than raw numbers.

    private static final byte INITIAL_STATE = 1, PROCESSING_SELECTION = 2, END_SELECTION = 3, PROCESSING_MOVEMENT_CHOICE = 4, EXECUTE_MOVEMENT = 5;

    //To remember the current state, we will need a variable... state.

    private byte state;

    /* We will consider a list of white balls
    the picture of a white ball will be stored in memory using bouleBlanche, then every member of balls will be a resized copy of bouleBlanche
    it works the same for black balls, of course
     */
    Board board;
    LinkedList <DrawBall> balls, selectList;
    DrawBall startBall, endBall;
    Bitmap plateau, fond, bouleNoire, bouleBlanche, bouleBleue, tick, cross;
    Bitmap[] arrows;

    // the boolean selection will help us detect the phase during which the user is selecting the balls he wants to move
    boolean selection = false, chooseMovement = false, validateMovement = false;
    Boolean[] list;
    // x and y will contain the coordinates of any user event. For some weired reason, these coordinates are not integers...
    float x = 10, y = 20;
    /* w and h will match the dimensions of the screen
    width and height those of the board picture
    evidently, width <= w and height <= h
    There is always at least one case of equality, rarely both
     */
    int w = 0, h = 0, width, height, move;


    public DrawView(Context context) {
        super(context);
        // let's store the bitmaps in memory
        bouleNoire = BitmapFactory.decodeResource(getResources(), R.drawable.boulenoire);
        bouleBlanche = BitmapFactory.decodeResource(getResources(), R.drawable.bouleblanche);
        bouleBleue = BitmapFactory.decodeResource(getResources(), R.drawable.boulebleue);

        balls = new LinkedList();
        selectList = new LinkedList();
        board = new Board();
        refresh();

        // we begin with the initial state
        state = INITIAL_STATE;

        // the board and background are stored too
        plateau = BitmapFactory.decodeResource(getResources(), R.drawable.cadre);
        fond = BitmapFactory.decodeResource(getResources(), R.drawable.fond);
        tick = BitmapFactory.decodeResource(getResources(), R.drawable.tick);
        cross = BitmapFactory.decodeResource(getResources(), R.drawable.croix);
        arrows = new Bitmap[]{BitmapFactory.decodeResource(getResources(), R.drawable.fleche0),
                BitmapFactory.decodeResource(getResources(), R.drawable.fleche1),
                BitmapFactory.decodeResource(getResources(), R.drawable.fleche2),
                BitmapFactory.decodeResource(getResources(), R.drawable.fleche3),
                BitmapFactory.decodeResource(getResources(), R.drawable.fleche4),
                BitmapFactory.decodeResource(getResources(), R.drawable.fleche5)};
    }

    /* convertCoordinates(X, Y) returns [x, y] where:
        - X is the column number in the game grid
        - Y is the row number in the game grid
        - (x, y) is the pixel matching the centre of the position (X, Y) on the screen
     */
    private int[] convertCoordinates(int x, int y){
        int absc = (int)(((x+y/2.0-2.0)*rel_span_x+rel_offset_x-rel_b_h/2)*height + (w-width)/2.0);
        y = (int)((y*rel_span_y+rel_offset_y-rel_b_h/2)*height + (h-height)/2.0);
        x = absc;
        return(new int[]{x, y});
    }

    /* revertCoordinates(x, y) returns [X, Y] where:
        - (x, y) is a pixel on the screen
        - (X, Y) is the position in the grid whiches representation on the screen is the closest to (x, y)
     */
    private int[] revertCoordinates(int x, int y){
        y = (int)(1/rel_span_y/height*(y +(height-h)/2 +rel_b_h*height/2-rel_offset_y*height));
        x = (int)(1/rel_span_x/height*(x + (width-w)/2 +rel_b_h*height/2-rel_offset_x*height)+2-y/2);
        return(new int[]{x, y});
    }

    /* NB: - applying convertCoordinates to the result of revertCoordinates has an effect of magnetic grid:
             the pixel is converted into the closest pixel that represents a position
           - applying revertCoordinates to the result of convertCoordinates has no effect
     */

    // dimensions() computes the dimensions of the displayed objects whenever the size of the screen varies

    private void cancel(){
        for(DrawBall elem : selectList){
            if(elem.getColour() == 0){
                elem.changeBitmap(bouleBlanche);
            }
            else elem.changeBitmap(bouleNoire);
        }
        selectList.clear();
        validateMovement = false;
        chooseMovement = false;
    }
    private void dimensions(){
        h = canvas.getHeight();
        w = canvas.getWidth();

        // we compute the optimal dimensions of the board image: it must be as large as possible, but not outspan the screen
        if (1290 * w < 1487 * h) {
            width = w;
            height = 1290 * w / 1487;
        } else {
            height = h;
            width = 1487 * h / 1290;
        }
        // once done, we draw accordingly
        plateau = Bitmap.createScaledBitmap(plateau, width, height, true);
        for(int i = 0 ; i < 6 ; i++)
            arrows[i] = Bitmap.createScaledBitmap(arrows[i], width/3, height/3, true);

        // the background is easier to draw: there must just be no hole
        fond = Bitmap.createScaledBitmap(fond, Math.max(w, h), Math.max(w, h), true);
        tick = Bitmap.createScaledBitmap(tick, height/12, height/12, true);
        cross = Bitmap.createScaledBitmap(cross, height/12, height/12, true);
        bouleBlanche = Bitmap.createScaledBitmap(bouleBlanche, height / 12, height / 12, true);
        bouleNoire = Bitmap.createScaledBitmap(bouleNoire, height / 12, height / 12, true);

        // we draw the balls proportionally to the height of the board
        for(DrawBall e : balls) {
            e.changeBitmap(Bitmap.createScaledBitmap(e.getBitmap(), height / 12, height / 12, true));
        }
        bouleBleue = Bitmap.createScaledBitmap(bouleBleue, height / 12, height / 12, true);
    }

    /* the core of the drawing phase happens here:
    onDraw is called whenever something has changed, have it an actual influence on the drawing or not, eg:
     - when the screen is rotated
     - when invalidate() is called, hence when the user touches the screen
     */
    @Override
    public void onDraw(final Canvas canvas) {
        this.canvas = canvas;

        // h contains the previously-computed height of the screen. There is no need to re-compute all dimensions unless this height has changed
        if(h != canvas.getHeight()){
            this.dimensions();
        }

        // OK, the dimensions are fine, we can draw
        canvas.drawBitmap(fond, 0, 0, null);
        canvas.drawBitmap(plateau, (w - width) / 2, (h - height) / 2, null);

        if(state == PROCESSING_SELECTION || state == END_SELECTION) {
            int[] coordinatesSelection = revertCoordinates((int) x, (int) y);
            for(DrawBall ball : balls){
                if((ball.getX() == coordinatesSelection[0] && ball.getY() == coordinatesSelection[1])){
                    selectList.remove(ball);
                    selectList.add(ball);
                    if(selectionIsValid(selectList))
                        ball.changeBitmap(bouleBleue);
                    else{
                        selectList.remove(ball);
                        if(ball.getColour() == 0){
                            ball.changeBitmap(bouleBlanche);
                        }
                    }
                }
            }
        }

        if(!selectList.isEmpty()){
            int[] coord = revertCoordinates((int)x, (int)y);
            if(coord[0] < 8 && coord[1] > 12 - coord[0] ){
                switch(selectList.size()){
                    case 1:
                        list = board.getDirections(selectList.get(0).getY(), selectList.get(0).getX());
                        break;
                    case 2:
                        list = board.getDirections(selectList.get(0).getY(), selectList.get(0).getX(), selectList.get(1).getY(), selectList.get(1).getX());
                        break;
                    case 3:
                        list = board.getDirections(selectList.get(0).getY(), selectList.get(0).getX(), selectList.get(1).getY(), selectList.get(1).getX(), selectList.get(2).getY(), selectList.get(2).getX());
                        break;
                    default:
                        list = new Boolean[] {false, false, false, false, false, false};
                }
                if(list[0])
                    canvas.drawBitmap(arrows[0], (w - width) / 2 + 2 * width / 3, (h - height) / 2 + height / 3, null);
                if(list[1])
                    canvas.drawBitmap(arrows[1], (w - width) / 2 + width / 2, (h - height) / 2, null);
                if(list[2])
                    canvas.drawBitmap(arrows[2], (w - width) / 2 + width / 6, (h - height) / 2, null);
                if(list[3])
                    canvas.drawBitmap(arrows[3], (w - width) / 2, (h - height) / 2 + height / 3, null);
                if(list[4])
                    canvas.drawBitmap(arrows[4], (w - width) / 2 + width / 6, (h - height) / 2 + 2 * height / 3, null);
                if(list[5])
                    canvas.drawBitmap(arrows[5], (w - width) / 2 + width / 2, (h - height) / 2 + 2 * height / 3, null);
                chooseMovement = true;
            }
            else if(coord[0] < 0 && coord[1] > 4 ){
                cancel();
            }
            else {
                coord = convertCoordinates(6, 8);
                canvas.drawBitmap(tick, coord[0], coord[1], null);
                coord = convertCoordinates(-2, 8);
                canvas.drawBitmap(cross, coord[0], coord[1], null);
            }
        }

        for(DrawBall e : balls) {
            // We have the coordinates of the balls in the game grid. We must compute the coordinates of their representation on the screen
            int coordinates[] = convertCoordinates(e.getX(), e.getY());

            // Now we can draw them
            canvas.drawBitmap(e.getBitmap(), coordinates[0], coordinates[1], null);
        }
    }

    // the following method handles all touch events coming from the user
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ok, an event has occurred. Let's locate it first
        x = event.getRawX();
        y = event.getRawY();

        // We must react accordingly: as soon as the finger is down, we enter a phase of selection
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch(state){
                case INITIAL_STATE:
                    state = PROCESSING_SELECTION;
                    startBall = balls.getFirst();
                    int[] coords = revertCoordinates((int)x, (int)y);
                    for(DrawBall e: balls){
                        if(Math.pow(e.getX() - coords[0], 2) + Math.pow(e.getY() - coords[1], 2) < Math.pow(startBall.getX() - coords[0], 2) + Math.pow(startBall.getY() - coords[1], 2)){
                            startBall = e;
                        }
                    }
                    // OK, now startBall contains the nearest ball to the beginning of the selection
                    break;
                case END_SELECTION:
                    state = PROCESSING_MOVEMENT_CHOICE;
                    break;
            }
        }

        // as soon as the finger is up, the selection is over
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            switch(state){
                case PROCESSING_SELECTION:
                    if(selectList.isEmpty()) state = INITIAL_STATE;
                    else state = END_SELECTION;
                    break;
                case PROCESSING_MOVEMENT_CHOICE:
                    state = EXECUTE_MOVEMENT;
                    board.doUserMove(move);
                    refresh();
                    break;
            }
        }
        else if(state == PROCESSING_SELECTION){
            float absc = startBall.getX() - revertCoordinates(x), ord = startBall.getY() - revertCoordinates(y)
            int distance = Math.min((int) (Math.sqrt(Math.pow(absc, 2) + Math.pow(ord, 2)) / rel_span_x), 3);
            // OK, we know the number of balls. We must then determine the vector.
            if(absc > sqrt(3)/2){
                absc = 1;
                ord = 0;
            }
            else if(absc > 0 && ord > 0){
                absc = 1;
                ord = -1;
            }
            else if(absc < 0 && ord < sqrt(3)/2){
                absc = 0;
                ord = -1;
            }
            else if(ord > sqrt(3)/2){
                absc = -1;
                ord = 0;
            }
            else if(absc < 0 && ord < 0){
                absc = -1;
                ord = 1;
            }
            else{
                absc = 0;
                ord = 1;
            }
            selectList.clear();
            selectList.append(startBall);
            if(distance > 1){
                for(DrawBall e : balls){
                    if(e.getX() == startBall.getX() + absc && e.getY() == startBall.getY() + ord){
                        e.changeBitmap(bouleBleue)
                        selectList.append(e);
                    }
                }
                if(!selectionIsValid(selectList)){
                    DrawBall e = selectList.getLast();
                    if(e.getColour() == 0)
                        e.changeBitmap(bouleBlanche)
                    else e.changeBitmap(bouleNoire)
                    selectList.removeLast();
                }
                if(distance == 3 && selectList.size() == 2){
                    for(DrawBall e : balls){
                        if(e.getX() == startBall.getX() + 2 * absc && e.getY() == startBall.getY() + 2 * ord){
                            selectList.append(e);
                        }
                    }
                    if(!selectionIsValid(selectList)){
                        DrawBall e = selectList.getLast();
                        if(e.getColour() == 0)
                            e.changeBitmap(bouleBlanche)
                        else e.changeBitmap(bouleNoire)
                        selectList.removeLast();
                    }
                }
            }
        }

        this.invalidate();
        return true;
    }

    public void refresh(){
        LinkedList<Ball> tmp = board.getBalls();
        balls.clear();
        cancel();
        for(Ball e : tmp){
            if(e.color == 1)
                balls.add(new DrawBall(e.j, e.i, bouleBlanche, 0));
            else balls.add(new DrawBall(e.j, e.i, bouleNoire, 1));
        }
    }

    public boolean selectionIsValid(LinkedList <DrawBall> l){
        if(l.size() > 3) return false;
        else if(l.size() < 2) return true;
        else{
            int vectorX = l.get(0).getX() - l.get(1).getX(), vectorY = l.get(0).getY() - l.get(1).getY();
            if((vectorX == 1 && vectorY == 0) || (vectorX == -1 && vectorY == 0)
                    ||(vectorX == 1 && vectorY == -1) || (vectorX == -1 && vectorY == 1)
                    ||(vectorX == 0 && vectorY == 1) || (vectorX == 0 && vectorY == -1)) {
                if (l.size() == 2) return (l.get(0).getColour ()== l.get(1).getColour());
                else{
                    return ((l.get(2).getX() - l.get(0).getX() == vectorX && l.get(2).getY() - l.get(0).getY() == vectorY)
                            ||(l.get(1).getX() - l.get(2).getX() == vectorX && l.get(1).getY() - l.get(2).getY() == vectorY)
                            && (l.get(0).getColour() == l.get(1).getColour() && l.get(1).getColour() == l.get(2).getColour()));
                }
            }
            else if(l.size() == 3){
                if(!(l.get(0).getColour() == l.get(1).getColour() && l.get(1).getColour() == l.get(2).getColour())) return false;
                vectorX = (int) Math.ceil(vectorX / 2.);
                vectorY = (int) Math.ceil(vectorY / 2.);
                if ((vectorX == 1 && vectorY == 0) || (vectorX == -1 && vectorY == 0)
                        || (vectorX == 1 && vectorY == -1) || (vectorX == -1 && vectorY == 1)
                        || (vectorX == 0 && vectorY == 1) || (vectorX == 0 && vectorY == -1)) {
                    return (l.get(2).getX() - l.get(1).getX() == vectorX && l.get(2).getY() - l.get(1).getY() == vectorY);
                } else return false;
            }
            else return false;
        }
    }
}