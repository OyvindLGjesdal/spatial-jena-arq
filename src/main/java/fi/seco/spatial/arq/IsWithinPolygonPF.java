package fi.seco.spatial.arq;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.spatial.EntityDefinition;
import org.apache.jena.query.spatial.pfunction.SpatialOperationWithBoxPFBase;
import org.apache.lucene.spatial.query.SpatialOperation;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Property function for determining if a geo point (WGS84) is inside a polygon. 
 *
 * Implemented as an extension of the Jena Spatial module,
 *  see http://jena.apache.org/documentation/query/spatial-query.html
 * Uses JTS Topology Suite,
 *  see http://tsusiatsoftware.net/jts/main.html
 * 
 * This property function requires the subject to be a resource with spatial-indexed geo point  
 * (using the properties from the namespace http://www.w3.org/2003/01/geo/wgs84_pos#) or unbound,
 * and the object to be a string or list representing a polygon. If the polygon is not closed,
 * i.e. it's last point is not the same as the first point, it's closed automatically. 
 *
 * Usage:
 * 
 *  ?place seco-spatial:withinPolygon 'polygon'   
 *  OR 
 *  ?place seco-spatial:withinPolygon ('polygon' ['delimiter_point'] ['delimiter_longlat'] [lat_long])
 *
 *   polygon: string containing the individual points of a polygon in format 'long_1 lat_1, long_2 lat_2, ...' (default)
 *   delimiter_point: delimiter used between the individual points of a polygon, default: ', '
 *   delimiter_longlat: delimiter used between longitude and latitude coordinates of a point in polygon, default: ' '
 *   lat_long: is latitude before longitude in a point in polygon, default: false
 *  
 * Examples:
 * 
 *  Simple:           ?place seco-spatial:withinPolygon '24.9422920760 59.9224888308, 25.1585533582 59.9424526638, 25.1687391225 60.0270350324, 24.9422920760 59.9224888308'
 *  
 *  WKT:              ?place seco-spatial:withinPolygon 'POLYGON ((24.9422920760 59.9224888308, 25.1585533582 59.9424526638, 25.1687391225 60.0270350324, 24.9422920760 59.9224888308))'^^<http://www.opengis.net/ont/geosparql#wktLiteral>
 *  
 *  SAPO:             ?place seco-spatial:withinPolygon ('24.9422920760,59.9224888308 25.1585533582,59.9424526638 25.1687391225,60.0270350324 24.9422920760,59.9224888308' ' ' ',')
 *   
 *  Lat before Long:  ?place seco-spatial:withinPolygon ('59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225, 59.9224888308 24.9422920760' ', ' ' ' true)
 *   
 * See class examples.SpatialFunctionsExample for complete SPARQL query examples.
 */

public class IsWithinPolygonPF extends SpatialOperationWithBoxPFBase {
	private Polygon polygon;
	private PropFuncArg argObjectBBox;
	
	@Override
	protected SpatialOperation getSpatialOperation() {
		return SpatialOperation.IsWithin;
	}

	@Override	
	public void build(PropFuncArg argSubject, Node predicate,
			PropFuncArg argObject, ExecutionContext execCxt) {
		
		if (!argSubject.isNode())
			throw new QueryBuildException("Subject is not a single node: "
					+ argSubject);

		// compute the bounding box of the polygon
	    ArrayList<Coordinate> points = new ArrayList<Coordinate>();
	    
	    String delimiterPoint = ", ";
	    String delimiterLatLong = " ";
	    boolean latLong = false;
	    
	    if (argObject.isList()) {
	    	List<Node> args = argObject.getArgList();
	    	if (!args.isEmpty()) {
	    		argObject = new PropFuncArg(args.get(0));
		    	if (args.size() > 1) {
		    		delimiterPoint = args.get(1).getLiteralLexicalForm();
		    		if (args.size() > 2) {
		    			delimiterLatLong = args.get(2).getLiteralLexicalForm();
		    			if (args.size() > 3) {
		    				Node latLongParam = args.get(3);
		    				if (latLongParam.getLiteralDatatype().equals(XSDDatatype.XSDboolean) &&
		    					latLongParam.getLiteralLexicalForm() == "true")
			    				latLong = true;
		    			}
			    	}
			    }
		    }
	    }
	    
	    if (argObject.getArg().isVariable()) 
			throw new QueryBuildException("Object is/contains a variable: "
					+ argObject);
	    
		Node polygonNode = argObject.getArg();
		if (polygonNode.isLiteral()) {
			String polygonStr = polygonNode.getLiteralLexicalForm();
			if (polygonStr.indexOf("POLYGON ((") == 0)
				polygonStr = polygonStr.substring("POLYGON ((".length(), polygonStr.length()-2);
			for (String pointStr : polygonStr.split(delimiterPoint)) {
				String[] coords = pointStr.split(delimiterLatLong);
				if (coords.length == 2) {
					float first = Float.parseFloat(coords[0]);
					float second = Float.parseFloat(coords[1]);
					if (latLong)
						points.add(new Coordinate(first, second));
					else
						points.add(new Coordinate(second, first));
				}
			}

			GeometryFactory gf = new GeometryFactory();
			CoordinateList list = new CoordinateList(points.toArray(new Coordinate[points.size()]));
			list.closeRing();
			this.polygon = gf.createPolygon(new LinearRing(new CoordinateArraySequence(list.toCoordinateArray()), gf), null);
			Envelope envelope = this.polygon.getEnvelopeInternal();
			
			List<Node> bBox = new ArrayList<Node>();
			bBox.add(NodeFactory.createLiteral(Double.toString(envelope.getMinX())));
			bBox.add(NodeFactory.createLiteral(Double.toString(envelope.getMinY())));
			bBox.add(NodeFactory.createLiteral(Double.toString(envelope.getMaxX())));
			bBox.add(NodeFactory.createLiteral(Double.toString(envelope.getMaxY())));
			this.argObjectBBox = new PropFuncArg(bBox);

			super.build(argSubject, predicate, this.argObjectBBox, execCxt);
		}
	}

	@Override
	public QueryIterator exec(Binding binding, PropFuncArg argSubject,
			Node predicate, PropFuncArg argObject, ExecutionContext execCxt) {
		if (this.polygon != null) {
			// first query the Lucene spatial index with the bounding box of the polygon
			QueryIterator qIter = super.exec(binding, argSubject, predicate, this.argObjectBBox, execCxt);
	
			// then filter out the points that are not inside the polygon
			Graph g = execCxt.getActiveGraph();
			Node subj = argSubject.getArg();
			if (subj.isVariable()) {
				Var v = Var.alloc(subj);
				ArrayList<Binding> filteredHits = new ArrayList<Binding>();
				while (qIter.hasNext()) {
					Binding b = qIter.nextBinding();
					if (isWithin(b.get(v), g))
						filteredHits.add(b);
				}
				return new QueryIterPlainWrapper(filteredHits.iterator());			
			}
			else if (subj.isURI()) {
				if (isWithin(subj, g))
					return qIter;
			}
		}
		return null;
	}
	
	private boolean isWithin(Node uri, Graph g) {
		// works only for points and does not support custom geo predicates (or WKT properties)
		// - fix by using Lucene polygon query (if available?)
		ExtendedIterator<Triple> iter = g.find(uri, EntityDefinition.geo_latitude.asNode(), null);
		if (iter.hasNext()) {
			Double latD = Double.parseDouble(iter.next().getObject().getLiteralLexicalForm());
			ExtendedIterator<Triple> iter2 = g.find(uri, EntityDefinition.geo_longitude.asNode(), null);
			if (iter2.hasNext()) {
				Double longD = Double.parseDouble(iter2.next().getObject().getLiteralLexicalForm());
				return isWithin(latD, longD);
			}
		}
		return false;
	}
	
	private boolean isWithin(double latD, double longD) {
		Coordinate coord = new Coordinate(latD, longD);
		GeometryFactory gf = new GeometryFactory();
		Point point = gf.createPoint(coord);
		return point.within(this.polygon);
	}
}