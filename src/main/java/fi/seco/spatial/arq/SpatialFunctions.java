	package fi.seco.spatial.arq;

import org.apache.jena.sparql.pfunction.PropertyFunction;
import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;

public class SpatialFunctions {
	
	static {
		init();
	}
	
    public static void init() {
		PropertyFunctionRegistry.get().put("http://www.seco.tkk.fi/spatial#withinPolygon", new PropertyFunctionFactory() {
            @Override
            public PropertyFunction create(String uri) {
                return new IsWithinPolygonPF();
            }
        });		
    }
}