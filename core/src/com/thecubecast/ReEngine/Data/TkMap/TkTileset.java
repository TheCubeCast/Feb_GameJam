package com.thecubecast.ReEngine.Data.TkMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import static com.thecubecast.ReEngine.Data.GameStateManager.Render;

public class TkTileset {

    String Name;
    String FilePath;
    int TileSizeW;
    int TileSizeH;
    int TileSep;

    TextureRegion TilesetImage;
    TextureRegion[] Tiles;

    public TkTileset(String name, String Path, int tileSizeW, int tileSizeH, int tileSep) {
        Name = name;
        FilePath = Path;
        TileSizeW = tileSizeW;
        TileSizeH = tileSizeH;
        TileSep = tileSep;
        TilesetImage = Render.getTexture("tileset");

        int cols = TilesetImage.getRegionWidth() / TileSizeW;
        int rows = TilesetImage.getRegionHeight() / TileSizeH;

        Tiles = new TextureRegion[rows * cols];

        TextureRegion[][] tmp = TilesetImage.split(
                TileSizeW,
                TileSizeH);

        int index = 0;
        for (int l = 0; l < TilesetImage.getRegionHeight() / TileSizeH; l++) {
            for (int j = 0; j < TilesetImage.getRegionWidth() / TileSizeW; j++) {
                Tiles[index++] = tmp[l][j];
            }
        }

    }

    public TextureRegion[] getTiles() {
        return Tiles;
    }
}
