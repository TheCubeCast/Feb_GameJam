package com.thecubecast.ReEngine.worldObjects.AI.Pawn;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.FlatTiledGraph;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.FlatTiledNode;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.TiledManhattanDistance;
import com.thecubecast.ReEngine.worldObjects.AI.Pathfinding.TiledSmoothableGraphPath;

import static com.badlogic.gdx.utils.TimeUtils.nanoTime;

public class Smart implements Telegraph {

    private Vector3 Destination;
    Pawn_Enemy WorldObject;

    FlatTiledGraph worldMap;

    TiledSmoothableGraphPath<FlatTiledNode> path;
    IndexedAStarPathFinder<FlatTiledNode> pathFinder;
    TiledManhattanDistance<FlatTiledNode> heuristic;

    private StateMachine<Smart, EnemyState> stateMachine;

    public Smart(Pawn_Enemy WorldObject, FlatTiledGraph worldMap) {

        Destination = new Vector3(WorldObject.getPosition());

        this.worldMap = worldMap;

        path = new TiledSmoothableGraphPath<FlatTiledNode>();
        heuristic = new TiledManhattanDistance<FlatTiledNode>();
        pathFinder = new IndexedAStarPathFinder<FlatTiledNode>(worldMap, true);

        stateMachine = new DefaultStateMachine<Smart, EnemyState>(this, EnemyState.IDLE);
        stateMachine.getCurrentState().enter(this);

        this.WorldObject = WorldObject;

    }

    public void update() {
        stateMachine.update();
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        return stateMachine.handleMessage(msg);
    }

    public void updateMap(FlatTiledGraph worldMap) {
        this.worldMap = worldMap;
        pathFinder = new IndexedAStarPathFinder<FlatTiledNode>(worldMap, true);
    }

    public void updatePath(boolean forceUpdate) {
        int tileX = (int) getDestination().x / 16;
        int tileY = (int) getDestination().y / 16;
        if (forceUpdate || tileX != WorldObject.getPosition().x || tileY != WorldObject.getPosition().y) {
            FlatTiledNode startNode = worldMap.getNode((int) WorldObject.getPosition().x / 16, (int) WorldObject.getPosition().y / 16);
            FlatTiledNode endNode = worldMap.getNode(tileX, tileY);
            if (forceUpdate || endNode.type == FlatTiledNode.GROUND) {
                if (endNode.type == FlatTiledNode.GROUND) {
                    //WorldObject.setPositionX(tileX * 16);
                    //WorldObject.setPositionY(tileY * 16);
                } else {
                    endNode = worldMap.getNode(tileX, tileY);
                }
                path.clear();
                worldMap.startNode = startNode;
                long startTime = nanoTime();
                pathFinder.searchNodePath(startNode, endNode, heuristic, path);

                if (pathFinder.metrics != null && false) {
                    float elapsed = (TimeUtils.nanoTime() - startTime) / 1000000f;
                    System.out.println("----------------- Indexed A* Path Finder Metrics -----------------");
                    System.out.println("start node...................... = " + startNode.x + " " + startNode.y);
                    System.out.println("end node...................... = " + endNode.x + " " + endNode.y);
                    System.out.println("Visited nodes................... = " + pathFinder.metrics.visitedNodes);
                    System.out.println("Open list additions............. = " + pathFinder.metrics.openListAdditions);
                    System.out.println("Open list peak.................. = " + pathFinder.metrics.openListPeak);
                    System.out.println("Path finding elapsed time (ms).. = " + elapsed);
                }
            }
        }
    }


    //GETTERS AND SETTERS
    public StateMachine<Smart, EnemyState> getStateMachine() {
        return stateMachine;
    }

    public Vector3 getDestination() {
        return Destination;
    }

    public void setDestination(Vector3 destination) {
        Destination = destination;
    }

    public TiledSmoothableGraphPath<FlatTiledNode> getPath() {
        return path;
    }
}
