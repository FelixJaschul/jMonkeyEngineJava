package project;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;

public class PROJECT extends SimpleApplication implements ActionListener {

    private EState e_state;
    private GState g_state;
    private BulletAppState b_state;
    private Node l_node;

    public static void main(String[] args) {
        PROJECT app = new PROJECT();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("TEST");
        settings.setResolution(1280, 720);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        b_state = new BulletAppState();
        stateManager.attach(b_state);

        l_node = new Node("LevelNode");
        rootNode.attachChild(l_node);

        Box floorBox = new Box(150, 0.1f, 150);
        Geometry floor = new Geometry("Floor", floorBox);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.DarkGray);
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(0, -0.2f, 0);
        rootNode.attachChild(floor);
        
        RigidBodyControl floorPhys = new RigidBodyControl(0);
        floor.addControl(floorPhys);
        b_state.getPhysicsSpace().add(floorPhys);

        e_state = new EState();
        g_state = new GState();

        stateManager.attach(g_state);
        inputManager.addMapping("ToggleMode", new KeyTrigger(KeyInput.KEY_F1), new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addListener(this, "ToggleMode");
        
        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(0, 5, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Crosshair
        Material chMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        chMat.setColor("Color", ColorRGBA.White);
        Geometry ch = new Geometry("Crosshair", new Quad(4, 4));
        ch.setMaterial(chMat);
        ch.setLocalTranslation((float) settings.getWidth() / 2 - 2, (float) settings.getHeight() / 2 - 2, 0);
        guiNode.attachChild(ch);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf)
    {
        if (name.equals("ToggleMode") && isPressed)
        {
            if (stateManager.hasState(e_state))
            {
                stateManager.detach(e_state);
                stateManager.attach(g_state);
                return;
            }
            if (!stateManager.hasState(g_state))
            {
                stateManager.detach(g_state);
                stateManager.attach(e_state);
                return;
            }
        }
    }

    private class EState extends BaseAppState implements ActionListener
    {
        private final Node editorNode = new Node("EditorNode");
        private Vector3f startPoint;
        private Geometry currentLineGeom;

        private final float gridSize = 1.0f,
                            wallHeight = 3.0f,
                            wallThickness = 0.2f;

        private final ColorRGBA[] colors =
        {
            ColorRGBA.DarkGray,
            ColorRGBA.Red,
            ColorRGBA.Blue,
            ColorRGBA.Green,
            ColorRGBA.Yellow,
            ColorRGBA.Magenta,
            ColorRGBA.Cyan
        };

        @Override
        protected void initialize(Application app)
        {
            createGrid();
        }

        private void createGrid()
        {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(1, 1, 1, 0.4f));
            mat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
            mat.getAdditionalRenderState().setDepthWrite(false);
            
            int lines = 50;
            float size = lines * gridSize;

            for (int i = -lines; i <= lines; i++)
            {
                Geometry gx =
                        new Geometry("GridX",
                                new Line(
                                    new Vector3f(i * gridSize, 0.05f, -size),
                                    new Vector3f(i * gridSize, 0.05f, size)
                                )
                        );

                gx.setMaterial(mat);
                editorNode.attachChild(gx);

                Geometry gz =
                        new Geometry("GridZ",
                            new Line(
                                    new Vector3f(-size, 0.05f, i * gridSize),
                                    new Vector3f(size, 0.05f, i * gridSize)
                            )
                        );

                gz.setMaterial(mat);
                editorNode.attachChild(gz);
            }
            editorNode.setQueueBucket(RenderQueue.Bucket.Transparent);
        }

        @Override
        protected void onEnable()
        {
            rootNode.attachChild(editorNode);

            inputManager.addMapping("Draw", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
            inputManager.addListener(this, "Draw");
            inputManager.setCursorVisible(false);

            flyCam.setDragToRotate(false);
            flyCam.setMoveSpeed(20f);
        }

        @Override
        protected void onDisable()
        {
            editorNode.removeFromParent();
            inputManager.deleteMapping("Draw");
            inputManager.removeListener(this);

            if (currentLineGeom != null) currentLineGeom.removeFromParent();

            startPoint = null;
        }

        @Override
        public void update(float tpf)
        {
            if (startPoint != null)
            {
                if (currentLineGeom != null) currentLineGeom.removeFromParent();

                currentLineGeom = new Geometry("Line", new Line(startPoint, getSnappedPoint()));
                Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                m.setColor("Color", ColorRGBA.Red);

                currentLineGeom.setMaterial(m);
                editorNode.attachChild(currentLineGeom);
            }
        }

        @Override
        public void onAction(String name, boolean isPressed, float tpf)
        {
            if (name.equals("Draw"))
            {
                if (isPressed) { startPoint = getSnappedPoint(); return; }
                if (startPoint != null)
                {
                    Vector3f end = getSnappedPoint();
                    if (startPoint.distance(end) > 0.1f) createWall(startPoint, end);
                    startPoint = null;

                    if (currentLineGeom != null)
                    {
                        currentLineGeom.removeFromParent();
                        currentLineGeom = null;
                    }
                }
            }
        }

        private Vector3f getSnappedPoint()
        {
            Vector2f center = new Vector2f(settings.getWidth() / 2f, settings.getHeight() / 2f);
            Vector3f origin = cam.getWorldCoordinates(center, 0f).clone();
            Vector3f dir    = cam.getWorldCoordinates(center, 1f).subtractLocal(origin).normalizeLocal();
            Vector3f pos    = origin.add(dir.mult((-origin.y / dir.y)));

            if (Math.abs(dir.y) < 0.001f) return origin;

            Vector3f nearest = null; float minDist = 0.5f;

            for (com.jme3.scene.Spatial s : l_node.getChildren())
            {
                Vector3f s1 = s.getUserData("s"), s2 = s.getUserData("e");
                if (s1 != null && pos.distance(s1) < minDist)
                {
                    minDist = pos.distance(s1);
                    nearest = s1;
                }
                if (s2 != null && pos.distance(s2) < minDist)
                {
                    minDist = pos.distance(s2);
                    nearest = s2;
                }
            }
            if (nearest != null) return nearest;
            return new Vector3f(Math.round(pos.x / gridSize) * gridSize, 0, Math.round(pos.z / gridSize) * gridSize);
        }

        private void createWall(Vector3f start, Vector3f end)
        {
            Geometry wall = new Geometry("Wall", new Box(start.distance(end) / 2f, wallHeight / 2f, wallThickness / 2f));
            Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            RigidBodyControl p = new RigidBodyControl(0);

            m.setColor("Color", colors[FastMath.nextRandomInt(0, colors.length - 1)].mult(0.5f));

            wall.setMaterial(m);
            wall.setLocalTranslation(start.add(end).divide(2f).setY(wallHeight / 2f));
            wall.rotate(0, -FastMath.atan2(end.z - start.z, end.x - start.x), 0);
            wall.setUserData("s", start.clone()); wall.setUserData("e", end.clone());
            wall.addControl(p);

            b_state.getPhysicsSpace().add(p);
            l_node.attachChild(wall);
        }

        @Override protected void cleanup(Application app) {}
    }

    private class GState extends BaseAppState implements ActionListener
    {
        private BetterCharacterControl player;
        private final Node playerNode = new Node();
        private boolean left, right, up, down;

        @Override
        protected void initialize(Application app)
        {
            player = new BetterCharacterControl(0.5f, 1.8f, 80f);
            playerNode.addControl(player);
        }

        @Override
        protected void onEnable()
        {
            inputManager.addMapping("L", new KeyTrigger(KeyInput.KEY_A));
            inputManager.addMapping("R", new KeyTrigger(KeyInput.KEY_D));
            inputManager.addMapping("U", new KeyTrigger(KeyInput.KEY_W));
            inputManager.addMapping("D", new KeyTrigger(KeyInput.KEY_S));
            inputManager.addListener(this, "L", "R", "U", "D");
            inputManager.setCursorVisible(false);

            flyCam.setDragToRotate(false);
            flyCam.setMoveSpeed(0);

            player.warp(cam.getLocation().subtract(0, 1.6f, 0));

            b_state.getPhysicsSpace().add(player);

            rootNode.attachChild(playerNode);
        }

        @Override
        protected void onDisable()
        {
            b_state.getPhysicsSpace().remove(player);

            playerNode.removeFromParent();

            inputManager.removeListener(this);
            inputManager.deleteMapping("L");
            inputManager.deleteMapping("R");
            inputManager.deleteMapping("U");
            inputManager.deleteMapping("D");

            flyCam.setMoveSpeed(20f);
        }

        @Override
        public void onAction(String name, boolean isPressed, float tpf)
        {
            switch (name)
            {
                case "L" -> left = isPressed;
                case "R" -> right = isPressed;
                case "U" -> up = isPressed;
                case "D" -> down = isPressed;
            }
        }

        @Override
        public void update(float tpf)
        {
            Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
            Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();
            Vector3f walk = new Vector3f();

            if (left) walk.addLocal(camLeft);
            if (right) walk.addLocal(camLeft.negateLocal());
            if (up) walk.addLocal(camDir);
            if (down) walk.addLocal(camDir.negateLocal());

            player.setWalkDirection(walk.normalizeLocal().multLocal(8f));
            cam.setLocation(playerNode.getWorldTranslation().add(0, 1.6f, 0));
        }

        @Override protected void cleanup(Application app) {}
    }
}
