package project;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.system.AppSettings;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.material.Material;

public class PROJECT extends SimpleApplication implements ActionListener {

    private EDITOR editorState;
    private STATE gameState;
    private BulletAppState bulletAppState;
    private Node levelNode;

    public static void main(String[] args) {
        PROJECT app = new PROJECT();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Quake-like Engine");
        settings.setResolution(1280, 720);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        levelNode = new Node("LevelNode");
        rootNode.attachChild(levelNode);

        // Floor to draw on
        Box floorBox = new Box(150, 0.1f, 150);
        Geometry floor = new Geometry("Floor", floorBox);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.DarkGray);
        floor.setMaterial(floorMat);
        // Move floor down so top is at -0.1f, grid is at 0 to avoid Z-fighting
        floor.setLocalTranslation(0, -0.2f, 0);
        rootNode.attachChild(floor);
        
        // Add floor to physics
        RigidBodyControl floorPhys = new RigidBodyControl(0);
        floor.addControl(floorPhys);
        bulletAppState.getPhysicsSpace().add(floorPhys);

        editorState = new EDITOR(levelNode, bulletAppState);
        gameState = new STATE(levelNode, bulletAppState);

        stateManager.attach(gameState);

        // Start in game mode
        stateManager.detach(editorState);

        inputManager.addMapping("ToggleMode", new KeyTrigger(KeyInput.KEY_F1), new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addListener(this, "ToggleMode");
        
        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(0, 5, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("ToggleMode") && isPressed) {
            if (stateManager.hasState(editorState)) {
                stateManager.detach(editorState);
                stateManager.attach(gameState);
            } else {
                stateManager.detach(gameState);
                stateManager.attach(editorState);
            }
        }
    }
}
