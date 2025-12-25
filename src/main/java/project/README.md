# PROJECT.java - Quake-like Engine Documentation

This document explains the structure and logic of `PROJECT.java`, which implements a simple level editor and a physics-based game mode using jMonkeyEngine.

## 1. Application Setup and Main Entry Point

The application starts in the `main` method, where we configure the basic window settings.

```java
34:    public static void main(String[] args) {
35:        PROJECT app = new PROJECT();
36:        AppSettings settings = new AppSettings(true);
37:        settings.setTitle("TEST");
38:        settings.setResolution(1280, 720);
39:        app.setSettings(settings);
40:        app.start();
41:    }
```
*   **Lines 36-39**: We initialize `AppSettings` to set the window title to "TEST" and the resolution to 1280x720 pixels.

## 2. simpleInitApp - Initializing the World

The `simpleInitApp` method is called once when the application starts. It sets up the physics engine, the floor, the crosshair, and the initial state.

```java
43:    @Override
44:    public void simpleInitApp() {
45:        b_state = new BulletAppState();
46:        stateManager.attach(b_state);
47:
48:        l_node = new Node("LevelNode");
49:        rootNode.attachChild(l_node);
...
63:        e_state = new EState();
64:        g_state = new GState();
65:
66:        stateManager.attach(g_state);
67:        inputManager.addMapping("ToggleMode", new KeyTrigger(KeyInput.KEY_F1));
68:        inputManager.addListener(this, "ToggleMode");
...
74:        // Crosshair
75:        Material chMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
76:        chMat.setColor("Color", ColorRGBA.White);
77:        Geometry ch = new Geometry("Crosshair", new Quad(4, 4));
78:        ch.setMaterial(chMat);
79:        ch.setLocalTranslation((float) settings.getWidth() / 2 - 2, (float) settings.getHeight() / 2 - 2, 0);
80:        guiNode.attachChild(ch);
81:    }
```
*   **Lines 45-46**: Initializes `BulletAppState`, which handles physics simulations (collisions, gravity).
*   **Lines 48-49**: `l_node` is a container for all the walls created in the editor.
*   **Lines 63-66**: Creates the Editor (`EState`) and Game (`GState`) states and attaches the game state by default.
*   **Lines 67-68**: Maps the `F1` key to toggle between the editor and game modes.
*   **Lines 74-80**: Creates a tiny 4x4 white dot in the center of the screen to serve as a crosshair.

## 3. Mode Toggling

The `onAction` method handles the switching between the two application states.

```java
83:    @Override
84:    public void onAction(String name, boolean isPressed, float tpf)
85:    {
86:        if (name.equals("ToggleMode") && isPressed)
87:        {
88:            if (stateManager.hasState(e_state))
89:            {
90:                stateManager.detach(e_state);
91:                stateManager.attach(g_state);
92:                return;
93:            }
...
```
*   **Lines 88-100**: When `F1` is pressed, it checks which state is currently active, detaches it, and attaches the other one.

## 4. EState (Editor Mode)

`EState` is an inner class that handles the level editing logic.

### 4.1 Grid Creation
The grid helps the user align walls.

```java
130:        private void createGrid()
131:        {
...
137:            int lines = 50;
138:            float size = lines * gridSize;
139:
140:            for (int i = -lines; i <= lines; i++)
141:            {
...
163:            }
164:            editorNode.setQueueBucket(RenderQueue.Bucket.Transparent);
165:        }
```
*   **Lines 130-165**: Generates a series of line geometries to form a grid. It uses a transparent material and disables depth writing so it stays visible above the floor without flickering.

### 4.2 Drawing Walls
The editor allows drawing walls by clicking and dragging.

```java
192:        @Override
193:        public void update(float tpf)
194:        {
195:            if (startPoint != null)
196:            {
...
199:                currentLineGeom = new Geometry("Line", new Line(startPoint, getSnappedPoint()));
...
204:                editorNode.attachChild(currentLineGeom);
205:            }
206:        }
```
*   **Lines 193-206**: While the mouse button is held, it draws a temporary red line from the start point to the current snapped mouse position.

### 4.3 Snapping Logic
Snapping ensures that walls are perfectly aligned and connected.

```java
229:        private Vector3f getSnappedPoint()
230:        {
231:            Vector2f center = new Vector2f(settings.getWidth() / 2f, settings.getHeight() / 2f);
232:            Vector3f origin = cam.getWorldCoordinates(center, 0f).clone();
233:            Vector3f dir    = cam.getWorldCoordinates(center, 1f).subtractLocal(origin).normalizeLocal();
234:            Vector3f pos    = origin.add(dir.mult((-origin.y / dir.y)));
...
254:            if (nearest != null) return nearest;
255:            return new Vector3f(Math.round(pos.x / gridSize) * gridSize, 0, Math.round(pos.z / gridSize) * gridSize);
256:        }
```
*   **Lines 231-234**: Uses raycasting from the crosshair (screen center) to find the point on the ground (y=0).
*   **Lines 238-254**: **Edge Snapping**: Iterates through existing walls to see if the cursor is near an endpoint. If it's within 0.5 units, it snaps to that endpoint.
*   **Line 255**: **Grid Snapping**: If no edge is nearby, it rounds the position to the nearest grid unit (1.0).

### 4.4 Wall Creation
When the mouse button is released, a 3D wall is generated.

```java
258:        private void createWall(Vector3f start, Vector3f end)
259:        {
260:            Geometry wall = new Geometry("Wall", new Box(start.distance(end) / 2f, wallHeight / 2f, wallThickness / 2f));
...
264:            m.setColor("Color", colors[FastMath.nextRandomInt(0, colors.length - 1)].mult(0.5f));
...
268:            wall.rotate(0, -FastMath.atan2(end.z - start.z, end.x - start.x), 0);
...
270:            wall.addControl(p);
271:
272:            b_state.getPhysicsSpace().add(p);
273:            l_node.attachChild(wall);
274:        }
```
*   **Line 260**: Creates a `Box` shape with a length based on the distance between start and end points.
*   **Line 264**: Assigns a random dark color from a predefined palette.
*   **Line 268**: Rotates the wall to align with the vector between the start and end points.
*   **Lines 270-272**: Adds a `RigidBodyControl` to the wall so it has physical collisions.

## 5. GState (Game Mode)

`GState` handles character movement and physics.

### 5.1 Player Physics
We use `BetterCharacterControl` for first-person movement.

```java
288:            player = new BetterCharacterControl(0.5f, 1.8f, 80f);
289:            playerNode.addControl(player);
```
*   **Line 288**: Creates a capsule-shaped physical character (radius 0.5, height 1.8, mass 80).

### 5.2 Movement Logic
Movement is calculated relative to the camera direction.

```java
341:        @Override
342:        public void update(float tpf)
343:        {
344:            Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
345:            Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();
...
352:            player.setWalkDirection(walk.normalizeLocal().multLocal(8f));
353:            cam.setLocation(playerNode.getWorldTranslation().add(0, 1.6f, 0));
354:        }
```
*   **Lines 344-345**: Gets the camera's forward and left vectors but ignores the vertical component (Y) so the player doesn't fly up when looking up.
*   **Line 352**: Sets the walking direction for the physics character.
*   **Line 353**: Updates the camera position to match the player's "eyes" (1.6 units above the ground).
