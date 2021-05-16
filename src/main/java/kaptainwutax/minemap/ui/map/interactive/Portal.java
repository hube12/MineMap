package kaptainwutax.minemap.ui.map.interactive;

import kaptainwutax.featureutils.structure.RuinedPortal;
import kaptainwutax.featureutils.structure.generator.structure.RuinedPortalGenerator;
import kaptainwutax.mcutils.block.Block;
import kaptainwutax.mcutils.block.Blocks;
import kaptainwutax.mcutils.util.data.Pair;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.minemap.init.Logger;
import kaptainwutax.minemap.ui.map.MapPanel;
import kaptainwutax.minemap.util.snksynthesis.voxelgame.Visualizer;
import kaptainwutax.minemap.util.snksynthesis.voxelgame.block.BlockType;
import kaptainwutax.terrainutils.ChunkGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Portal {
    private final MapPanel map;
    private CPos pos;
    private RuinedPortal feature;
    private final Visualizer visualizer;
    private AtomicBoolean isRunning;

    public Portal(MapPanel map) {
        this.map = map;
        this.visualizer = new Visualizer();
        this.isRunning = new AtomicBoolean(false);
    }

    public Visualizer getVisualizer() {
        return visualizer;
    }

    public void run() {
        if (!isRunning.get()) {
            isRunning.set(true);
            Thread t = new Thread(() -> {
                visualizer.run(); //blocking
                isRunning.set(false);
            });
            t.start();
        }
    }

    public void setFeature(RuinedPortal feature) {
        this.feature = feature;
    }

    public Pair<RuinedPortal, CPos> getInformations() {
        return new Pair<>(this.feature, this.pos);
    }

    public boolean generateContent() {
        Pair<RuinedPortal, CPos> informations = this.getInformations();
        RuinedPortalGenerator ruinedPortalGenerator = new RuinedPortalGenerator(informations.getFirst().getVersion());
        Pair<ChunkGenerator, Function<CPos,CPos>> generator=this.map.context.getChunkGenerator(feature);
        if (generator==null){
            visualizer.setText("Portal did not generate");
            getVisualizer().getBlockManager().scheduleDestroy();
            return false;
        }
        if (!ruinedPortalGenerator.generate(generator.getFirst(), generator.getSecond().apply(informations.getSecond()))){
            visualizer.setText("Portal did not generate");
            getVisualizer().getBlockManager().scheduleDestroy();
            return false;
        }
        List<Pair<Block, BPos>> blocks = ruinedPortalGenerator.getPortal();
        if (blocks == null || blocks.isEmpty()) {
            visualizer.setText("Portal did not generate");
            getVisualizer().getBlockManager().scheduleDestroy();
            return false;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        HashMap<BPos, Block> blockHashMap = new HashMap<>();
        for (Pair<Block, BPos> block : blocks) {
            if (blockHashMap.containsKey(block.getSecond())) {
                Logger.LOGGER.severe("Impossible case " + informations);
                System.err.println("Impossible case " + informations);
            }
            blockHashMap.put(block.getSecond(), block.getFirst());
            minX = Math.min(block.getSecond().getX(), minX);
            minY = Math.min(block.getSecond().getY(), minY);
            minZ = Math.min(block.getSecond().getZ(), minZ);
            maxX = Math.max(block.getSecond().getX(), maxX);
            maxY = Math.max(block.getSecond().getY(), maxY);
            maxZ = Math.max(block.getSecond().getZ(), maxZ);
        }
        getVisualizer().getBlockManager().scheduleDestroy();
        for (BPos pos : blockHashMap.keySet()) {
            BPos p = pos.subtract(minX, minY, minZ).add(8, 0, 8);
            Block block = blockHashMap.get(pos);
            BlockType type = block == Blocks.OBSIDIAN ? BlockType.OBSIDIAN : block == Blocks.CRYING_OBSIDIAN ? BlockType.CRYING_OBSIDIAN : BlockType.GRASS;
            getVisualizer().getBlockManager().scheduleBlock(p, type);
        }
        BPos pos=ruinedPortalGenerator.getPos();
        getVisualizer().setText(String.format("Generated at %d, %d, %d and was %s with world height at %d",
            pos.getX(),pos.getY(),pos.getZ(),ruinedPortalGenerator.isBuried()?"buried":"at the surface",ruinedPortalGenerator.getHeight()));
        return true;
    }

    public void setPos(CPos pos) {
        this.pos = pos;
    }


}