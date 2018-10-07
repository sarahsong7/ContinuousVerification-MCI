package core;

import agents.*;
import misc.Position;

import misc.Range;
import misc.Time;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import stimulus.*;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class World extends SoSObject{

    private final ArrayList<Scenario> scenarios = new ArrayList<>();

    public static int maxPatient = 100;
    public static int maxFireFighter = 8;
    public static final int maxHospital = 4;
    public static final int maxAmbulance = 4;
    public static final int maxSafeZone = 4;

    public Map map;
    public MsgRouter router;
    public ArrayList<Patient> patients = new ArrayList<>(maxPatient);
    public ArrayList<FireFighter> fireFighters = new ArrayList<FireFighter>(maxFireFighter);
    public ArrayList<Hospital> hospitals = new ArrayList<Hospital>(maxHospital);
    public ArrayList<SafeZone> safeZones = new ArrayList<>(maxSafeZone);
    public ArrayList<Ambulance> ambulances = new ArrayList<>(maxAmbulance);

    public static final String fireFighterPrefix = "FF";

    public World() {

        workbook = new XSSFWorkbook();
//        patientSheet = workbook.createSheet("patients");

        map = new Map();
        addChild(map);
        map.canUpdate(false);

        // 맵 생성 후에 라우터 생성해야함
        // 안그러면 널 에러
        router = new MsgRouter(this, workbook);
        addChild(router);

        createObjects();
        writeScenario();
    }

    private void createObjects() {
        createFireFighters();
        createSafeZones();
        createOrganization();
        createHospitals();
        createPatients();
        createAmbulances();
    }

    private void createPatients() {
        for (int i = 0; i < maxPatient; i++) {
            Patient patient = new Patient(this, "Patient" + (i + 1));
            patients.add(patient);
            patient.setStatus(Patient.Status.random());
            Position randomPosition = null;

            while(true) {
                randomPosition = GlobalRandom.nextPosition(Map.mapSize.width, Map.mapSize.height);
                boolean isSafeZone = false;
                for(SafeZone zone: safeZones) {
                    if(zone.isSafeZone(randomPosition)) {
                        isSafeZone = true;
                        break;
                    }
                }
                if(isSafeZone == false) break;
            }
            patient.setPosition(randomPosition);
            this.addChild(patient);
        }
    }

    private void createFireFighters() {
        Position[] positions = new Position[] {
                new Position(0, 0),
                new Position(Map.mapSize.width - 1, 0),
                new Position(Map.mapSize.width - 1, Map.mapSize.height - 1),
                new Position(0, Map.mapSize.height - 1)
        };
        for (int i = 0; i < maxFireFighter; i++) {
            FireFighter ff = new FireFighter(this, fireFighterPrefix + (i + 1));
            //ff.setPosition(0, 0);
            fireFighters.add(ff);

            ff.setPosition(positions[positionIndex++]);
            if(positionIndex >= 4)
                positionIndex = 0;
//            if(i == 0) {
//                addChild(ff.individualMap);
//            }
            addChild(ff);
        }
    }

    private void createHospitals() {
        for(int i = 0; i < maxHospital; ++i) {
            Hospital hospital = new Hospital(this, "Hospital" + (i + 1));
            hospitals.add(hospital);
            addChild(hospital);

            if(i == 0){
                hospital.setCapacity(20);
            } else {
                hospital.setCapacity(100);
            }
        }

        hospitals.get(0).setPosition(0, 0);
        hospitals.get(1).setPosition(Map.mapSize.width - 1, 0);
        hospitals.get(2).setPosition(0, Map.mapSize.height - 1);
        hospitals.get(3).setPosition(Map.mapSize.width - 1, Map.mapSize.height - 1);
    }

    private void createSafeZones() {
        for(int i = 0; i < maxSafeZone; ++i) {
            SafeZone safeZone = new SafeZone(this, "SafeZone" + (i + 1));
            safeZones.add(safeZone);
            addChild(safeZone);
        }

        safeZones.get(0).setPosition(new Position(Map.mapSize.width / 4, Map.mapSize.height / 4));
        safeZones.get(1).setPosition(new Position(3 * Map.mapSize.width / 4, Map.mapSize.height / 4));
        safeZones.get(2).setPosition(new Position(3 * Map.mapSize.width / 4, 3 * Map.mapSize.height / 4));
        safeZones.get(3).setPosition(new Position(Map.mapSize.width / 4, 3 * Map.mapSize.height / 4));
    }

    private void createAmbulances() {
        for(int i = 0; i < maxAmbulance; ++i) {
            Ambulance ambulance = new Ambulance(this, "Ambulance" + (i + 1));
            ambulances.add(ambulance);
            addChild(ambulance);
        }
    }

    private void createOrganization() {
        Organization organization = new Organization(this, "Organization");
        addChild(organization);
    }

    int frameCount = 0;
    public int savedPatientCount = 0;
    @Override
    public void onUpdate() {

        if(getPatientCount() == 0 && map.getUnvisitedTileCount() == 0) {
            canUpdate(false);
            //printPatientLog(true);
            //printFireFighetrLog(true);
            return;
        } else {
            //printPatientLog(false);
            //printFireFighetrLog(false);
            frameCount++;
            System.out.println("FrameCount: " + frameCount);
        }

        ArrayList<Scenario> mustRemove = new ArrayList<>();
        for(Scenario scenario: scenarios) {
            if(scenario.frame == Time.getFrameCount()) {
                scenario.execute();
                mustRemove.add(scenario);
            }
        }
        scenarios.removeAll(mustRemove);

    }

    Workbook workbook = new XSSFWorkbook();
    Sheet patientSheet = workbook.createSheet("patients");
    private void printPatientLog(boolean isFinish) {

        if(frameCount == 0) {
            Row row = patientSheet.createRow(frameCount);
            Cell frameCountCell = row.createCell(0);
            Cell savedPatientCell = row.createCell(1);

            frameCountCell.setCellValue("frame count");
            savedPatientCell.setCellValue("number of rescued patients");
        }

        Row row = patientSheet.createRow(frameCount + 1);
        Cell frameCountCell = row.createCell(0);
        Cell savedPatientCell = row.createCell(1);

        frameCountCell.setCellValue(frameCount);
        savedPatientCell.setCellValue(savedPatientCount);
    }

    Sheet fireFighterSheet = workbook.createSheet("fire fighters");
    private void printFireFighetrLog(boolean isFinish) {

        // i * 2
        // i * 2 + 1

        if(frameCount == 0) {
            Row row = fireFighterSheet.createRow(frameCount);
            Cell frameCountCell = row.createCell(0);
            frameCountCell.setCellValue("frame count");
            Cell[] positionCells = new Cell[maxFireFighter];

            for(int i = 0; i < maxFireFighter; ++i) {
                Cell currentCell = row.createCell(i * 2 + 1);
                String position = fireFighters.get(i).position.toString();
                currentCell.setCellValue("FF" + (i + 1) + " pos");

                currentCell = row.createCell(  i * 2 + 2);
                currentCell.setCellValue("FF" + (i + 1) + " Status");
            }
        }

        Row row = fireFighterSheet.createRow(frameCount + 1);
        Cell frameCountCell = row.createCell(0);
        frameCountCell.setCellValue(frameCount);
        Cell[] positionCells = new Cell[maxFireFighter];

        for(int i = 0; i < maxFireFighter; ++i) {
            Cell currentCell = row.createCell(i * 2 + 1);

            String position = fireFighters.get(i).position.toString();
            currentCell.setCellValue(position);

            currentCell = row.createCell(  i * 2 + 2);
            currentCell.setCellValue(fireFighters.get(i).currentAction.name);
            //currentCell.setCellValue(fireFighters.get(i).getState().toString());
        }

        if(isFinish) {
            row = fireFighterSheet.createRow(frameCount + 2);
            frameCountCell = row.createCell(0);
            frameCountCell.setCellValue("total distance");
            positionCells = new Cell[maxFireFighter];

            for(int i = 0; i < maxFireFighter; ++i) {
                Cell currentCell = row.createCell(i * 2 + 1);
                positionCells[i] = currentCell;

                positionCells[i].setCellValue(fireFighters.get(i).totalDistance);
            }
        }
    }

    @Override
    public void onRender(Graphics2D g) {
        Rectangle rect = g.getDeviceConfiguration().getBounds();
        g.setColor(new Color(100, 100, 100));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);

        g.setColor(Color.red);
        g.setFont(new Font("default", Font.BOLD, 16));
        String strFrameCount = "frameCount: " + frameCount;
        g.drawChars(strFrameCount.toCharArray(), 0, strFrameCount.length(), 600, 700);
    }

    public Map getMap() {
        return map;
    }


    public int getPatientCount() {
        int count = 0;
        for(SoSObject child: children) {
            if(child instanceof Patient) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void clear() {
        long nano = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(nano);
        try (OutputStream fileOut = new FileOutputStream("log/" + date +".xlsx")) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean contains(SoSObject object) {
        return children.contains(object);
    }

    int positionIndex = 0;
    void addFireFighter() {
        maxFireFighter++;
        Position[] positions = new Position[]{
                new Position(0, 0),
                new Position(Map.mapSize.width - 1, 0),
                new Position(Map.mapSize.width - 1, Map.mapSize.height - 1),
                new Position(0, Map.mapSize.height - 1)
        };

        FireFighter ff = new FireFighter(this, fireFighterPrefix + (fireFighters.size() + 1));
        fireFighters.add(ff);

        ff.setPosition(positions[positionIndex++]);
        if (positionIndex >= 4)
            positionIndex = 0;
        addChild(ff);
    }

    public void addPatient(Position position) {
        maxPatient++;
        Patient patient = new Patient(this, "Patient" + (patients.size() + 1));
        patients.add(patient);
        patient.setStatus(Patient.Status.random());
        if(position == null) {
            Position randomPosition = null;

            while (true) {
                randomPosition = GlobalRandom.nextPosition(Map.mapSize.width, Map.mapSize.height);
                boolean isSafeZone = false;
                for (SafeZone zone : safeZones) {
                    if (zone.isSafeZone(randomPosition)) {
                        isSafeZone = true;
                        break;
                    }
                }
                if (isSafeZone == false) break;
            }

            patient.setPosition(randomPosition);
        } else {
            patient.setPosition(position);
        }
        this.addChild(patient);

        ArrayList<Patient> data = new ArrayList<>();
        data.add(patient);


        for(FireFighter fireFighter: fireFighters) {
            Msg msg = new Msg()
                    .setFrom(fireFighterPrefix)
                    .setTo(fireFighter.name)
                    .setTitle("patientsMemory")
                    .setData(data);
            router.route(msg);
        }
    }







//    private class SetSightRange extends Scenario {
//
//        FireFighter target;
//        int value;
//        public SetSightRange(FireFighter target, int frame, int value) {
//            super(frame);
//            this.target = target;
//            this.value = value;
//        }
//
//        public SetSightRange(int frame, int value) {
//            super(frame);
//            this.value = value;
//        }
//        @Override
//        public void execute() {
//            if(target != null) {
//                target.defaultSightRange = value;
//            } else {
//                fireFighters.forEach(fireFighter -> fireFighter.defaultSightRange = value);
//            }
//        }
//    }
//
//    private class AddFireFighter extends Scenario {
//        public AddFireFighter(int frame) {
//            super(frame);
//        }
//
//        @Override
//        public void execute() {
//            FireFighter ff = new FireFighter(World.this, fireFighterPrefix + (fireFighters.size() + 1));
//            fireFighters.add(ff);
//            ff.sightRange = 100;
//            ff.setPosition(34, 33);
//            addChild(ff);
//        }
//    }
//
//    private class SetTileMoveDelay extends Scenario {
//
//        Range range;
//        float factor;
//
//        public SetTileMoveDelay(int frame, Range range, int factor) {
//            super(frame);
//            this.range = range;
//            this.factor = factor;
//        }
//
//        @Override
//        public void execute() {
//            for(int y = range.left; y <= range.right; ++y) {
//                for(int x = range.top; x <= range.bottom; ++x) {
//                    map.getTile(x, y).moveDelayFactor = factor;
//                }
//            }
//        }
//    }
//
//    private class SetTileSightRange extends Scenario {
//
//        Range range;
//        float factor;
//
//        public SetTileSightRange(int frame, Range range, float factor) {
//            super(frame);
//            this.range = range;
//            this.factor = factor;
//        }
//
//        @Override
//        public void execute() {
//            for(int y = range.left; y <= range.right; ++y) {
//                for(int x = range.top; x <= range.bottom; ++x) {
//                    map.getTile(x, y).sightRangeFactor = factor;
////                    map.getTile(x, y).applySightRange = true;
////                    map.getTile(x, y).sightRange = value;
//                }
//            }
//        }
//    }

    private void writeScenario() {

        ArrayList<String> firefighterNames = new ArrayList<>();
        for(int i = 0; i < maxFireFighter; ++i) {
            firefighterNames.add(fireFighterPrefix + (i + 1));
        }

        ArrayList<String> AmbulanceNames = new ArrayList<>();
        for(int i = 0; i < maxAmbulance; ++i) {
            firefighterNames.add("Ambulance" + (i + 1));
        }

        // TODO: moveDelay
        //scenarios.add(new Scenario(this, 100, "FF1", "moveDelay", 30));                                       // 특정 frame count 이후 특정 FF의 move speed 변경
        //scenarios.add(new Scenario(this, 100, firefighterNames, "moveDelay", 10));                            // 특정 frame count 이후 전체 FF의 move speed 변경
        //scenarios.add(new Scenario(this, 10, new Range(0, 0, 10, 10), "moveDelayFactor", 10.0f));             // 특정 frame count 이후 특정 구역에서의 move speed 변경
        //scenarios.add(new Scenario(this, 100, AmbulanceNames, "moveDelay", 10));                              // 특정 frame count 이후 Ambulance 전체 move speed 변경

//        scenarios.add(new MoveDelayScenario(this, 100, "FF1", 30));
//        scenarios.add(new MoveDelayScenario(this, 100, firefighterNames, 30));
//        scenarios.add(new MoveDelayScenario(this, 100, new Range(0, 0, 10, 10), 10.0f));
//        scenarios.add(new MoveDelayScenario(this, 100, AmbulanceNames, 10));                              // 특정 frame count 이후 Ambulance 전체 move speed 변경

        // TODO: sightRange
        //scenarios.add(new Scenario(this, 100, "FF1", "defaultSightRange", 30));                               // 특정 frame count 이후 특정 FF의 sight range 변화
        //scenarios.add(new Scenario(this, 100, firefighterNames, "defaultSightRange", 30));                    // 특정 frame count 이후 전체 FF의 sight range 변화
        //scenarios.add(new Scenario(this, 10, new Range(0, 0, 10, 10), "sightRangeFactor", 100.0f));           // 특정 frame count 이후 특정 구역의 sight range 변화

//        scenarios.add(new SightRangeScenario(this, 100, "FF1", 30));                               // 특정 frame count 이후 특정 FF의 sight range 변화
//        scenarios.add(new SightRangeScenario(this, 100, firefighterNames, 30));                    // 특정 frame count 이후 전체 FF의 sight range 변화
//        scenarios.add(new SightRangeScenario(this, 10, new Range(0, 0, 10, 10), 100.0f));           // 특정 frame count 이후 특정 구역의 sight range 변화

//        // TODO: communication (1 to 1 casting)
//        scenarios.add(new CommunicationScenario(this, 10, "ALL_DELAY", 300));
//        scenarios.add(new CommunicationScenario(this, 500,  "ALL_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "TO_ORG_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "TO_ORG_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "FROM_ORG_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "FROM_ORG_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "FF_TO_ORG_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "FF_TO_ORG_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "ORG_TO_FF_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "ORG_TO_FF_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "AMB_TO_ORG_DELAY", 300));
//        scenarios.add(new CommunicationScenario(this, 500,  "AMB_TO_ORG_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "ORG_TO_AMB_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "ORG_TO_AMB_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "SZ_TO_ORG_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "SZ_TO_ORG_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "ORG_TO_SZ_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "ORG_TO_SZ_DELAY", 0));
//
//        scenarios.add(new CommunicationScenario(this, 10, "FF_RANGECAST_DELAY", 130));
//        scenarios.add(new CommunicationScenario(this, 200,  "FF_RANGECAST_DELAY", 0));


        scenarios.add(new FireFighterToPatientScenario(this, 100, "FF1"));

        scenarios.add(new LambdaScenario(this, 100, this::addFireFighter));
    }



}
