package com.jcity.roads;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.*;

import org.apache.commons.chain.*;
import org.apache.commons.chain.impl.*;
import org.apache.log4j.*;

import com.jcity.model.Layer;
import com.jcity.pipeline.*;
import com.jcity.roads.Roadmap;
import com.jcity.roads.gui.RoadmapPanel;

@SuppressWarnings("serial")
public class RoadMapGeneratorTest extends RoadmapPanel {

	private static final String CONFIG = "manhatten";
	// private static final String CONFIG = "sf";
	// private static final String CONFIG = "sample";
	// private Roadmap rm;
	// private RoadmapGenerator rmg;
	private Logger logger = Logger.getLogger(this.getClass());
	private int verts = 0;
	private int edges;
	private static final float SCALE = 0.3f;
	private BufferedImage buff;
	private RoadmapContext ctx;
	private Layer bgLayer;
	String mapLegendText = "Height";

	public static void main(String... args) throws Exception {
		new RoadMapGeneratorTest();
	}

	public void keyTyped(KeyEvent e) {
		Layer l = null;
		String s = "";

		switch (e.getKeyChar()) {
		case '1':
			l = ctx.getHeightMap();
			s = "Height";
			break;
		case '2':
			l = ctx.getBlockedMap();
			s = "Blocked";
			break;
		case '3':
			l = ctx.getDensityMap();
			s = "Density";
			break;
		case '4':
			l = ctx.getBuildingHeightMap();
			s = "BHeight";
			break;
		case '5':
			l = ctx.getPatternMap();
			s = "Pattern";
			break;
		case '6':
			l = ctx.getWaterMap();
			s = "Water";
			break;
		}

		if (l != null) {
			bgLayer = l;
			mapLegendText = s;
			repaint();
		}
	}

	protected RoadMapGeneratorTest() throws Exception {

		Chain ch = new ChainBase();
		ctx = new RoadmapContext();
		ch.addCommand(new ContextLoader("/com/jcity/" + CONFIG + "/config.properties"));
		ch.addCommand(new RasterMapLoaderCommand());
		ch.addCommand(new RoadSystemGenerator());
		ch.addCommand(new BlockGenerator());
		ch.addCommand(new LotGenerator());
		ch.addCommand(new ZoningGenerator());
		ch.addCommand(new BuildingGenerator());
		
		ch.execute(ctx);

		setupGui(ctx.getResolutionX() * SCALE, ctx.getResolutionY() * SCALE);


		// logger.info(rm.toJSON());
		// try {
		// // retrieve image
		// BufferedImage bi = rm.toImage();
		// File outputfile = new File("saved.png");
		// ImageIO.write(bi, "png", outputfile);
		// } catch (IOException e) {
		// logger.error(e);
		// }
	}

	@Override
	protected void paintComponent(Graphics g) {
		drawMap(ctx.getRoadmap(), g);
	}

	private void drawMap(Roadmap rm, Graphics g) {
		if (rm == null)
			return;
		logger.debug("drawMap");
		int w = (int) (ctx.getResolutionX() * SCALE);
		int h = (int) (ctx.getResolutionY() * SCALE);

		if (bgLayer == null) {
			bgLayer = ctx.getHeightMap();
		}

		// if (rm.getVertexCount() != verts || rm.getEdgeCount() != edges) {
		verts = rm.getVertexCount();
		edges = rm.getEdgeCount();
		// Draw the buffer once.
		buff = rm.toImage(w, h, bgLayer);
		// }

		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(buff, 0, 0, null);
		g2.setColor(Color.RED);
		g2.drawString(mapLegendText, 20, 20);
	}
}
