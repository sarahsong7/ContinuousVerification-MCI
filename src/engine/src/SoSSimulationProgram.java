import agents.Patient;
import core.World;
import misc.Time;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

// Add parts of key

// 참고: http://www.java-gaming.org/topics/basic-game/21919/view.html
// SoSSimulationProgram 클래스에서 참조하고 있는 클래스: Time, SoSObject, SoSScenario

/**
 * Project: NewSimulator
 * Created by IntelliJ IDEA
 * Author: Sumin Park <smpark@se.kaist.ac.kr>
 * Github: https://github.com/sumin0407/NewSimulator.git
 */

public class SoSSimulationProgram implements Runnable, KeyListener {
    final int MAX_SIMULATION_COUNT = 1;
    final int ADDED_PATIENTS= 20;
    final int ADDED_FF1 = 6;
    final int ADDED_FF2 = 3;
    final double SUCCESS_RATE = 0.7;
    final boolean isCV = true;
    final boolean isSlice = true;
    double OMIndA = 1.0;
    double risk = 0.00;
    double riskAdd = 0.0011;
    final double OMIndR = 1.0;

    final int SIMULATION_WIDTH = 910;
    final int SIMULATION_HEIGHT = 910;
//    final int CONSOLE_WIDTH = 200;

    JFrame frame;
    Canvas canvas;
    BufferStrategy bufferStrategy;
    boolean isExpert = true;


    public SoSSimulationProgram(){
        frame = new JFrame("SimulationEngine");

        JPanel panel = (JPanel) frame.getContentPane();
//        panel.setPreferredSize(new Dimension(SIMULATION_WIDTH + CONSOLE_WIDTH, SIMULATION_HEIGHT));
        panel.setPreferredSize(new Dimension(SIMULATION_WIDTH , SIMULATION_HEIGHT));
        //panel.setLayout(null);
        panel.setLayout(new FlowLayout());

        // 시뮬레이션 콘솔 GUI
//        Button button = new Button("Console GUI Here");
//        button.setPreferredSize(new Dimension(CONSOLE_WIDTH, SIMULATION_HEIGHT));
//        panel.add(button);
        // 시뮬레이션 콘솔 GUI

        // 시뮬레이션 화면
        canvas = new Canvas();
        canvas.setBounds(0, 0, SIMULATION_WIDTH, SIMULATION_HEIGHT);
        canvas.setIgnoreRepaint(true);

        panel.add(canvas);
        // 시뮬레이션 화면

        // 마우스 이벤트... 알아보기
        canvas.addMouseListener(new MouseControl());
        canvas.addKeyListener(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

        canvas.createBufferStrategy(2);
        bufferStrategy = canvas.getBufferStrategy();

        canvas.requestFocus();


        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                //System.out.println("jdialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                running = false;
                clear();
            }
        });

    }

    ArrayList<Integer> savedPatientsPerWorld =new ArrayList<Integer>();

    boolean pause = false;

    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_S) {
            System.out.println("Stop button pressed!");
            pause = true;

        }
//        if (e.getKeyCode() == KeyEvent.VK_G) {
//            pause = false;
//        }
        //restart
        if (e.getKeyCode() == KeyEvent.VK_I) {
            pause = false;
            init();
        }
    }
    public void keyReleased(KeyEvent e)
    {}
    public void keyTyped(KeyEvent e)
    {}


    private class MouseControl extends MouseAdapter{
        public void mouseClicked(MouseEvent e) {
            int a = 10;
        }
    }

    long desiredFPS = 120;
    long desiredDeltaLoop = (1000*1000*1000)/desiredFPS;

    boolean running = true;

    boolean stop = false;

    public void run(){
//        Scanner scan = new Scanner();
        long beginLoopTime;
        long endLoopTime;
        long currentUpdateTime = System.nanoTime();
        long lastUpdateTime;
        long deltaLoop;

        init();

        while(running){
            beginLoopTime = System.nanoTime();
            render();

            lastUpdateTime = currentUpdateTime;
            currentUpdateTime = System.nanoTime();
            if(!pause) {
                update((int) ((currentUpdateTime - lastUpdateTime) / (1000 * 1000)));
            } else { // pause
                frame.setVisible(false);
                if(isExpert) {
                    pause = false;
                    update((int) ((currentUpdateTime - lastUpdateTime) / (1000 * 1000)));

                    if (isCV) {

                        //여기다 이제 얼만큼 FF add 할지 다른 프로그램으로 정하고,
                        int numFF = world.fireFighterCounter;
                        System.out.println(numFF);
                        String strFF = "";
                        for (int i = 1; i<=numFF; i++) {
                            strFF+=(" numP"+i+" dead"+i);
                        }

                        int numPatient=0;// 현재 안고쳐진 환자수 구하기
                        for (Patient patient: world.patients) {
                            if (!patient.isSaved)
                                numPatient++;
                        }
                        try {
                            String result;
                            InputStream is;
                            System.out.println("IN");
                            String propStr = "echo P=? [(";
                            for (int i =1; i<numFF; i++) {
                                propStr += ("dead"+i+"+");
                            }
                            propStr += "dead"+numFF+"^<1) U (";
                            for (int i =1; i<numFF; i++) {
                                propStr += ("numP"+i+"+");
                            }
                            propStr += ("numP"+numFF+"^>MAX)] > prop.pctl &&");

                            if(isSlice) {
                                is = Runtime.getRuntime().exec("cmd.exe /c " +
                                        "cd C:\\cygwin64\\home\\Sarah\\prism\\classes && " +
                                        "java parser.PrismParser -s MCISoS_original.prism" + strFF + " > ..\\bin\\MCISoS_slice.prism &&" + // 1. 모델에서 소방관 수 만큼 slice,
                                        "cd ..\\bin &&" +
                                        propStr +
                                        "prism.bat MCISoS_slice.prism prop.pctl -prop 1 -sim -simmethod ci -const RISK="+risk+ " -simsamples 1000").getInputStream(); // 2. 모델에서 환자수 쓰기, 소방관 수 만큼 true
                            } else {
                                is = Runtime.getRuntime().exec("cmd.exe /c " +
                                        "cd C:\\cygwin64\\home\\Sarah\\prism\\bin && " +
                                        propStr +
                                        "prism.bat MCISoS_original.prism prop.pctl -prop 1 -sim -simmethod ci -const RISK="+risk+ " -simsamples 1000").getInputStream(); // 1. 모델에서 환자수 쓰기, 소방관 수만큼 true,
                            }
                            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                            while ((result =br.readLine()) != null) {
                                System.out.println(result);
                                if(result.startsWith("Result:")){ // 3. 검증 결과 읽기
                                    System.out.println(result);
                                    String[] value = result.split(" ");
                                    if (Double.parseDouble(value[1])<SUCCESS_RATE) {
                                        int addedFF = (int) (ADDED_FF1 * OMIndR);
                                        world.onAddFireFighter(world.frameCount + 1, addedFF);
                                    } else {
                                        int addedFF = (int) (ADDED_FF2 * OMIndR);
                                        world.onAddFireFighter(world.frameCount + 1, addedFF);
                                    }
                                }
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        world.verificationTime.add((System.nanoTime()-currentUpdateTime)/1000000000);
                        update((int) ((currentUpdateTime - lastUpdateTime) / (1000 * 1000)));
//                    expertMode();
                    }
                } else {
                    beginnerMode();
                }
                risk += riskAdd;
                frame.setVisible(true);
            }

            if (world.frameCount==600 || world.frameCount==700 || world.frameCount==800 || world.frameCount==900) {
                int numPatient=0;// 현재 안고쳐진 환자수 구하기
                for (Patient patient: world.patients) {
                    if (!patient.isSaved)
                        numPatient++;
                }
//                OMIndA = ((double) numPatient/world.patients.size() +OMIndA)/2;
                pause = true;
            }

            endLoopTime = System.nanoTime();
            deltaLoop = endLoopTime - beginLoopTime;

            if (world.frameCount >4000 && simulation_count >=MAX_SIMULATION_COUNT) {
                running =false;
            }
//            if(deltaLoop > desiredDeltaLoop){
//                //Do nothing. We are already late.
//            }else{
//                try{
//                    Thread.sleep((desiredDeltaLoop - deltaLoop)/(1000*1000));
//                }catch(InterruptedException e){
//                    //Do nothing
//                }
//            }
        }
    }

    private void render() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
        g.clearRect(0, 0, SIMULATION_WIDTH, SIMULATION_HEIGHT);
        render(g);
        g.dispose();
        bufferStrategy.show();
    }



    // misc.Time class implementation
    private static final class TimeImpl extends Time {
        public TimeImpl() {
            instance = this;
            deltaTime = 0;
            time = 0;
            frameCount = 0;
        }

        public void update(int deltaTime) {
            this.deltaTime = deltaTime;
            time += deltaTime;
            frameCount++;
        }
    }
    TimeImpl timeImpl = new TimeImpl();

    World world;
    protected void init() {
        world = new World();

    }

    /**
     * Rewrite this method for your game
     */
    // deltaTime 단위: 밀리초
    int time = 0;
    int simulation_count = 1;
    protected void update(int deltaTime){



        time += deltaTime;
        if(time >= Time.fromSecond(0.0f)) {
            timeImpl.update(deltaTime);
            world.update();
            if(world.isFinished() && simulation_count < MAX_SIMULATION_COUNT) {
                savedPatientsPerWorld.add(world.savedPatientCount);
                simulation_count++;
                System.out.println("simulation_count: "+simulation_count);
                world = new World();
            }
            time = 0;
        }
    }

    /**
     * Rewrite this method for your game
     */
    protected void render(Graphics2D g){
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, SIMULATION_WIDTH, SIMULATION_HEIGHT);
        world.render(g);
    }

    protected void clear() {
        world.setSavedPatients(savedPatientsPerWorld);
        world.clear();
    }

    public static void main(String [] args){

        SoSSimulationProgram simulationEngine = new SoSSimulationProgram();
        new Thread(simulationEngine).start();
    }

    private void expertMode() {
        System.out.print("Input command here: ");
        Scanner input = new Scanner(System.in);
        String command = input.next().toLowerCase();
        switch (command) {
            case "resume":
                pause = false;
                frame.setVisible(true);
                break;
            case "add":
                command = input.next().toLowerCase();
                // add firefighter 275 5
                switch (command) {
                    case "firefighter":
                        world.onAddFireFighter(input.nextInt(), input.nextInt());
                        break;
                    case "ambulance":
                        world.onAddAmbulance(input.nextInt(), input.nextInt());
                        break;
                    case "patient":
                        world.onAddPatient(input.nextInt(), input.nextInt());
                }

                break;
        }
    }

    private void beginnerMode() {
        String menu = "===== Menu\n" +
                "  1. Add\n" +
                "  2. Set\n" +
                "  3. Remove\n" +
                "  0. Resume\n" +
                "===== Input command: ";
        int command = getCommandOnBeginnerMode(menu);
        switch (command) {
            case 1:
                menu = "==== Add menu\n" +
                        "  1. Ambulance\n" +
                        "  2. FireFighter\n" +
                        "===== Input command: ";
                command = getCommandOnBeginnerMode(menu);
                switch (command) {
                    case 1: {
                        int frame = getCommandOnBeginnerMode("frame count: ");
                        int number = getCommandOnBeginnerMode("number of ambulance: ");
                        world.onAddAmbulance(frame, number);
                        break;
                    }
                    case 2: {
                        int frame = getCommandOnBeginnerMode("frame count: ");
                        int number = getCommandOnBeginnerMode("number of firefighter: ");
                        world.onAddFireFighter(frame, number);
                        break;
                    }
                }
                break;
            case 2:
                break;
            case 3:
                break;
            case 0:
                frame.setVisible(true);
                canvas.requestFocus();
                pause = false;
                break;
        }
    }

    int getCommandOnBeginnerMode(String menu) {
        System.out.print(menu);
        Scanner input = new Scanner(System.in);
        while(true) {
            if (input.hasNextInt()) {
                break;
            } else {
                System.out.println("Please enter number");
                System.out.println(input.next());
                System.out.print(menu);
            }
        }
        return input.nextInt();
    }

}
