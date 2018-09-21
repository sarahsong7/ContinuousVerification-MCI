package core;

import misc.Position;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public abstract class SoSObject <T extends SoSObject> {

    // 1. 여기서부터
    static Queue<SoSObject> all = null;
    static void initAll() {
        all = new LinkedList<>();
    }
    static void updateAll() {
        // 모든 오브젝트 업데이트
        all.add(null);
        while(true) {
            SoSObject object = all.poll();
            if(object == null) break;
            object.update();
            all.add(object);
        }
    }
    static void renderAll(Graphics2D g) {
        // 모든 오브젝트 렌더링
        all.add(null);
        while(true) {
            SoSObject object = all.poll();
            if(object == null) break;
            object.render(g);
            all.add(object);
        }
    }
    static void clearAll() {
        // 모든 오브젝트 정리
        all.stream().forEach(object -> object.clear());
    }
    // 1. 여기까지 다른 클래스로 빼도 됨. 그냥 이렇게 하고 싶었음. 설계따위

    protected String name;
    protected Position position;
    protected Queue<Msg> msgQueue = new LinkedList<>();
    protected BufferedImage image;

    boolean _canUpdate = true;
    boolean _canRender = true;

    public SoSObject() {
        assert all != null : "오브젝트를 생성하기 전에 SoSObject.initAll()을 호출할 것";
        all.add(this);
    }

    public abstract T init();

    public T canUpdate(boolean _canUpdate) {
        this._canUpdate = _canUpdate;
        return (T)this;
    }

    // 외부용
    public final void update() {
        if(_canUpdate) {
            onUpdate();
        }
    }
    // 상속용
    protected void onUpdate() {

    }

    // 외부용
    public final void render(Graphics2D g) {
        if(_canRender) {
            onRender(g);
        }
    }

    // 상속용
    protected void onRender(Graphics2D g) {
        int width = image.getWidth();
        int height = image.getHeight();
        g.drawImage(image, position.x * width, position.y * height, null);
    }

    public T clear() {
        position = null;
        return (T)this;
    }

    public void destroy() {
        clear();
        all.remove(this);
    }

    public T setPosition(Position position) {
        return setPosition(position.x, position.y);
    }
    public T setPosition(int x, int y) {
        Tile nextTile = Map.global.getTile(x, y);
        if(nextTile == null)
            return (T) this;

        if(position != null) {
            Tile tile = Map.global.getTile(position);
            if(tile != null) {
                tile.remove(this);
            }
        }
        position = new Position(x, y);

        nextTile.add(this);
        return (T)this;
    }

    protected T loadImage(String filepath) {
        try {
            image = ImageIO.read(new File(filepath));
        } catch(IOException e) {
            e.printStackTrace();
        }
        return (T)this;
    }
}