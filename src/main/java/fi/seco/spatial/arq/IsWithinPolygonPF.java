package fi.seco.spatial.arq;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.spatial.SpatialIndexException;
import org.apache.jena.query.spatial.SpatialValueUtil;
import org.apache.jena.query.spatial.pfunction.SpatialMatch;
import org.apache.jena.query.spatial.pfunction.SpatialOperationPFBase;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * UNFINISHED VERSION, trying to use the Lucene spatial index for checking if a point is inside polygon (instead of bbox as in master branch)
 * 
 * Property function that requires the subject to be a resource with spatial-indexed geo point (can be unbound) and the object to
 * be a string or list representing a polygon.
 *
 * Object format: 'polygon' OR 
 *                ('polygon' ['delimiter_point'] ['delimiter_latlong'] [long_lat])
 *                
 *
 *  delimiter_point, default: ', '
 *  delimiter_latlong, default: ' '
 *  long_lat, default: false (true means that long is before lat in the polygon string)
 *  
 *  Examples:
 *   Simple:  '59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225'
 *   WKT:     'POLYGON ((59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225))'
 *   SAPO:    ('24.9422920760,59.9224888308 25.1585533582,59.9424526638 25.1687391225,60.0270350324' ' ' ',' true)
 */

public class IsWithinPolygonPF extends SpatialOperationPFBase {
	private static Logger log = LoggerFactory.getLogger(IsWithinPolygonPF.class);
	
	@Override
	protected SpatialOperation getSpatialOperation() {
		return SpatialOperation.IsWithin;
	}

	@Override	
	public void build(PropFuncArg argSubject, Node predicate,
			PropFuncArg argObject, ExecutionContext execCxt) {
		super.build(argSubject, predicate, argObject, execCxt);

		if (!argSubject.isNode())
			throw new QueryBuildException("Subject is not a single node: "
					+ argSubject);
	}

	@Override
	protected SpatialMatch objectToStruct(PropFuncArg argObject) {
	    String delimiterPoint = ", ";
	    String delimiterLatLong = " ";
	    boolean longLat = false;
	    
	    if (argObject.isList()) {
	    	List<Node> args = argObject.getArgList();
	    	if (!args.isEmpty()) {
		    	if (args.size() > 1) {
		    		delimiterPoint = args.get(1).getLiteralLexicalForm();
		    		if (args.size() > 2) {
		    			delimiterLatLong = args.get(2).getLiteralLexicalForm();
		    			if (args.size() > 3) {
		    				Node longLatParam = args.get(3);
		    				if (longLatParam.getLiteralDatatype().equals(XSDDatatype.XSDboolean) && longLatParam.getLiteralLexicalForm() == "true")
		    					longLat = true;
		    			}

		    		}
		    	}
		    	argObject = new PropFuncArg(args.get(0));
	    	}
	    }
		
		Node polygonNode = argObject.getArg();
		if (polygonNode.isLiteral()) {
			String origPolygonStr = polygonNode.getLiteralLexicalForm();
			String polygonStr = "";
			if (origPolygonStr.indexOf("POLYGON ((") == 0) {
				origPolygonStr = origPolygonStr.substring("POLYGON ((".length(), origPolygonStr.length()-2);
			}
			for (String pointStr : origPolygonStr.split(delimiterPoint)) {
				String[] coords = pointStr.split(delimiterLatLong);
				if (coords.length == 2) {
					float first = Float.parseFloat(coords[0]);
					float second = Float.parseFloat(coords[1]);
					
					if (longLat)
						polygonStr += second+", "+first+" ";
					else
						polygonStr += first+", "+second+" ";
				}
			}
			polygonStr = polygonStr.trim();
		}

		SpatialMatch match = new PolygonSpatialMatch(polygonStr, getSpatialOperation());

		if (log.isDebugEnabled())
			log.debug("Trying SpatialMatch: " + match.toString());
		return match;
	}
}