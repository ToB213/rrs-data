/**
 * 
 */
package mrl_2023.world.geometry;
import com.vividsolutions.jts.geom.*;

import java.util.logging.Logger;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class Scaling {
	private static Logger logger = Logger.getLogger(Scaling.class.getName());

	public static Coordinate[] get(Coordinate[] coord, Coordinate c, double coef){
		Coordinate[] coord_= new Coordinate[coord.length];
		double xc = c.x, yc = c.y;
		Coordinate ci;
		double x, y;
		for(int i=0; i<coord.length; i++) {
			ci = coord[i];
			x = ci.x;
			y = ci.y;
			coord_[i] = new Coordinate(xc+coef*(x-xc), yc+coef*(y-yc));
		}
		return coord_;
	}

	public static Point get(Point geom, Coordinate c, double coef, GeometryFactory gf) {
		double xc = c.x, yc = c.y;
		return gf.createPoint( new Coordinate(xc+coef*(geom.getX()-xc), yc+coef*(geom.getY()-yc)) );
	}

	public static LineString get(LineString ls, Coordinate c, double coef, GeometryFactory gf) {
		return gf.createLineString(get(ls.getCoordinates(), c, coef));
	}

	public static LinearRing get(LinearRing lr, Coordinate c, double coef, GeometryFactory gf) {
		return gf.createLinearRing(get(lr.getCoordinates(), c, coef));
	}

	public static Polygon get(Polygon geom, Coordinate c, double coef, GeometryFactory gf) {
		LinearRing lr = get((LinearRing)geom.getExteriorRing(), c, coef, gf);
		LinearRing[] lr_ = new LinearRing[geom.getNumInteriorRing()];
		for(int j=0; j<geom.getNumInteriorRing(); j++) lr_[j] = get((LinearRing)geom.getInteriorRingN(j), c, coef, gf);
		return gf.createPolygon(lr, lr_);
	}

	public static GeometryCollection get(GeometryCollection geomCol, Coordinate c, double coef, GeometryFactory gf) {
		Geometry[] gs = new Geometry[geomCol.getNumGeometries()];
		for(int i=0; i< geomCol.getNumGeometries(); i++) gs[i] = get(geomCol.getGeometryN(i), c, coef, gf);
		return gf.createGeometryCollection(gs);
	}

	public static Geometry get(Geometry geom, Coordinate c, double coef, GeometryFactory gf) {
		if(geom instanceof Point) return get((Point)geom, c, coef, gf);
		else if(geom instanceof Polygon) return get((Polygon)geom, c, coef, gf);
		else if(geom instanceof LineString) return get((LineString)geom, c, coef, gf);
		else if(geom instanceof LinearRing) return get((LinearRing)geom, c, coef, gf);
		logger.warning("Scaling of " + geom.getClass().getSimpleName() + " not supported yet.");
		return null;
	}
}
