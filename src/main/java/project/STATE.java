package project;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class STATE extends BaseAppState implements ActionListener {

    private SimpleApplication app;
    private Node rootNode;
    private Node gameNode;
    private Node levelNode;
    private BulletAppState bulletAppState;

    private BetterCharacterControl playerControl;
    private Node playerNode;
    private boolean isWalking = false;

    private boolean left = false, right = false, up = false, down = false;
    private Vector3f walkDirection = new Vector3f();

    public STATE(Node levelNode, BulletAppState bulletAppState) {
        this.levelNode = levelNode;
        this.bulletAppState = bulletAppState;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        this.gameNode = new Node("GameNode");

        playerNode = new Node("PlayerNode");
        playerControl = new BetterCharacterControl(0.5f, 1.8f, 80f);
        playerNode.addControl(playerControl);
        
        setupInput();
    }

    private void setupInput() {
        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("WalkLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("WalkRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("WalkUp", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("WalkDown", new KeyTrigger(KeyInput.KEY_S));
        
        inputManager.addListener(this, "WalkLeft", "WalkRight", "WalkUp", "WalkDown");
    }

    @Override
    protected void cleanup(Application app) {
        InputManager inputManager = app.getInputManager();
        inputManager.deleteMapping("WalkLeft");
        inputManager.deleteMapping("WalkRight");
        inputManager.deleteMapping("WalkUp");
        inputManager.deleteMapping("WalkDown");
        inputManager.removeListener(this);
    }

    @Override
    protected void onEnable() {
        rootNode.attachChild(gameNode);
        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setDragToRotate(false);
        app.getInputManager().setCursorVisible(false);
        
        // Initial mode is walking
        setWalking(true);
    }

    @Override
    protected void onDisable() {
        if (isWalking) {
            setWalking(false);
        }
        gameNode.removeFromParent();
    }

    public void setWalking(boolean walking) {
        this.isWalking = walking;
        if (isWalking) {
            // Start walking at current camera location
            // Subtract eye height so feet are on ground if cam is at eye level
            Vector3f feetPos = app.getCamera().getLocation().subtract(0, 1.6f, 0);
            playerControl.warp(feetPos);
            bulletAppState.getPhysicsSpace().add(playerControl);
            gameNode.attachChild(playerNode);
            app.getFlyByCamera().setMoveSpeed(0); // Disable flyCam movement
        } else {
            bulletAppState.getPhysicsSpace().remove(playerControl);
            playerNode.removeFromParent();
            app.getFlyByCamera().setMoveSpeed(20f); // Re-enable flyCam movement
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("WalkLeft")) left = isPressed;
        else if (name.equals("WalkRight")) right = isPressed;
        else if (name.equals("WalkUp")) up = isPressed;
        else if (name.equals("WalkDown")) down = isPressed;
    }

    @Override
    public void update(float tpf) {
        if (isWalking) {
            Vector3f camDir = app.getCamera().getDirection().clone().setY(0).normalizeLocal();
            Vector3f camLeft = app.getCamera().getLeft().clone().setY(0).normalizeLocal();
            walkDirection.set(0, 0, 0);
            if (left) walkDirection.addLocal(camLeft);
            if (right) walkDirection.addLocal(camLeft.negateLocal());
            if (up) walkDirection.addLocal(camDir);
            if (down) walkDirection.addLocal(camDir.negateLocal());
            
            if (walkDirection.lengthSquared() > 0) {
                walkDirection.normalizeLocal().multLocal(8f);
            }
            playerControl.setWalkDirection(walkDirection);
            
            app.getCamera().setLocation(playerNode.getWorldTranslation().add(0, 1.6f, 0));
        }
    }
}
