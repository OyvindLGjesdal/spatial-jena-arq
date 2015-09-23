package fi.seco.spatial.arq;

import org.apache.jena.query.spatial.SpatialQuery;
import org.apache.jena.query.spatial.pfunction.SpatialMatch;
import org.apache.lucene.spatial.query.SpatialOperation;

import com.spatial4j.core.shape.Shape;

/**
 * UNFINISHED VERSION, trying to use the Lucene spatial index for checking if a point is inside polygon (instead of bbox as in master branch)
 *
 */

public class PolygonSpatialMatch extends SpatialMatch {

	private final Shape shape;
	private final SpatialOperation operation;
	
	 public PolygonSpatialMatch(String polygonStr, SpatialOperation operation) {
		 this.shape = SpatialQuery.ctx.readShapeFromWkt(polygonStr);
		 this.operation = operation;
	 }
	
	 @Override
	public String toString() {
		return "(" + shape + " " + operation + ")";
	}	
}