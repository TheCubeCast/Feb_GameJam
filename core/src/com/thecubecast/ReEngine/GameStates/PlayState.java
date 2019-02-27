// GameState that tests new mechanics.

package com.thecubecast.ReEngine.GameStates;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.thecubecast.ReEngine.Data.Cube;
import com.thecubecast.ReEngine.Data.GameStateManager;
import com.thecubecast.ReEngine.Data.ParticleHandler;
import com.thecubecast.ReEngine.Data.TkMap.TkMap;
import com.thecubecast.ReEngine.Graphics.Scene2D.UIFSM;
import com.thecubecast.ReEngine.Graphics.Scene2D.UI_state;
import com.thecubecast.ReEngine.Graphics.ScreenShakeCameraController;
import com.thecubecast.ReEngine.worldObjects.*;
import com.thecubecast.ReEngine.worldObjects.AI.Enemy;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.FlatTiledGraph;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.FlatTiledNode;
import com.thecubecast.ReEngine.worldObjects.EntityPrefabs.Hank;
import com.thecubecast.ReEngine.worldObjects.EntityPrefabs.Pawn;

import java.util.ArrayList;
import java.util.List;

public class PlayState extends DialogStateExtention {

    //GUI
    UIFSM UI;

    //Camera
    OrthographicCamera GuiCam;
    public static OrthographicCamera camera;
    ScreenShakeCameraController shaker;

    //Particles
    public static ParticleHandler Particles;

    //GameObjects
    public HackSlashPlayer player;
    private List<Cube> Collisions = new ArrayList<>();
    public static List<WorldObject> Entities = new ArrayList<>();

    TkMap WorldMap;

    //AI
    FlatTiledGraph MapGraph;

    Enemy tempEnemy;

    public PlayState(GameStateManager gsm) {
        super(gsm);
    }

    public void init() {

        WorldMap = new TkMap("Saves/World.cube");

        player = new HackSlashPlayer(13 * 16, 1 * 16, gsm);

        Entities.add(player);

        //Setup Dialog Instance
        MenuInit(gsm.UIWidth, gsm.UIHeight);

        gsm.DiscordManager.setPresenceDetails("topdown Demo - Level 1");
        gsm.DiscordManager.setPresenceState("In Game");
        gsm.DiscordManager.getPresence().largeImageText = "Level 1";
        gsm.DiscordManager.getPresence().startTimestamp = System.currentTimeMillis() / 1000;

        //Camera setup
        camera = new OrthographicCamera();
        GuiCam = new OrthographicCamera();
        camera.setToOrtho(false, GameStateManager.WorldWidth, GameStateManager.WorldHeight);
        GuiCam.setToOrtho(false, GameStateManager.UIWidth, GameStateManager.UIHeight);
        shaker = new ScreenShakeCameraController(camera);

        UI = new UIFSM(GuiCam, gsm);
        UI.inGame = true;
        UI.setState(UI_state.InGameHome);
        UI.setVisable(false);

        //Particles
        Particles = new ParticleHandler();

        MapGraph = new FlatTiledGraph(WorldMap);

        tempEnemy = new Pawn("[PURPLE]Pawn", 280,316,0, new Vector3(8,8,16), 1, 100, NPC.intractability.Silent, false, MapGraph, gsm);

        Entities.add(tempEnemy);

        Entities.add(new Hank(450, 300, 0) {
            @Override
            public void interact() {
                AddDialog("Hank","Hey, you can talk to me now?!");
                AddDialog("Hank","I'm no longer {COLOR=purple}{WAVE}alone{ENDWAVE}{CLEARCOLOR}!!!");
            }
        });

    }

    public void update() {

        Particles.Update();

        for (int i = 0; i < Entities.size(); i++) {
            if (Entities.get(i) instanceof Enemy) {
                ((Enemy)Entities.get(i)).update(Gdx.graphics.getDeltaTime(), Collisions, player);
            } else {
                Entities.get(i).update(Gdx.graphics.getDeltaTime(), Collisions);
            }
        }

        cameraUpdate(player, camera, Entities,0,0, WorldMap.getWidth()*WorldMap.getTileSize(), WorldMap.getHeight()*WorldMap.getTileSize());

        handleInput();
    }

    public void draw(SpriteBatch g, int height, int width, float Time) {

        shaker.update(gsm.DeltaTime);
        g.setProjectionMatrix(shaker.getCombinedMatrix());

        Rectangle drawView = new Rectangle(camera.position.x - camera.viewportWidth / 2 - camera.viewportWidth / 4, camera.position.y - camera.viewportHeight / 2 - camera.viewportHeight / 4, camera.viewportWidth + camera.viewportWidth / 4, camera.viewportHeight + camera.viewportHeight / 4);

        g.setShader(null);
        g.begin();

        WorldMap.Draw(camera, g);

        //Block of code renders all the entities
        WorldObjectComp entitySort = new WorldObjectComp();
        WorldObjectCompDepth entitySortz = new WorldObjectCompDepth();
        Entities.sort(entitySort);
        Entities.sort(entitySortz);
        for (int i = 0; i < Entities.size(); i++) {
            if (drawView.overlaps(new Rectangle(Entities.get(i).getPosition().x, Entities.get(i).getPosition().y, Entities.get(i).getSize().x, Entities.get(i).getSize().y))) {
                Entities.get(i).draw(g, Time);
            }
        }

        //Renders my favorite little debug stuff
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) { //KeyHit
            gsm.Cursor = GameStateManager.CursorType.Question;
        } else {
            gsm.Cursor = GameStateManager.CursorType.Normal;
        }

        //Particles
        Particles.Draw(g);

        //Renders the GUI for entities
        for (int i = 0; i < Entities.size(); i++) {
            if (Entities.get(i) instanceof NPC) {
                NPC Entitemp = (NPC) Entities.get(i);
                if (drawView.overlaps(new Rectangle(Entitemp.getPosition().x, Entitemp.getPosition().y, Entitemp.getSize().x, Entitemp.getSize().y))) {
                    ((NPC) Entities.get(i)).drawGui(g, Time);
                }
            }
        }

        g.end();

        //DEBUG CODE
        gsm.Render.debugRenderer.setProjectionMatrix(camera.combined);
        gsm.Render.debugRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (GameStateManager.Debug) {

            //Gonna make a debug renderer for the MapGraph, or i guess now? changing that fixed the crashing

            for (int y = 0; y < WorldMap.getHeight(); y++) {
                for (int x = 0; x < WorldMap.getWidth(); x++) {
                    switch (MapGraph.getNode(x, y).type) {
                        case FlatTiledNode.GROUND:
                            gsm.Render.debugRenderer.setColor(Color.GREEN);
                            gsm.Render.debugRenderer.rect(x * 16, y * 16, 16, 16);
                            break;
                        case FlatTiledNode.COLLIDABLE:
                            gsm.Render.debugRenderer.setColor(Color.RED);
                            gsm.Render.debugRenderer.rect(x * 16, y * 16, 16, 16);
                            break;
                        default:
                            //gsm.Render.debugRenderer.setColor(Color.WHITE);
                            //gsm.Render.debugRenderer.rect(x * 16, y * 16, 16, 16);
                            break;
                    }
                }
            }

            gsm.Render.debugRenderer.setColor(Color.FIREBRICK);
            for (int i = 0; i < Entities.size(); i++) {
                if(Entities.get(i) instanceof Enemy) {
                    Enemy temp = (Enemy) Entities.get(i);
                    int nodeCount = temp.getPath().getCount();
                    for (int j = 0; j < nodeCount; j++) {
                        FlatTiledNode node = temp.getPath().nodes.get(j);
                        gsm.Render.debugRenderer.rect(node.x * 16 + 4, node.y * 16 + 4, 4, 4);
                    }
                }
            }

            gsm.Render.debugRenderer.setColor(Color.FOREST);
            for (int i = 0; i < Entities.size(); i++) {
                if(Entities.get(i) instanceof Student) {
                    Student temp = (Student) Entities.get(i);
                    gsm.Render.debugRenderer.rect(temp.getDestination().x+2, temp.getDestination().y+2, 12, 12);
                }
            }



            for (int i = 0; i < Collisions.size(); i++) {

                //The bottom
                gsm.Render.debugRenderer.setColor(Color.YELLOW);
                gsm.Render.debugRenderer.rect(Collisions.get(i).getPrism().min.x, Collisions.get(i).getPrism().min.y + Collisions.get(i).getPrism().min.z / 2, Collisions.get(i).getPrism().getWidth(), Collisions.get(i).getPrism().getHeight());

                //The top of the Cube
                gsm.Render.debugRenderer.setColor(Color.RED);
                gsm.Render.debugRenderer.rect(Collisions.get(i).getPrism().min.x, Collisions.get(i).getPrism().min.y + Collisions.get(i).getPrism().getDepth() / 2 + Collisions.get(i).getPrism().min.z / 2, Collisions.get(i).getPrism().getWidth(), Collisions.get(i).getPrism().getHeight());

                gsm.Render.debugRenderer.setColor(Color.ORANGE);
            }

            for (int i = 0; i < Entities.size(); i++) {
                //gsm.Render.debugRenderer.box(Entities.get(i).getHitbox().min.x, Entities.get(i).getHitbox().min.y, Entities.get(i).getHitbox().min.z, Entities.get(i).getHitbox().getWidth(), Entities.get(i).getHitbox().getHeight(), Entities.get(i).getHitbox().getDepth());

                //The bottom
                gsm.Render.debugRenderer.setColor(Color.GREEN);
                gsm.Render.debugRenderer.rect(Entities.get(i).getHitbox().min.x, Entities.get(i).getHitbox().min.y + Entities.get(i).getHitbox().min.z / 2, Entities.get(i).getHitbox().getWidth(), Entities.get(i).getHitbox().getHeight());

                //The top of the Cube
                gsm.Render.debugRenderer.setColor(Color.BLUE);
                gsm.Render.debugRenderer.rect(Entities.get(i).getHitbox().min.x, Entities.get(i).getHitbox().min.y + Entities.get(i).getHitbox().getDepth() / 2 + Entities.get(i).getHitbox().min.z / 2, Entities.get(i).getHitbox().getWidth(), Entities.get(i).getHitbox().getHeight());

            }

            //The bottom of the PLAYER
            gsm.Render.debugRenderer.setColor(Color.YELLOW);
            gsm.Render.debugRenderer.rect(player.getHitbox().min.x, player.getHitbox().min.y + player.getHitbox().min.z / 2, player.getHitbox().getWidth(), player.getHitbox().getHeight());
            //The top of the Cube
            gsm.Render.debugRenderer.setColor(Color.RED);
            gsm.Render.debugRenderer.rect(player.getHitbox().min.x, player.getHitbox().min.y + player.getHitbox().getDepth() / 2 + player.getHitbox().min.z / 2, player.getHitbox().getWidth(), player.getHitbox().getHeight());


        }

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) { //KeyHit
            Vector3 pos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(pos);
            gsm.Render.debugRenderer.setColor(Color.WHITE);
            gsm.Render.debugRenderer.rect(((int) pos.x / 16) * 16 + 1, ((int) pos.y / 16) * 16 + 1, 15, 15);
        }

        gsm.Render.debugRenderer.end();

    }

    public void drawUI(SpriteBatch g, int height, int width, float Time) {
        //Draws things on the screen, and not the world positions
        g.setProjectionMatrix(GuiCam.combined);
        g.begin();
        MenuDraw(g, Gdx.graphics.getDeltaTime());
        g.end();
        UI.Draw(g);
    }

    private void handleInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (DialogOpen) {
                DialogNext();
            } else {
                for (int i = 0; i < Entities.size(); i++) {
                    if (Entities.get(i).getHitbox().intersects(player.getIntereactBox())) {
                        if (Entities.get(i) instanceof NPC) {
                            NPC Entitemp = (NPC) Entities.get(i);
                            Entitemp.interact();
                        }

                        //if (Entities.get(i) instanceof Trigger) {
                            //Trigger Ent = (Trigger) Entities.get(i);
                            //Ent.Interact(player,shaker,this,MainCameraFocusPoint,Particles,Entities);
                        //}
                     }
                }
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            //player.setPositionY(player.getPosition().y + 1);
            player.setVelocityY(player.getVelocity().y + 1);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            //player.setPositionY(player.getPosition().y - 1);
            player.setVelocityY(player.getVelocity().y - 1);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            //player.setPositionX(player.getPosition().x + 1);
            player.setFacing(false);
            player.setVelocityX(player.getVelocity().x + 1);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            //player.setPositionX(player.getPosition().x - 1);
            player.setFacing(true);
            player.setVelocityX(player.getVelocity().x - 1);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) { // ATTACK
            if (player.AttackTime < .1f) {

                //Vector3 addVeloc = new Vector3(player.getPosition().x - player.getAttackBox().min.x, player.getPosition().y - player.getAttackBox().min.y, 0);
                //player.setVelocity(new Vector3(player.getVelocity().x + (addVeloc.x*1.4f * -1), player.getVelocity().y + (addVeloc.y*1.4f * -1), player.getVelocity().z + (addVeloc.z*1.4f * -1)));

                Particles.AddParticleEffect("sparkle", player.getIntereactBox().getCenterX(), player.getIntereactBox().getCenterY());
                for (int i = 0; i < Entities.size(); i++) {
                    if (player.getAttackBox().intersects(Entities.get(i).getHitbox())) {
                        if (Entities.get(i) instanceof NPC) {
                            NPC Entitemp = (NPC) Entities.get(i);

                            float HitVelocity = 40;

                            Vector3 hitDirection = new Vector3(1 * HitVelocity, 0 * HitVelocity, 0);
                            Entitemp.damage(10, hitDirection);
                            shaker.addDamage(0.35f);
                        }
                    }
                }

                player.AttackTime += 0.75f;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            tempEnemy.interact();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            AddDialog("Pawn", "It's working!");
        }

        if (Gdx.input.justTouched()) { //NEEDS TO GET CHANGED TO ISJUSTDOWN
            //Calculate the angle of attack based on location of mouse around player.
            Vector3 pos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(pos);

            //Trying to find the way to calulate the angle from pos to playerPos //STILL NOT WORKING
            //System.out.println(new Vector2(pos.x, pos.y).angle(new Vector2(player.getPosition().x, player.getPosition().y)));

            player.TriggerAttack();
        }

    }

    public void reSize(SpriteBatch g, int H, int W) {

        /*Vector3 campostemp = camera.position;
        camera.setToOrtho(false, gsm.WorldWidth, gsm.WorldHeight);
        camera.position.set(campostemp);
        GuiCam.setToOrtho(false, gsm.UIWidth, gsm.UIHeight);
        shaker.reSize(camera);

        UI.reSize();

        //shaker.reSize(camera); */
    }

    public void cameraUpdate(WorldObject mainFocus, OrthographicCamera cam, List<WorldObject> Entities, int MinX, int MinY, int MaxX, int MaxY) {

        Vector2 FocalPoint = new Vector2(mainFocus.getPosition().x, mainFocus.getPosition().y);
        float totalFocusPoints = 1;

        for (int i = 0; i < Entities.size(); i++) {
            if (Entities.get(i).FocusStrength != 0) {
                if (mainFocus.getPosition().dst(Entities.get(i).getPosition()) <= 200) {
                    float tempX = Entities.get(i).getPosition().x;
                    float tempY = Entities.get(i).getPosition().y;

                    double dist = mainFocus.getPosition().dst(Entities.get(i).getPosition());

                    double influence = -((dist - 200) / 200) * 1;

                    FocalPoint.x += (tempX * (Entities.get(i).FocusStrength * influence));
                    FocalPoint.y += (tempY * (Entities.get(i).FocusStrength * influence));
                    totalFocusPoints += Entities.get(i).FocusStrength * influence;
                }
            }
        }

        if (FocalPoint.x - cam.viewportWidth / 2 <= MinX) {
            FocalPoint.x = MinX + cam.viewportWidth / 2;
        } else if (FocalPoint.x + cam.viewportWidth / 2 >= MaxX) {
            FocalPoint.x = MaxX - cam.viewportWidth / 2;
        }

        if (FocalPoint.y - cam.viewportHeight / 2 <= MinY) {
            FocalPoint.y = MinY + cam.viewportHeight / 2;
        } else if (FocalPoint.y + cam.viewportHeight / 2 >= MaxY) {
            FocalPoint.y = MaxY - cam.viewportHeight / 2;
        }

        cam.position.set((int) (FocalPoint.x / totalFocusPoints), (int) (FocalPoint.y / totalFocusPoints), 0);

        cam.update();
    }

    @Override
    public void dispose() {
        Collisions.clear();
        Entities.clear();
    }

    @Override
    public void Shutdown() {

    }

}