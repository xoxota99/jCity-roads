package com.jcity.pipeline;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;

import org.apache.commons.chain.*;

import com.jcity.model.*;

/**
 * (Step 2) Command responsible for loading any Raster-based (grid data) map,
 * from an image resource.
 * 
 * @author philippd
 * 
 */
public class RasterMapLoaderCommand extends LogEnabledCommand {
	private static Map<String, String> mapList = new HashMap<>();

	static {
		mapList.put("heightMap", "heightMapResourceName");
		mapList.put("buildingHeightMap", "buildingHeightMapResourceName");
		mapList.put("densityMap", "densityMapResourceName");
		mapList.put("waterMap", "waterMapResourceName");
		mapList.put("blockedMap", "blockedMapResourceName");
		mapList.put("patternMap", "patternMapResourceName");
	}

	protected Layer loadMap(String resourceName, CityContext ctx) throws FileNotFoundException {
		if (resourceName != null) {
			BufferedImage img = null;
			try {
				logger.debug("loading " + resourceName);
				InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
				// ClassLoader cl = getClass().getClassLoader();
				// is =
				// cl.getResourceAsStream("../manhatten/manhatten-height.tga");
				if (is == null)
					return null;
				img = ImageIO.read(is);
			} catch (IOException e) {
				throw new FileNotFoundException("Error loading map file '" + resourceName + "'");
			} catch (RuntimeException e2) {
				logger.error(e2, e2);
				throw e2;
			}
			int w = img.getWidth();
			int h = img.getHeight();
			int[][] values = new int[w][h];
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					int rgb = img.getRGB(x, y);
					int val = rgb & 0x000000ff; // Red? Blue?.
					// logger.debug("RGB value is "+Integer.toHexString(rgb)+",
					// int value is "+Integer.toHexString(val));
					values[x][h - y - 1] = val; // WTF: reverse y-axis, for some
												// reason.
				}
			}
			Layer retval = new Layer(w, h, ctx.getResolutionX(), ctx.getResolutionY());
			retval.setValues(values);
			retval.setImage(img);

			logger.debug("Finished loading " + resourceName);

			return retval;
		}
		return null;
	}

	@Override
	public boolean execute(Context ctx) throws Exception {
		CityContext sctx = (CityContext) ctx;

		// TODO: Put this list in a config somewhere.
		for (Map.Entry<String, String> e : mapList.entrySet()) {
			if (sctx.get(e.getValue()) != null) {
				sctx.put(e.getKey(), loadMap(sctx.getInputDir() + sctx.get(e.getValue()), sctx));
			}
		}
		return false;
	}
}
