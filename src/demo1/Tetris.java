package demo1;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//编写俄罗斯方块主类
public class Tetris extends JPanel {

    //声明正在下落
    private Tetromino currentOne = Tetromino.randomOne();
    // 将下落的方块
    private Tetromino nextOne = Tetromino.randomOne();
    //声明游戏主区域
    private Cell[][] wall = new Cell[18][9];
    //声明单元格的值为48像素
    private static final int CELL_SIZE = 48;

    //声明游戏分数池
    int[] scores_pool = {0,1,2,5,10};
    //声明当前游戏的分数 行数
    private int totalScore;
    private int totalLine;

    //声明游戏的三种状态，游戏中 暂停 游戏结束
    public static final int PLAYING = 0;
    public static final int PAUSE = 1;
    public static final int GAMEOVER = 2;

    //声明变量存放当前状态值
    private int game_state = 2;

    //每次下落间隔多少ms
    private int step = 500;
    //声明一个数组， 用来显示游戏状态
    String[] show_state = {"P[pause]","C[continue]","S[replay]"};


    //载入方块图片
    public static BufferedImage I;
    public static BufferedImage J;
    public static BufferedImage L;
    public static BufferedImage O;
    public static BufferedImage S;
    public static BufferedImage T;
    public static BufferedImage Z;
    public static BufferedImage backImage;
    static{
        try {
            I = ImageIO.read(new File("images/I.png"));
            J = ImageIO.read(new File("images/J.png"));
            L = ImageIO.read(new File("images/L.png"));
            O = ImageIO.read(new File("images/O.png"));
            S = ImageIO.read(new File("images/S.png"));
            T = ImageIO.read(new File("images/T.png"));
            Z = ImageIO.read(new File("images/Z.png"));
            backImage = ImageIO.read(new File("images/background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(backImage,0,0,null);
        //平移坐标轴
        g.translate(22,15);
        //绘制游戏主区域
        paintWall(g);
        //绘制正在下落的四方格
        printCurrentOne(g);
        //绘制将要下落的四方格
        printNextOne(g);
        //绘制游戏得分
        paintScore(g);
        //绘制游戏当前状态
        paintState(g);
    }

    //封装业务逻辑
    public void start(){
        game_state = PLAYING;
        KeyListener l = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                switch (code){
                    case KeyEvent.VK_DOWN:
                        softDropAction();
                        break;
                    case KeyEvent.VK_LEFT:
                        moveLeftAction();
                        break;
                    case KeyEvent.VK_RIGHT:
                        moveRightAction();
                        break;
                    case KeyEvent.VK_UP:
                        rotateRightAction();
                        break;
                    case KeyEvent.VK_SPACE:
                        handDropAction();
                        break;
                    //暂停游戏
                    case KeyEvent.VK_P:
                        if (game_state == PLAYING){
                            game_state = PAUSE;
                        }
                        break;
                    //游戏继续
                    case KeyEvent.VK_C:
                        if (game_state == PAUSE){
                            game_state = PLAYING;
                        }
                        break;
                    //游戏重新开始
                    case KeyEvent.VK_S:
                        if (game_state == GAMEOVER){
                            wall = new Cell[18][9];
                            currentOne = Tetromino.randomOne();
                            nextOne = Tetromino.randomOne();
                            totalLine = 0;
                            totalScore = 0;
                            step = 500;
                        }
                        break;
                }
            }
        };

        //将俄罗斯方块窗口设置为焦点
        this.addKeyListener(l);
        this.requestFocus();

        while (true){
            //判断状态在游戏中， 每0。5s下落一次
            if (game_state == PLAYING){
                if (step >= 300){
                    step -= 5;
                }
                else if (step >= 200){
                    step -= 1;
                }
                try{
                    Thread.sleep(step);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                if (canDrop()){
                    currentOne.softDrop();
                }
                else {
                    landtoWall();
                    destroyLine();
                    if (isGameOver()){
                        game_state = GAMEOVER;
                    }else{
                        currentOne = nextOne;
                        nextOne = Tetromino.randomOne();
                    }
                }
            }
            repaint();
        }
    }

    //创建顺时针旋转
    public void rotateRightAction(){
        currentOne.rotateRight();
        if (outOfBound() == true || coincide()){
            currentOne.rotateLeft();
        }
    }

    //判断当前行是否已满
    public boolean isFullLine(int row){
        Cell[] cells = wall[row];
        for (Cell cell: cells){
            if (cell == null){
                return false;
            }
        }
        return true;
    }

    //瞬间下落
    public void handDropAction(){
        while (canDrop() == true){
            currentOne.softDrop();
        }
        landtoWall();
        destroyLine();
        if (isGameOver() == true){
            game_state = GAMEOVER;
        }
        else {
            currentOne = nextOne;
            nextOne = Tetromino.randomOne();
        }
    }

    //按键一次 四方格下落一个
    public void softDropAction(){
        if (canDrop() == true){
            currentOne.softDrop();
        }
        else {
            landtoWall();
            destroyLine();
            if (isGameOver() == true){
                game_state = GAMEOVER;
            }
            else {
                currentOne = nextOne;
                nextOne = Tetromino.randomOne();
            }
        }
    }

    //将到墙的方块嵌入到墙中
    private void landtoWall() {
        Cell[] cells = currentOne.cells;
        for (Cell cell:cells){
            int row = cell.getRow();
            int col = cell.getCol();
            wall[row][col] = cell;
        }
    }

    //判断四方格能否下落
    public boolean canDrop(){
        Cell[] cells = currentOne.cells;
        for (Cell cell:cells){
            int row = cell.getRow();
            int col = cell.getCol();
            if (row == wall.length - 1 || wall[row + 1][col] != null){
                return false;
            }
        }
        return true;
    }

    //创建消行方法
    public void destroyLine(){
        int line = 0;
        Cell[] cells = currentOne.cells;
        for (Cell cell:cells){
            int row = cell.getRow();
            if (isFullLine(row)){
                line += 1;
                for (int i=row;i>0;i--){
                    System.arraycopy(wall[i - 1], 0,wall[i], 0,wall[0].length);
                }
                wall[0] = new Cell[9];
            }
        }
        //获取分数行数 累加到总分数行数中
        totalLine += line;
        totalScore += scores_pool[line];
    }

    //判断游戏是否结束
    public boolean isGameOver(){
        Cell[] cells = nextOne.cells;
        for (Cell cell:cells){
            int row = cell.getRow();
            int col = cell.getCol();
            if (wall[row][col] != null){
                return true;
            }
        }
        return false;
    }

    //绘制游戏当前状态
    private void paintState(Graphics g) {
        if (game_state == PLAYING) {
            g.drawString(show_state[PLAYING], 500, 660);
        } else if (game_state == PAUSE) {
            g.drawString(show_state[PAUSE], 500, 660);
        } else if (game_state == GAMEOVER) {
            g.drawString(show_state[GAMEOVER], 500, 660);
            g.setColor(Color.red);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 60));
            g.drawString("GAMEOVER!", 30, 400);
        }
    }

    //显示分数
    private void paintScore(Graphics g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("分数:" + totalScore,500,248);
        g.drawString("行数:"+totalScore,500,435);
    }

    //显示下一个四方格的形状
    private void printNextOne(Graphics g) {
        Cell[] cells = nextOne.cells;
        for (Cell cell: cells){
            int x = cell.getCol() * CELL_SIZE + 380;
            int y = cell.getRow() * CELL_SIZE + 27;
            g.drawImage(cell.getImage(),x,y,null);
        }
    }

    //显示当前四方格
    private void printCurrentOne(Graphics g) {
        Cell[] cells = currentOne.cells;
        for (Cell cell: cells){
            int x = cell.getCol() * CELL_SIZE;
            int y = cell.getRow() * CELL_SIZE;
            g.drawImage(cell.getImage(),x,y,null);
        }
    }

    //显示背景
    private void paintWall(Graphics g) {
        for (int i=0;i< wall.length;i++){
            for (int j=0;j<wall[i].length;j++){
                int x = j * CELL_SIZE;
                int y = i * CELL_SIZE;
                Cell cell = wall[i][j];
                //判断当前单元格是否有小方块 没有则绘制矩形，有则将小方块嵌入墙中
                if (cell == null){
                    g.drawRect(x,y,CELL_SIZE,CELL_SIZE);
                }
                else {
                    g.drawImage(cell.getImage(),x,y,null);
                }
            }
        }
    }

    //判断方块是否重合
    public boolean coincide(){
        Cell[] cells = currentOne.cells;
        for (Cell cell: cells){
            int row = cell.getRow();
            int col = cell.getCol();
            if (wall[row][col] != null){
                return true;
            }
        }
        return false;
    }

    //按键一次，四方格左移动一次
    public void moveLeftAction(){
        //判断是否越界或四方格是否重合
        currentOne.moveLeft();
        if (outOfBound() == true || coincide()==true){
            currentOne.moveRight();
        }

    }

    //按键一次，四方格右移动一次
    public void moveRightAction(){
        //判断是否越界或四方格是否重合
        currentOne.moveRight();
        if (outOfBound() == true || coincide()==true){
            currentOne.moveLeft();
        }

    }

    //判断游戏是否出界
    public boolean outOfBound(){
        Cell[] cells = currentOne.cells;
        for (Cell cell:cells){
            int col = cell.getCol();
            int row = cell.getRow();
            if (row < 0 || row > wall.length - 1 || col < 0 || col > wall[0].length - 1){
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        //创建窗口对象
        JFrame frame = new JFrame("俄罗斯方块");
        //创建游戏界面
        Tetris panel = new Tetris();
        //将面板嵌入到窗口中
        frame.add(panel);
        frame.setVisible(true);
        frame.setSize(810,940);
        //设置窗口居中
        frame.setLocationRelativeTo(null);
        //设置窗口 关闭时程序中止
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //游戏主要逻辑封装在方法内
        panel.start();
    }

}
