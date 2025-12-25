package project;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.renderer.queue.RenderQueue;

public class EDITOR extends BaseAppState implements ActionListener {

    private SimpleApplication app;
    private Node rootNode;
    private Node editorNode;
    private Node levelNode;
    private BulletAppState bulletAppState;
    
    private Vector3f startPoint;
    private Geometry currentLineGeom;
    private float gridSize = 1.0f;
    private float wallHeight = 3.0f;
    private float wallThickness = 0.2f;

    private ColorRGBA[] wallColors = new ColorRGBA[]{
        new ColorRGBA(0.4f, 0.1f, 0.1f, 1.0f), // Dark Red
        new ColorRGBA(0.1f, 0.1f, 0.4f, 1.0f), // Dark Blue
        new ColorRGBA(0.1f, 0.4f, 0.1f, 1.0f), // Dark Green
        new ColorRGBA(0.4f, 0.4f, 0.1f, 1.0f), // Dark Yellow
        new ColorRGBA(0.4f, 0.1f, 0.4f, 1.0f), // Dark Purple
        new ColorRGBA(0.1f, 0.4f, 0.4f, 1.0f)  // Dark Teal
    };

    public EDITOR(Node levelNode, BulletAppState bulletAppState) {
        this.levelNode = levelNode;
        this.bulletAppState = bulletAppState;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        this.editorNode = new Node("EditorNode");
        
        createGrid();
        setupInput();
    }

    private void createGrid() {
        Node gridNode = new Node("Grid");
        Material gridMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        gridMat.setColor("Color", new ColorRGBA(1, 1, 1, 0.4f)); 
        gridMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        gridMat.getAdditionalRenderState().setDepthWrite(false);
        gridNode.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        int lines = 50;
        float size = lines * gridSize;
        float yOffset = 0.05f; // Slightly higher to ensure visibility
        for (int i = -lines; i <= lines; i++) {
            // X lines
            Line lineX = new Line(new Vector3f(i * gridSize, yOffset, -size), new Vector3f(i * gridSize, yOffset, size));
            Geometry gX = new Geometry("GridLineX", lineX);
            gX.setMaterial(gridMat);
            gridNode.attachChild(gX);
            
            // Z lines
            Line lineZ = new Line(new Vector3f(-size, yOffset, i * gridSize), new Vector3f(size, yOffset, i * gridSize));
            Geometry gZ = new Geometry("GridLineZ", lineZ);
            gZ.setMaterial(gridMat);
            gridNode.attachChild(gZ);
        }
        editorNode.attachChild(gridNode);
    }

    private void setupInput() {
        InputManager inputManager = app.getInputManager();
        inputManager.addMapping("Draw", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "Draw");
    }

    @Override
    protected void cleanup(Application app) {
        InputManager inputManager = app.getInputManager();
        inputManager.deleteMapping("Draw");
        inputManager.removeListener(this);
    }

    @Override
    protected void onEnable() {
        rootNode.attachChild(editorNode);
        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setDragToRotate(true);
        app.getFlyByCamera().setMoveSpeed(20f);
        app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        editorNode.removeFromParent();
        if (currentLineGeom != null) {
            currentLineGeom.removeFromParent();
            currentLineGeom = null;
        }
        startPoint = null;
    }

    @Override
    public void update(float tpf) {
        if (startPoint != null) {
            Vector3f endPoint = getSnappedPoint();
            updateVisualLine(startPoint, endPoint);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Draw")) {
            if (isPressed) {
                startPoint = getSnappedPoint();
            } else {
                if (startPoint != null) {
                    Vector3f endPoint = getSnappedPoint();
                    if (startPoint.distance(endPoint) > 0.1f) {
                        createWall(startPoint, endPoint);
                    }
                    startPoint = null;
                    if (currentLineGeom != null) {
                        currentLineGeom.removeFromParent();
                        currentLineGeom = null;
                    }
                }
            }
        }
    }

    private Vector3f getSnappedPoint() {
        Vector3f rawPos = getMouseWorldPoint();
        Vector3f edgeSnapped = snapToNearestEdge(rawPos);
        if (edgeSnapped != null) {
            return edgeSnapped;
        }
        return snapToGrid(rawPos);
    }

    private Vector3f getMouseWorldPoint() {
        // For simplicity, we assume drawing on the XZ plane (y=0)
        Vector2f click2d = app.getInputManager().getCursorPosition();
        Vector3f click3d = app.getCamera().getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
        Vector3f dir = app.getCamera().getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
        
        if (Math.abs(dir.y) < 0.001f) return click3d; // horizontal ray or nearly so
        
        float t = -click3d.y / dir.y;
        if (t < 0) return click3d; // pointing away from plane
        return click3d.add(dir.mult(t));
    }

    private Vector3f snapToGrid(Vector3f pos) {
        float x = Math.round(pos.x / gridSize) * gridSize;
        float z = Math.round(pos.z / gridSize) * gridSize;
        return new Vector3f(x, 0, z);
    }

    private Vector3f snapToNearestEdge(Vector3f pos) {
        // Basic snapping to existing wall endpoints
        float snapThreshold = 0.5f;
        Vector3f nearest = null;
        float minDist = snapThreshold;

        for (com.jme3.scene.Spatial s : levelNode.getChildren()) {
            if (s instanceof Geometry && s.getUserData("start") != null) {
                Vector3f sStart = (Vector3f) s.getUserData("start");
                Vector3f sEnd = (Vector3f) s.getUserData("end");
                
                float dStart = pos.distance(sStart);
                if (dStart < minDist) {
                    minDist = dStart;
                    nearest = sStart;
                }
                
                float dEnd = pos.distance(sEnd);
                if (dEnd < minDist) {
                    minDist = dEnd;
                    nearest = sEnd;
                }
            }
        }
        return nearest;
    }

    private void updateVisualLine(Vector3f start, Vector3f end) {
        if (currentLineGeom != null) {
            currentLineGeom.removeFromParent();
        }
        Line line = new Line(start, end);
        currentLineGeom = new Geometry("CurrentLine", line);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        currentLineGeom.setMaterial(mat);
        editorNode.attachChild(currentLineGeom);
    }

    private void createWall(Vector3f start, Vector3f end) {
        Vector3f center = start.add(end).divide(2f);
        center.y = wallHeight / 2f;
        
        float length = start.distance(end);
        Box box = new Box(length / 2f, wallHeight / 2f, wallThickness / 2f);
        Geometry wall = new Geometry("Wall", box);
        
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA color = wallColors[FastMath.nextRandomInt(0, wallColors.length - 1)];
        mat.setColor("Color", color);
        wall.setMaterial(mat);
        
        wall.setLocalTranslation(center);
        
        // Rotate wall to align with start-end line
        Vector3f direction = end.subtract(start).normalizeLocal();
        float angle = FastMath.atan2(direction.z, direction.x);
        wall.rotate(0, -angle, 0);
        
        wall.setUserData("start", start.clone());
        wall.setUserData("end", end.clone());
        
        // Add physics
        RigidBodyControl wallPhys = new RigidBodyControl(0);
        wall.addControl(wallPhys);
        bulletAppState.getPhysicsSpace().add(wallPhys);
        
        levelNode.attachChild(wall);
    }
}
