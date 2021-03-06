package kaptainwutax.minemap.ui.dialog;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.Feature;
import kaptainwutax.featureutils.structure.EndCity;
import kaptainwutax.featureutils.structure.RegionStructure;
import kaptainwutax.featureutils.structure.Stronghold;
import kaptainwutax.featureutils.structure.Structure;
import kaptainwutax.featureutils.structure.generator.structure.EndCityGenerator;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.minemap.MineMap;
import kaptainwutax.minemap.feature.OWBastionRemnant;
import kaptainwutax.minemap.feature.OWFortress;
import kaptainwutax.minemap.feature.StructureHelper;
import kaptainwutax.minemap.init.Features;
import kaptainwutax.minemap.listener.Events;
import kaptainwutax.minemap.ui.map.MapContext;
import kaptainwutax.minemap.ui.map.MapManager;
import kaptainwutax.minemap.ui.map.MapPanel;
import kaptainwutax.minemap.ui.map.MapSettings;
import kaptainwutax.minemap.util.data.Str;
import kaptainwutax.minemap.util.ui.interactive.Dropdown;
import kaptainwutax.terrainutils.TerrainGenerator;
import one.util.streamex.StreamEx;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StructureHopperDialog extends Dialog {
    public Dropdown<StructureItem> structureItemDropdown;
    public JButton continueButton;
    private MapPanel map;
    private MapContext context;
    private MapSettings settings;
    private MapManager manager;
    private ChunkRand chunkRand;

    public StructureHopperDialog(Runnable onExit) {
        super("Go to Structure Coordinates", new GridLayout(0, 1));
        this.addExitProcedure(onExit);
    }

    @Override
    public void initComponents() {
        map = MineMap.INSTANCE.worldTabs.getSelectedMapPanel();
        if (map == null) return;
        context = map.getContext();
        settings = context.getSettings();
        manager = map.getManager();
        chunkRand = new ChunkRand();
        List<Feature<?, ?>> features = settings.getAllFeatures();

        List<StructureItem> structureItems = features.stream()
            .filter(e -> e instanceof RegionStructure || e instanceof Stronghold/**/)
            .map(e -> new StructureItem((Structure<?, ?>) e))
            .collect(Collectors.toList());
        if (map.getContext().getDimension()==Dimension.END){
            structureItems.add(new StructureItem((Structure<?, ?>) Features.getForVersion(map.getContext().getVersion()).get(EndCity.class), bPos -> {
                EndCityGenerator endCityGenerator = new EndCityGenerator(map.getContext().getVersion());
                if (!endCityGenerator.generate(map.getContext().getTerrainGenerator(), bPos.toChunkPos())) return false;
                return endCityGenerator.hasShip();
            }) {
                @Override
                public String toString() {
                    return "End city with Elytra";
                }
            });
        }

        this.structureItemDropdown = new Dropdown<>(structureItems);
        this.continueButton = new JButton();
        this.continueButton.setText("Continue");

        this.continueButton.addMouseListener(Events.Mouse.onPressed(e -> create()));

        this.getContentPane().add(this.structureItemDropdown);
        this.getContentPane().add(this.continueButton);
    }

    protected void create() {
        if (!this.continueButton.isEnabled()) return;
        Structure<?, ?> feature = this.structureItemDropdown.getSelected().getFeature();
        Function<BPos, Boolean> filter = this.structureItemDropdown.getSelected().getFilter();
        if (!(feature instanceof RegionStructure || feature instanceof Stronghold)) return;
        BPos centerPos = manager.getCenterPos();
        BiomeSource biomeSource = context.getBiomeSource();
        TerrainGenerator terrainGenerator = context.getTerrainGenerator();
        int dimCoeff = 0;
        if (feature instanceof OWBastionRemnant || feature instanceof OWFortress) {
            biomeSource = context.getBiomeSource(Dimension.NETHER);
            terrainGenerator = context.getTerrainGenerator(Dimension.NETHER);
            dimCoeff = 3;
        }

        Stream<BPos> stream=StructureHelper.getClosest(feature, centerPos, context.worldSeed, chunkRand, biomeSource,terrainGenerator, dimCoeff);
        assert stream != null;
        List<BPos> bPosList = StreamEx.of(stream)
            .sequential()
            .filter(e -> filter != null ? filter.apply(e) : true)
            .limit(1)
            .collect(Collectors.toList());
        if (!bPosList.isEmpty()) {
            BPos bPos = bPosList.get(0);
            manager.setCenterPos(bPos.getX(), bPos.getZ());
        } else {
            System.out.println("Not found");
        }
        this.dispose();
    }

    protected void cancel() {
        continueButton.setEnabled(false);
        dispose();
    }

    static class StructureItem {

        private final Structure<?, ?> feature;
        private final Function<BPos, Boolean> filter;

        StructureItem(Structure<?, ?> feature) {
            this(feature, null);
        }

        StructureItem(Structure<?, ?> feature, Function<BPos, Boolean> filter) {
            this.feature = feature;
            this.filter = filter;
        }

        public Function<BPos, Boolean> getFilter() {
            return filter;
        }

        public Structure<?, ?> getFeature() {
            return feature;
        }

        @Override
        public String toString() {
            return Str.prettifyDashed(feature.getName());
        }
    }

}
