package kaptainwutax.minemap.feature;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.featureutils.Feature;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.terrainutils.TerrainGenerator;
import kaptainwutax.terrainutils.terrain.OverworldTerrainGenerator;

public class SpawnPoint extends Feature<Feature.Config, SpawnPoint.Data> {

    public SpawnPoint() {
        super(new Config(), null);
    }

    public static String name() {
        return "spawn";
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean canStart(SpawnPoint.Data data, long structureSeed, ChunkRand rand) {
        throw new UnsupportedOperationException("Spawn depends on biomes!");
    }

    @Override
    public boolean canSpawn(SpawnPoint.Data data, BiomeSource source) {
        if (source instanceof OverworldBiomeSource) {
            Context context=this.getContext(source.getWorldSeed());
            if (context.getGenerator()!=null){
                BPos spawn=new kaptainwutax.featureutils.misc.SpawnPoint((OverworldTerrainGenerator) context.getGenerator()).getSpawnPoint();
                return data.blockX == spawn.getX() && data.blockZ == spawn.getZ();
            }
        }

        return false;
    }

    @Override
    public boolean canGenerate(Data data, TerrainGenerator generator) {
        return true;
    }

    @Override
    public Dimension getValidDimension() {
        return Dimension.OVERWORLD;
    }

//    public BPos get(BiomeSource source) {
//        return source instanceof OverworldBiomeSource ? ((OverworldBiomeSource) source).getSpawnPoint() : null;
//    }

    public static class Data extends Feature.Data<SpawnPoint> {
        public final int blockX;
        public final int blockZ;

        public Data(SpawnPoint feature, int blockX, int blockZ) {
            super(feature, blockX >> 4, blockZ >> 4);
            this.blockX = blockX;
            this.blockZ = blockZ;
        }
    }

}
